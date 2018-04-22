package com.alirabiee.apistats.data.statistics;

/**
 * This exception denotes that the pertinent transaction has a timestamp in the future
 */
public class FutureStampedTransactionException extends Exception {
    public FutureStampedTransactionException() {
        super();
    }

    public FutureStampedTransactionException(String message) {
        super(message);
    }
}
