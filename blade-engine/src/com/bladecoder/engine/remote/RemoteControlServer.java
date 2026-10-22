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
    private final RemoteEventLog eventLog;
    private RemoteControlMode mode = RemoteControlMode.PLAYER;
    private volatile RemoteDebugSession debugSession;
    private java.util.concurrent.ExecutorService workers;
    private String sessionId;

    private final RemoteCommandExecutor commandExecutor;
    private final RemoteHttpHandler requestHandler;

    private volatile boolean running;
    private volatile int configuredPort = DEFAULT_PORT;
    private volatile String lastError;
    private ServerSocket serverSocket;

    public RemoteControlServer(UI ui, World world) {
        this.ui = ui;
        this.world = world;
        eventLog = new RemoteEventLog(world);
        world.getEvents().addListener(eventLog);
        commandExecutor = new RemoteCommandExecutor(ui, world, this, eventLog);
        requestHandler = new RemoteHttpHandler(commandQueue, new RemoteGameStateProvider(ui, world, this), eventLog, this, this);
    }

    public synchronized boolean start(int port) { return start(port, RemoteControlMode.PLAYER); }

    public synchronized boolean start(int port, RemoteControlMode selectedMode) {
        if (running) return mode == selectedMode;
        mode = selectedMode;
        sessionId = java.util.UUID.randomUUID().toString();

        if (port < (mode == RemoteControlMode.DEBUG ? 0 : 1) || port > 65535) {
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
            configuredPort = socket.getLocalPort();
            workers = new java.util.concurrent.ThreadPoolExecutor(8, 8, 30, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<Runnable>(32), new java.util.concurrent.ThreadFactory() {
                public Thread newThread(Runnable task) { Thread thread = new Thread(task, "blade-http-request");thread.setDaemon(true);return thread; }
            });
            if (mode == RemoteControlMode.DEBUG) debugSession = new RemoteDebugSession(ui, world, commandExecutor);
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
        if (debugSession != null) { debugSession.close();debugSession = null; }
        if (workers != null) workers.shutdownNow();
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

    /** Restore debug polling/routing before scene input is processed. */
    public void prepareInput(){if(debugSession!=null)debugSession.prepareInput();}

    /** Called once per frame by {@link com.bladecoder.engine.BladeEngine}. */
    public void update() {
        if (debugSession != null) debugSession.update();
        if (!running || world.inCutMode() || ui.getRecorder().isPlaying() || ui.getTesterBot().isEnabled()) {
            return;
        }

        RemoteCommandRequest request = commandQueue.poll();
        if (request != null && !request.isCancelled())
            request.complete(commandExecutor.execute(request.getCommand()));
    }

    @Override public String getMode() { return mode.name().toLowerCase(java.util.Locale.ROOT); }
    @Override public String getSessionId() { return debugSession == null ? sessionId : debugSession.id; }
    public float adjustDelta(float normal) { return debugSession == null ? normal : debugSession.delta(normal); }
    RemoteHttpResponse debugRoute(RemoteHttpRequest request) throws Exception {
        RemoteDebugSession session = debugSession;
        if (session == null) return new RemoteHttpResponse(404, RemoteHttpProtocol.errorJson("Endpoint unavailable"));
        return session.route(request);
    }
    /** Game-thread inspector facade. Shares the HTTP session/backend without loopback requests. */
    public java.util.Map<String,Object> inspectDebug() throws Exception {return requireDebug().snapshot();}
    public java.util.Map<String,Object> inspectEvents() throws Exception {return requireDebug().events.read(0,0);}
    public String debugJson(Object value){return RemoteJson.toJson(value);}
    private RemoteDebugSession requireDebug() {
        if(debugSession==null || !running)throw new IllegalStateException("An explicit debug session is required");return debugSession;
    }
    public java.util.Map<String,Object> submitInspectorCommand(String operation,String arguments) throws Exception {
        RemoteDebugSession session=requireDebug();
        com.badlogic.gdx.utils.JsonValue parsedJson=new com.badlogic.gdx.utils.JsonReader().parse(arguments);
        if("scenario.run".equals(operation))RemoteScenario.validate(parsedJson.require("scenario"));
        Object parsed=RemoteDebugState.value(parsedJson);
        if(!(parsed instanceof java.util.Map))throw new IllegalArgumentException("Command arguments must be an object");
        String body=RemoteJson.toJson(RemoteDebugState.map("request_id",java.util.UUID.randomUUID().toString(),"operation",operation,"arguments",parsed,"timeout_ms",30000));
        if(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("Command body exceeds the shared protocol limit");
        return inspectorRequest(session,new RemoteHttpRequest("POST","/debug/v1/commands",body));
    }
    public java.util.Map<String,Object> inspectCommand(String id) throws Exception {
        if(id==null || !id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Invalid command id");
        return inspectorRequest(requireDebug(),new RemoteHttpRequest("GET","/debug/v1/commands/"+id,""));
    }
    @SuppressWarnings("unchecked")
    private java.util.Map<String,Object> inspectorRequest(RemoteDebugSession session,RemoteHttpRequest request) throws Exception {
        request.headers.put("authorization","Bearer "+session.token);
        RemoteHttpResponse response=session.route(request);
        java.util.Map<String,Object> result=(java.util.Map<String,Object>)RemoteDebugState.value(new com.badlogic.gdx.utils.JsonReader().parse(response.body));
        if(response.status>=400)throw new IllegalArgumentException(RemoteJson.toJson(result));return result;
    }
    public java.util.Map<String,Object> exportInspectorEvidence() throws Exception {return requireDebug().exportInspectorEvidence();}
    public void writeDiscovery(String filename) {
        if (!running) return;
        java.util.Map<String,Object> info = RemoteDebugState.map("protocol",1,"mode",getMode(),"session_id",getSessionId(),
                "url","http://127.0.0.1:"+configuredPort,"runtime",System.getProperty("java.version"));
        if (debugSession != null) info.put("token",debugSession.token);
        java.io.File file = new java.io.File(filename);
        try {
            java.nio.file.Path path = file.toPath();
            try { java.nio.file.Files.createFile(path, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))); }
            catch (UnsupportedOperationException unsupported) { java.nio.file.Files.createFile(path); }
            java.nio.file.Files.write(path, RemoteJson.toJson(info).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException failure) { reportError("Cannot write HTTP discovery file", failure); }
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
                try { workers.execute(new Runnable() {
                    @Override
                    public void run() {
                        requestHandler.handle(socket);
                    }
                }); } catch (java.util.concurrent.RejectedExecutionException busy) { socket.close(); }
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
