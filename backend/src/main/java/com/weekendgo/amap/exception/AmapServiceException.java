package com.weekendgo.amap.exception;

public class AmapServiceException extends RuntimeException {

    public AmapServiceException(String message) {
        super(message);
    }

    public AmapServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
