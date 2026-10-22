package com.bladecoder.engine.remote;

import java.util.ArrayDeque;

/** Thread-safe FIFO queue between HTTP request threads and the render thread. */
final class RemoteCommandQueue {
    private final ArrayDeque<RemoteCommandRequest> commands = new ArrayDeque<RemoteCommandRequest>();

    synchronized boolean add(RemoteCommandRequest command) {
        commands.removeIf(request -> request.isCancelled());
        if(commands.size() >= 128)return false;
        commands.add(command);
        return true;
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
