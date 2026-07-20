/*******************************************************************************
 * Copyright 2026 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 ******************************************************************************/
package com.bladecoder.engine.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Localhost-only HTTP server used to control a running game from another
 * process. World access is delegated to render-thread collaborators.
 */
public class RemoteControlServer implements RemoteServerStatus, RemoteErrorReporter {
    public static final int DEFAULT_PORT = 8080;

    private final UI ui;
    private final World world;
    private final RemoteCommandQueue commandQueue = new RemoteCommandQueue();
    private final RemoteEventLog eventLog = new RemoteEventLog();
    private final RemoteCommandExecutor commandExecutor;
    private final RemoteHttpHandler requestHandler;

    private volatile boolean running;
    private volatile int configuredPort = DEFAULT_PORT;
    private volatile String lastError;
    private ServerSocket serverSocket;

    public RemoteControlServer(UI ui, World world) {
        this.ui = ui;
        this.world = world;
        world.getEvents().addListener(eventLog);
        commandExecutor = new RemoteCommandExecutor(ui, world, this, eventLog);
        requestHandler = new RemoteHttpHandler(commandQueue, new RemoteGameStateProvider(ui, world, this), eventLog, this);
    }

    public synchronized boolean start(int port) {
        if (port < 1 || port > 65535) {
            reportError("Invalid HTTP control port: " + port);
            return false;
        }

        configuredPort = port;
        if (running)
            return true;

        ServerSocket socket = null;
        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
            serverSocket = socket;
            running = true;
            lastError = null;

            Thread acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    acceptLoop();
                }
            }, "blade-engine-http-control");
            acceptThread.setDaemon(true);
            acceptThread.start();

            EngineLogger.debug("HTTP remote control listening on 127.0.0.1:" + port);
            return true;
        } catch (Exception e) {
            closeSocket(socket);
            reportError("Unable to start HTTP remote control on port " + port + ": " + e.getMessage(), e);
            return false;
        }
    }

    public synchronized void stop() {
        running = false;
        closeSocket(serverSocket);
        serverSocket = null;
        commandQueue.clear();
    }

    public void dispose() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getConfiguredPort() {
        return configuredPort;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public int getQueueLength() {
        return commandQueue.size();
    }

    /** Called once per frame by {@link com.bladecoder.engine.BladeEngine}. */
    public void update() {
        if (!running || world.inCutMode() || ui.getRecorder().isPlaying() || ui.getTesterBot().isEnabled()) {
            return;
        }

        RemoteCommandRequest request = commandQueue.poll();
        if (request != null && !request.isCancelled())
            request.complete(commandExecutor.execute(request.getCommand()));
    }

    @Override
    public void reportError(String message) {
        lastError = message;
        EngineLogger.error(message);
    }

    @Override
    public void reportError(String message, Exception exception) {
        lastError = message;
        EngineLogger.error(message, exception);
    }

    private void acceptLoop() {
        while (running) {
            try {
                ServerSocket socketServer = serverSocket;
                if (socketServer == null)
                    return;

                final Socket socket = socketServer.accept();
                Thread requestThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        requestHandler.handle(socket);
                    }
                }, "blade-engine-http-control-request");
                requestThread.setDaemon(true);
                requestThread.start();
            } catch (IOException e) {
                if (running)
                    reportError("HTTP remote control accept failed: " + e.getMessage(), e);
            }
        }
    }

    private void closeSocket(ServerSocket socket) {
        if (socket == null)
            return;

        try {
            socket.close();
        } catch (IOException e) {
            reportError("Error closing HTTP remote control server", e);
        }
    }
}
