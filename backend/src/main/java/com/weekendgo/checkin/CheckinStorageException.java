package com.weekendgo.checkin;

public class CheckinStorageException extends RuntimeException {

    public CheckinStorageException(String message) {
        super(message);
    }

    public CheckinStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
