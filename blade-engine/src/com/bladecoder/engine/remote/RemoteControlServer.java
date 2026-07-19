/*******************************************************************************
 * Copyright 2026 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Small localhost-only HTTP server used to control a running game from another
 * process. All world access happens on the LibGDX render thread.
 */
public class RemoteControlServer {
    public static final int DEFAULT_PORT = 8080;

    private static final int MAX_BODY_LENGTH = 64 * 1024;
    private static final long STATE_TIMEOUT_MS = 2000;

    private final UI ui;
    private final World world;
    private final ArrayDeque<RemoteCommand> commandQueue = new ArrayDeque<RemoteCommand>();

    private volatile boolean running;
    private volatile int configuredPort = DEFAULT_PORT;
    private volatile String lastError;
    private ServerSocket serverSocket;

    public RemoteControlServer(UI ui, World world) {
        this.ui = ui;
        this.world = world;
    }

    public synchronized boolean start(int port) {
        if (port < 1 || port > 65535) {
            lastError = "Invalid HTTP control port: " + port;
            EngineLogger.error(lastError);
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
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException closeError) {
                    EngineLogger.error("Error closing HTTP remote control socket", closeError);
                }
            }
            lastError = "Unable to start HTTP remote control on port " + port + ": " + e.getMessage();
            EngineLogger.error(lastError, e);
            return false;
        }
    }

    public synchronized void stop() {
        running = false;

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                EngineLogger.error("Error closing HTTP remote control server", e);
            }
            serverSocket = null;
        }

        synchronized (commandQueue) {
            commandQueue.clear();
        }
    }

    public void dispose() {
        stop();
    }

    public boolean isRunning() {
        return running;
    }

    public int getConfiguredPort() {
        return configuredPort;
    }

    public String getLastError() {
        return lastError;
    }

    /** Called once per frame by {@link com.bladecoder.engine.BladeEngine}. */
    public void update() {
        if (!running || world.isPaused() || world.inCutMode() || ui.getRecorder().isPlaying()
                || ui.getTesterBot().isEnabled()) {
            return;
        }

        RemoteCommand command;
        synchronized (commandQueue) {
            command = commandQueue.poll();
        }

        if (command != null)
            execute(command);
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
                        handle(socket);
                    }
                }, "blade-engine-http-control-request");
                requestThread.setDaemon(true);
                requestThread.start();
            } catch (IOException e) {
                if (running) {
                    lastError = "HTTP remote control accept failed: " + e.getMessage();
                    EngineLogger.error(lastError, e);
                }
            }
        }
    }

    private void handle(Socket socket) {
        try {
            HttpRequest request = readRequest(socket.getInputStream());
            HttpResponse response = route(request);
            writeResponse(socket.getOutputStream(), response);
        } catch (BadRequestException e) {
            writeQuietly(socket, new HttpResponse(400, errorJson(e.getMessage())));
        } catch (Exception e) {
            lastError = "HTTP remote control request failed: " + e.getMessage();
            EngineLogger.error(lastError, e);
            writeQuietly(socket, new HttpResponse(500, errorJson("Internal server error")));
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // The response has already been sent when this can happen.
            }
        }
    }

    private HttpResponse route(HttpRequest request) throws BadRequestException {
        if ("GET".equals(request.method) && "/health".equals(request.path))
            return new HttpResponse(200, toJson(healthState()));

        if ("GET".equals(request.method) && "/state".equals(request.path)) {
            String state = getStateJson();
            return state == null ? new HttpResponse(503, errorJson("Game thread is not available"))
                    : new HttpResponse(200, state);
        }

        if ("POST".equals(request.method) && "/command".equals(request.path)) {
            RemoteCommand command = parseCommand(request.body);
            synchronized (commandQueue) {
                commandQueue.add(command);
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("status", "queued");
            result.put("queueLength", getQueueLength());
            return new HttpResponse(202, toJson(result));
        }

        if (!"GET".equals(request.method) && !"POST".equals(request.method))
            return new HttpResponse(405, errorJson("Only GET and POST are supported"));

        return new HttpResponse(404, errorJson("Unknown endpoint"));
    }

    private RemoteCommand parseCommand(String body) throws BadRequestException {
        if (body == null || body.trim().isEmpty())
            throw new BadRequestException("A JSON command body is required");

        JsonValue value;
        try {
            value = new JsonReader().parse(body);
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON command");
        }

        if (value == null || !value.isObject())
            throw new BadRequestException("Command must be a JSON object");

        String type = requiredString(value, "type");
        if ("actorVerb".equals(type)) {
            return RemoteCommand.actorVerb(requiredString(value, "actorId"), requiredString(value, "verb"),
                    optionalString(value, "target"));
        } else if ("sceneVerb".equals(type)) {
            return RemoteCommand.sceneVerb(requiredString(value, "verb"));
        } else if ("dialogOption".equals(type)) {
            return RemoteCommand.dialogOption(requiredInt(value, "option"));
        } else if ("goto".equals(type)) {
            return RemoteCommand.gotoPosition(requiredFloat(value, "x"), requiredFloat(value, "y"));
        } else if ("saveGame".equals(type)) {
            return RemoteCommand.saveGame(requiredString(value, "target"));
        }

        throw new BadRequestException("Unknown command type: " + type);
    }

    private void execute(RemoteCommand command) {
        Scene scene = world.getCurrentScene();
        if (scene == null) {
            logCommandError("No current scene available for remote command");
            return;
        }

        switch (command.type) {
        case ACTOR_VERB:
            BaseActor baseActor = scene.getActor(command.actorId, true);
            if (baseActor instanceof InteractiveActor) {
                ((InteractiveActor) baseActor).runVerb(command.verb, command.target);
            } else {
                logCommandError("Remote command actor not found: " + command.actorId);
            }
            break;
        case SCENE_VERB:
            scene.runVerb(command.verb);
            break;
        case DIALOG_OPTION:
            if (!world.hasDialogOptions() || command.option < 0 || command.option >= world.getDialogOptions().size()) {
                logCommandError("Remote command dialog option is not available: " + command.option);
            } else {
                world.selectDialogOption(command.option);
            }
            break;
        case GOTO:
            if (scene.getPlayer() == null) {
                logCommandError("Remote goto command requires a scene player");
            } else {
                scene.getPlayer().goTo(new Vector2(command.x, command.y), null, false);
            }
            break;
        case SAVE_GAME:
            try {
                world.getSerializer().saveGameState(command.target, true);
            } catch (IOException e) {
                logCommandError("Remote save game failed: " + e.getMessage());
            }
            break;
        default:
            logCommandError("Unknown remote command");
            break;
        }
    }

    private String getStateJson() {
        if (Gdx.app == null)
            return null;

        final String[] result = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = toJson(gameState());
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            return latch.await(STATE_TIMEOUT_MS, TimeUnit.MILLISECONDS) ? result[0] : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private Map<String, Object> healthState() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("status", running ? "running" : "stopped");
        state.put("port", configuredPort);
        state.put("queueLength", getQueueLength());
        if (lastError != null)
            state.put("lastError", lastError);
        return state;
    }

    private Map<String, Object> gameState() {
        Map<String, Object> state = healthState();
        state.put("paused", world.isPaused());
        state.put("cutMode", world.inCutMode());
        state.put("assetState", world.getAssetState() == null ? null : world.getAssetState().name());
        state.put("recorderPlaying", ui.getRecorder().isPlaying());
        state.put("testerBotEnabled", ui.getTesterBot().isEnabled());

        Scene scene = world.getCurrentScene();
        if (scene == null)
            return state;

        Map<String, Object> sceneState = new LinkedHashMap<String, Object>();
        sceneState.put("id", scene.getId());
        sceneState.put("state", scene.getState());
        state.put("scene", sceneState);

        if (scene.getPlayer() != null) {
            Map<String, Object> player = new LinkedHashMap<String, Object>();
            player.put("id", scene.getPlayer().getId());
            player.put("x", scene.getPlayer().getX());
            player.put("y", scene.getPlayer().getY());
            state.put("player", player);
        }

        List<Map<String, Object>> actors = new ArrayList<Map<String, Object>>();
        for (BaseActor actor : scene.getActors().values()) {
            if (actor instanceof InteractiveActor) {
                InteractiveActor interactiveActor = (InteractiveActor) actor;
                Map<String, Object> actorState = new LinkedHashMap<String, Object>();
                actorState.put("id", interactiveActor.getId());
                actorState.put("description", interactiveActor.getDesc());
                actorState.put("state", interactiveActor.getState());
                actorState.put("visible", interactiveActor.isVisible());
                actorState.put("canInteract", interactiveActor.canInteract());
                actorState.put("x", interactiveActor.getX());
                actorState.put("y", interactiveActor.getY());
                actorState.put("verbs", new ArrayList<String>(interactiveActor.getVerbManager().getVerbs().keySet()));
                actors.add(actorState);
            }
        }
        state.put("actors", actors);
        state.put("dialogOptions", world.hasDialogOptions() ? new ArrayList<String>(world.getDialogOptions())
                : new ArrayList<String>());
        return state;
    }

    private int getQueueLength() {
        synchronized (commandQueue) {
            return commandQueue.size();
        }
    }

    private void logCommandError(String message) {
        lastError = message;
        EngineLogger.error(message);
    }

    private static String requiredString(JsonValue value, String name) throws BadRequestException {
        String string = optionalString(value, name);
        if (string == null || string.trim().isEmpty())
            throw new BadRequestException("Missing or empty field: " + name);
        return string;
    }

    private static String optionalString(JsonValue value, String name) throws BadRequestException {
        JsonValue child = value.get(name);
        if (child == null || child.isNull())
            return null;
        if (!child.isString())
            throw new BadRequestException("Field must be a string: " + name);
        return child.asString();
    }

    private static int requiredInt(JsonValue value, String name) throws BadRequestException {
        JsonValue child = value.get(name);
        if (child == null || !child.isLong())
            throw new BadRequestException("Field must be an integer: " + name);
        return child.asInt();
    }

    private static float requiredFloat(JsonValue value, String name) throws BadRequestException {
        JsonValue child = value.get(name);
        if (child == null || (!child.isLong() && !child.isDouble()))
            throw new BadRequestException("Field must be a number: " + name);
        return child.asFloat();
    }

    private static HttpRequest readRequest(InputStream input) throws IOException, BadRequestException {
        String requestLine = readHttpLine(input);
        if (requestLine == null)
            throw new BadRequestException("Empty request");

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length != 3)
            throw new BadRequestException("Invalid HTTP request line");

        int contentLength = 0;
        String line;
        while ((line = readHttpLine(input)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon <= 0)
                throw new BadRequestException("Invalid HTTP header");
            String headerName = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String headerValue = line.substring(colon + 1).trim();
            if ("content-length".equals(headerName)) {
                try {
                    contentLength = Integer.parseInt(headerValue);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Invalid Content-Length");
                }
            } else if ("transfer-encoding".equals(headerName)) {
                throw new BadRequestException("Transfer-Encoding is not supported");
            }
        }

        if (contentLength < 0 || contentLength > MAX_BODY_LENGTH)
            throw new BadRequestException("Request body is too large");

        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int count = input.read(body, offset, contentLength - offset);
            if (count == -1)
                throw new BadRequestException("Unexpected end of request body");
            offset += count;
        }

        String path = requestParts[1];
        int queryStart = path.indexOf('?');
        if (queryStart >= 0)
            path = path.substring(0, queryStart);
        return new HttpRequest(requestParts[0], path, new String(body, StandardCharsets.UTF_8));
    }

    private static String readHttpLine(InputStream input) throws IOException, BadRequestException {
        StringBuilder line = new StringBuilder();
        int next;
        while ((next = input.read()) != -1) {
            if (next == '\n')
                return line.toString();
            if (next != '\r')
                line.append((char) next);
            if (line.length() > 8192)
                throw new BadRequestException("HTTP header is too large");
        }
        return line.length() == 0 ? null : line.toString();
    }

    private static void writeResponse(OutputStream output, HttpResponse response) throws IOException {
        byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + response.status + " " + reasonPhrase(response.status) + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n" + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(body);
        output.flush();
    }

    private static void writeQuietly(Socket socket, HttpResponse response) {
        try {
            writeResponse(socket.getOutputStream(), response);
        } catch (IOException e) {
            // Nothing can be reported to a disconnected HTTP client.
        }
    }

    private static String reasonPhrase(int status) {
        switch (status) {
        case 200:
            return "OK";
        case 202:
            return "Accepted";
        case 400:
            return "Bad Request";
        case 404:
            return "Not Found";
        case 405:
            return "Method Not Allowed";
        case 503:
            return "Service Unavailable";
        default:
            return "Internal Server Error";
        }
    }

    private static String errorJson(String message) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("error", message);
        return toJson(error);
    }

    private static String toJson(Object object) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, object);
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String) {
            appendJsonString(builder, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first)
                    builder.append(',');
                appendJsonString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                appendJson(builder, entry.getValue());
                first = false;
            }
            builder.append('}');
        } else if (value instanceof Iterable) {
            builder.append('[');
            boolean first = true;
            for (Object element : (Iterable<?>) value) {
                if (!first)
                    builder.append(',');
                appendJson(builder, element);
                first = false;
            }
            builder.append(']');
        } else {
            appendJsonString(builder, String.valueOf(value));
        }
    }

    private static void appendJsonString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '"':
                builder.append("\\\"");
                break;
            case '\\':
                builder.append("\\\\");
                break;
            case '\b':
                builder.append("\\b");
                break;
            case '\f':
                builder.append("\\f");
                break;
            case '\n':
                builder.append("\\n");
                break;
            case '\r':
                builder.append("\\r");
                break;
            case '\t':
                builder.append("\\t");
                break;
            default:
                if (c < 0x20) {
                    String hex = Integer.toHexString(c);
                    builder.append("\\u");
                    for (int j = hex.length(); j < 4; j++)
                        builder.append('0');
                    builder.append(hex);
                } else {
                    builder.append(c);
                }
                break;
            }
        }
        builder.append('"');
    }

    private static class HttpRequest {
        final String method;
        final String path;
        final String body;

        HttpRequest(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body;
        }
    }

    private static class HttpResponse {
        final int status;
        final String body;

        HttpResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static class BadRequestException extends Exception {
        private static final long serialVersionUID = 1L;

        BadRequestException(String message) {
            super(message);
        }
    }

    private enum CommandType {
        ACTOR_VERB, SCENE_VERB, DIALOG_OPTION, GOTO, SAVE_GAME
    }

    private static class RemoteCommand {
        final CommandType type;
        String actorId;
        String verb;
        String target;
        int option;
        float x;
        float y;

        private RemoteCommand(CommandType type) {
            this.type = type;
        }

        static RemoteCommand actorVerb(String actorId, String verb, String target) {
            RemoteCommand command = new RemoteCommand(CommandType.ACTOR_VERB);
            command.actorId = actorId;
            command.verb = verb;
            command.target = target;
            return command;
        }

        static RemoteCommand sceneVerb(String verb) {
            RemoteCommand command = new RemoteCommand(CommandType.SCENE_VERB);
            command.verb = verb;
            return command;
        }

        static RemoteCommand dialogOption(int option) {
            RemoteCommand command = new RemoteCommand(CommandType.DIALOG_OPTION);
            command.option = option;
            return command;
        }

        static RemoteCommand gotoPosition(float x, float y) {
            RemoteCommand command = new RemoteCommand(CommandType.GOTO);
            command.x = x;
            command.y = y;
            return command;
        }

        static RemoteCommand saveGame(String target) {
            RemoteCommand command = new RemoteCommand(CommandType.SAVE_GAME);
            command.target = target;
            return command;
        }
    }
}
