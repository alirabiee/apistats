package com.alirabiee.apistats.data.statistics;

import com.alirabiee.apistats.data.Transaction;
import com.google.common.collect.MinMaxPriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class StatisticsImpl implements Statistics {

    private static final Logger log = LoggerFactory.getLogger(StatisticsImpl.class);

    private final StatElement[] elements; // holds transactions in each time-point
    private volatile int oldestTimeIndex; // transactions older than this will be discarded
    private volatile int newestTimeIndex;
    private volatile double txSum; // total sum of transactions over the configured time-range
    private volatile double txMax; // max transaction amount in total over the configured time-range
    private volatile double txMin; // min transaction amount in total over the configured time-range
    private volatile int txCount; // total number of transactions over the configured time-range

    private final ReadWriteLock dsLock = new ReentrantReadWriteLock(); // to synchronise access to the underlying datastructure
    private final ReadWriteLock reportLock = new ReentrantReadWriteLock(); // to synchronise access to the report data, tx*

    public StatisticsImpl(@Value("${history.max-seconds}") Integer maxSeconds) {
        this.elements = new StatElement[maxSeconds];
        this.oldestTimeIndex = 0;
        this.newestTimeIndex = this.elements.length - 1;

        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = new StatElement();
        }

        clear();
    }

    /**
     * Clear previous statistics data
     */
    @Override
    public void clear() {
        for (StatElement element : elements) {
            element.clear();
        }

        updateStats();
    }

    /**
     * Get latest available transaction report, O(1)
     * @return The report data
     */
    @Override
    public StatisticsReport getReport() {
        reportLock.readLock().lock();
        final StatisticsReport report = new StatisticsReport(txSum, txSum / Math.max(1, txCount), txMax, txMin, txCount);
        reportLock.readLock().unlock();

        return report;
    }

    /**
     * Add a transaction event to the history, O(lg n)
     * @param tx The transaction to be added
     * @throws ExpiredTransactionException if the timestamp is outside of the range
     * @throws FutureStampedTransactionException if the timestamp is in the future
     */
    @Override
    public void add(Transaction tx) throws ExpiredTransactionException, FutureStampedTransactionException {
        final int second = (int) ((System.currentTimeMillis() - tx.getTimestamp()) / 1000);

        if (second >= 60) {
            throw new ExpiredTransactionException();
        }

        if (second < 0) {
            log.error("A future-stamped transaction was received. Check your system clock.");
            throw new FutureStampedTransactionException();
        }

        dsLock.readLock().lock();
        int index = newestTimeIndex - second;
        dsLock.readLock().unlock();

        if (index < 0) {
            index += elements.length;
        }

        elements[index].add(tx);

        updateStats();
    }

    @Scheduled(fixedRate = 1000)
    public void moveTimeline() {
        dsLock.writeLock().lock();
        elements[oldestTimeIndex].clear();
        newestTimeIndex = (newestTimeIndex + 1) % elements.length;
        oldestTimeIndex = (oldestTimeIndex + 1) % elements.length;
        dsLock.writeLock().unlock();

        updateStats();
    }

    /**
     * This method updates current stats with O(1) with the constant coefficient configured by history.max-seconds
     */
    private void updateStats() {
        reportLock.writeLock().lock();
        txCount = Arrays.stream(elements).map(StatElement::count).reduce(Integer::sum).orElse(0);
        txSum = Arrays.stream(elements).map(StatElement::sum).reduce(Double::sum).orElse(0D);
        txMin = Arrays.stream(elements).map(StatElement::min).min(Double::compareTo).orElse(0D);
        txMax = Arrays.stream(elements).map(StatElement::max).max(Double::compareTo).orElse(0D);
        reportLock.writeLock().unlock();
    }

    /**
     * Holds the state of transactions for each time-point
     */
    class StatElement {
        private MinMaxPriorityQueue<Transaction> transactions =
                MinMaxPriorityQueue.create();
        private volatile double txSum = 0;

        synchronized void clear() {
            transactions.clear();
            txSum = 0;
        }

        synchronized void add(Transaction tx) {
            txSum += tx.getAmount();
            transactions.add(tx);
        }

        synchronized Double sum() {
            return txSum;
        }

        synchronized Double min() {
            return transactions.isEmpty() ? Double.MAX_VALUE : transactions.peekFirst().getAmount();
        }

        synchronized Double max() {
            return transactions.isEmpty() ? Double.MIN_VALUE : transactions.peekLast().getAmount();
        }

        synchronized Integer count() {
            return transactions.size();
        }
    }
}
