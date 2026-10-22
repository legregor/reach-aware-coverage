package com.github.legregor.microservice.coverage.analysis.model;

/**
 * Kind of request body a call sends.
 */
public enum BodyKind {
    /** No request body is sent. */
    NONE,
    /** An object of a class from the service's own source code (a DTO with properties). */
    OBJECT,
    /** Any other value (List, String, number, Map, library type): one request element. */
    VALUE
}
