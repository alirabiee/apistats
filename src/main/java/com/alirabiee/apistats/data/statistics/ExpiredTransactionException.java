package com.alirabiee.apistats.data.statistics;

/**
 * This exception denotes that the pertinent transaction has a timestamp outside the permitted time range
 */
public class ExpiredTransactionException extends Exception {
    public ExpiredTransactionException() {
        super();
    }

    public ExpiredTransactionException(String message) {
        super(message);
    }
}
