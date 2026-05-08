package com.weekendgo.place;

public class PlaceStorageException extends RuntimeException {

    public PlaceStorageException(String message) {
        super(message);
    }

    public PlaceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
