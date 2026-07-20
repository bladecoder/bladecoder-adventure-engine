package com.bladecoder.engine.remote;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/** Routes HTTP requests without owning the server socket lifecycle. */
final class RemoteHttpHandler {
    private static final long COMMAND_TIMEOUT_MS = 2000;
    private final RemoteCommandParser commandParser = new RemoteCommandParser();
    private final RemoteCommandQueue commandQueue;
    private final RemoteGameStateProvider stateProvider;
    private final RemoteEventLog eventLog;
    private final RemoteErrorReporter errorReporter;

    RemoteHttpHandler(RemoteCommandQueue commandQueue, RemoteGameStateProvider stateProvider, RemoteEventLog eventLog,
            RemoteErrorReporter errorReporter) {
        this.commandQueue = commandQueue;
        this.stateProvider = stateProvider;
        this.eventLog = eventLog;
        this.errorReporter = errorReporter;
    }

    void handle(Socket socket) {
        try {
            RemoteHttpRequest request = RemoteHttpProtocol.readRequest(socket.getInputStream());
            RemoteHttpProtocol.writeResponse(socket.getOutputStream(), route(request));
        } catch (RemoteBadRequestException e) {
            writeQuietly(socket, new RemoteHttpResponse(400, RemoteHttpProtocol.errorJson(e.getMessage())));
        } catch (Exception e) {
            errorReporter.reportError("HTTP remote control request failed: " + e.getMessage(), e);
            writeQuietly(socket, new RemoteHttpResponse(500, RemoteHttpProtocol.errorJson("Internal server error")));
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // The response has already been sent when this can happen.
            }
        }
    }

    private RemoteHttpResponse route(RemoteHttpRequest request) throws RemoteBadRequestException {
        if ("GET".equals(request.method) && "/health".equals(request.path))
            return new RemoteHttpResponse(200, RemoteJson.toJson(stateProvider.getHealthState()));

        if ("GET".equals(request.method) && "/state".equals(request.path)) {
            String state = stateProvider.getStateJson();
            return state == null ? new RemoteHttpResponse(503, RemoteHttpProtocol.errorJson("Game thread is not available"))
                    : new RemoteHttpResponse(200, state);
        }

        if ("GET".equals(request.method) && "/events".equals(request.path))
            return new RemoteHttpResponse(200, RemoteJson.toJson(eventLog.getEventsState()));

        if ("POST".equals(request.method) && "/command".equals(request.path)) {
            RemoteCommandRequest commandRequest = new RemoteCommandRequest(commandParser.parse(request.body));
            commandQueue.add(commandRequest);
            RemoteCommandResult commandResult;
            try {
                commandResult = commandRequest.await(COMMAND_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                commandRequest.cancel();
                return new RemoteHttpResponse(503, RemoteHttpProtocol.errorJson("Interrupted while waiting for game thread"));
            }
            if (commandResult == null) {
                commandRequest.cancel();
                return new RemoteHttpResponse(504, RemoteHttpProtocol.errorJson("Game thread did not process the command in time"));
            }
            if (!commandResult.successful)
                return new RemoteHttpResponse(commandResult.status, RemoteHttpProtocol.errorJson(commandResult.message));
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("status", "executed");
            result.put("queueLength", commandQueue.size());
            return new RemoteHttpResponse(200, RemoteJson.toJson(result));
        }

        if (!"GET".equals(request.method) && !"POST".equals(request.method))
            return new RemoteHttpResponse(405, RemoteHttpProtocol.errorJson("Only GET and POST are supported"));

        return new RemoteHttpResponse(404, RemoteHttpProtocol.errorJson("Unknown endpoint"));
    }

    private void writeQuietly(Socket socket, RemoteHttpResponse response) {
        try {
            RemoteHttpProtocol.writeResponse(socket.getOutputStream(), response);
        } catch (IOException e) {
            // Nothing can be reported to a disconnected HTTP client.
        }
    }
}
