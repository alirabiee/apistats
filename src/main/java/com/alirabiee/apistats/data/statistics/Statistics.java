package com.alirabiee.apistats.data.statistics;

import com.alirabiee.apistats.data.Transaction;

/**
 * Get statistics on transactions within the configured time-range
 */
public interface Statistics {
    /**
     * Clear previous statistics data, O(1)
     */
    void clear();

    /**
     * Get latest available transaction report, O(1)
     * @return The report data
     */
    StatisticsReport getReport();

    /**
     * Add a transaction event to the history, O(lg 1)
     * @param tx The transaction to be added
     * @throws ExpiredTransactionException if the timestamp is outside of the range
     * @throws FutureStampedTransactionException if the timestamp is in the future
     */
    void add(Transaction tx) throws ExpiredTransactionException, FutureStampedTransactionException;
}
