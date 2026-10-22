package com.bladecoder.engine.remote;

interface RemoteServerStatus {
    default String getMode() { return "player"; }
    default String getSessionId() { return null; }

    boolean isRunning();

    int getConfiguredPort();

    int getQueueLength();

    String getLastError();
}
