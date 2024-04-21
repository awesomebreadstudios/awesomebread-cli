package com.awesomebread.cli.exceptions;

public class InvalidOptionsException extends RuntimeException {
    public InvalidOptionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOptionsException(String message) {
        super(message);
    }

    public InvalidOptionsException(Throwable cause) {
        super(cause);
    }
}
