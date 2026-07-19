package com.bladecoder.engine.remote;

import java.util.ArrayDeque;

/** Thread-safe FIFO queue between HTTP request threads and the render thread. */
final class RemoteCommandQueue {
    private final ArrayDeque<RemoteCommand> commands = new ArrayDeque<RemoteCommand>();

    synchronized void add(RemoteCommand command) {
        commands.add(command);
    }

    synchronized RemoteCommand poll() {
        return commands.poll();
    }

    synchronized void clear() {
        commands.clear();
    }

    synchronized int size() {
        return commands.size();
    }
}
