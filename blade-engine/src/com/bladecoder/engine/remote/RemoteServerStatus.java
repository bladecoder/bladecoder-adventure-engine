package com.bladecoder.engine.remote;

interface RemoteServerStatus {
    boolean isRunning();

    int getConfiguredPort();

    int getQueueLength();

    String getLastError();
}
