package com.bladecoder.engine.remote;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** A queued command and the HTTP request thread waiting for its render-thread result. */
final class RemoteCommandRequest {
    private final RemoteCommand command;
    private final CountDownLatch completed = new CountDownLatch(1);
    private volatile RemoteCommandResult result;
    private volatile boolean cancelled;

    RemoteCommandRequest(RemoteCommand command) {
        this.command = command;
    }

    RemoteCommand getCommand() {
        return command;
    }

    void complete(RemoteCommandResult result) {
        if (cancelled)
            return;
        this.result = result;
        completed.countDown();
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }

    RemoteCommandResult await(long timeoutMs) throws InterruptedException {
        return completed.await(timeoutMs, TimeUnit.MILLISECONDS) ? result : null;
    }
}
