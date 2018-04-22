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
    private int oldestTimeIndex; // transactions older than this will be discarded
    private int newestTimeIndex;
    private double txSum; // total sum of transactions over the configured time-range
    private double txMax; // max transaction amount in total over the configured time-range
    private double txMin; // min transaction amount in total over the configured time-range
    private int txCount; // total number of transactions over the configured time-range

    private final ReadWriteLock dsLock = new ReentrantReadWriteLock(); // to synchronise access to the underlying datastructure
    private final ReadWriteLock reportLock = new ReentrantReadWriteLock(); // to synchronise access to the report data, tx*

    public StatisticsImpl(@Value("${history.max-seconds}") Integer maxSeconds) {
        log.debug("maxSeconds = " + maxSeconds);
        this.elements = new StatElement[maxSeconds];
        this.oldestTimeIndex = 0;
        this.newestTimeIndex = this.elements.length - 1;
        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = new StatElement();
        }
        clear();
    }

    @Override
    public void clear() {
        for (StatElement element : elements) {
            element.clear();
        }

        updateStats();
    }

    @Override
    public StatisticsReport getReport() {
        reportLock.readLock().lock();
        final StatisticsReport report = new StatisticsReport(txSum, txSum / Math.max(1, txCount), txMax, txMin, txCount);
        reportLock.readLock().unlock();

        return report;
    }

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
        dsLock.readLock().lock();
        elements[oldestTimeIndex].clear();
        dsLock.readLock().unlock();

        dsLock.writeLock().lock();
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
        txCount = Arrays.stream(elements).map(StatElement::count).reduce(Integer::sum).get();
        txSum = Arrays.stream(elements)
                      .map(StatElement::sum)
                      .reduce(Double::sum)
                      .orElse(0D);
        txMin = Arrays.stream(elements).map(StatElement::min).min(Double::compareTo).get();
        txMax = Arrays.stream(elements).map(StatElement::max).max(Double::compareTo).get();
        reportLock.writeLock().unlock();
    }

    /**
     * Holds the state of transactions for each time-point
     */
    class StatElement {
        private MinMaxPriorityQueue<Transaction> transactions =
                MinMaxPriorityQueue.create();
        private double txSum = 0;

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