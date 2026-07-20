package com.bladecoder.engine.remote;

import java.util.ArrayDeque;

/** Thread-safe FIFO queue between HTTP request threads and the render thread. */
final class RemoteCommandQueue {
    private final ArrayDeque<RemoteCommandRequest> commands = new ArrayDeque<RemoteCommandRequest>();

    synchronized void add(RemoteCommandRequest command) {
        commands.add(command);
    }

    synchronized RemoteCommandRequest poll() {
        return commands.poll();
    }

    synchronized void clear() {
        commands.clear();
    }

    synchronized int size() {
        return commands.size();
    }
}
