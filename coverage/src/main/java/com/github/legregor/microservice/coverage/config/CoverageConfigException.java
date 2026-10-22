package com.github.legregor.microservice.coverage.config;

import java.io.IOException;

/** The coverage config file cannot be read or does not have the expected structure; the message says why. */
public class CoverageConfigException extends IOException {

    public CoverageConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
