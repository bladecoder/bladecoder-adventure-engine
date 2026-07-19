package com.bladecoder.engine.remote;

final class RemoteBadRequestException extends Exception {
    private static final long serialVersionUID = 1L;

    RemoteBadRequestException(String message) {
        super(message);
    }
}
