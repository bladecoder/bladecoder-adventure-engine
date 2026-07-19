package com.bladecoder.engine.remote;

interface RemoteErrorReporter {
    void reportError(String message);

    void reportError(String message, Exception exception);
}
