package com.ejemploAPI.config.exceptions;

public class InvalidJsonFormatException extends RuntimeException {

    public InvalidJsonFormatException(String message) {
        super(message);
    }

    public InvalidJsonFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
