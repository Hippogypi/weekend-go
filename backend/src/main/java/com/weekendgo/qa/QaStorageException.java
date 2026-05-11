package com.weekendgo.qa;

public class QaStorageException extends RuntimeException {

    public QaStorageException(String message) {
        super(message);
    }

    public QaStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
