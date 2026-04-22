package com.edwards.gpucalc.config;

public final class ConfigParseException extends RuntimeException {

    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
