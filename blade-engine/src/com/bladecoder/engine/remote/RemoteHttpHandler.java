package com.bladecoder.engine.remote;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/** Routes HTTP requests without owning the server socket lifecycle. */
final class RemoteHttpHandler {
    private final RemoteCommandParser commandParser = new RemoteCommandParser();
    private final RemoteCommandQueue commandQueue;
    private final RemoteGameStateProvider stateProvider;
    private final RemoteErrorReporter errorReporter;

    RemoteHttpHandler(RemoteCommandQueue commandQueue, RemoteGameStateProvider stateProvider,
            RemoteErrorReporter errorReporter) {
        this.commandQueue = commandQueue;
        this.stateProvider = stateProvider;
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

        if ("POST".equals(request.method) && "/command".equals(request.path)) {
            commandQueue.add(commandParser.parse(request.body));
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("status", "queued");
            result.put("queueLength", commandQueue.size());
            return new RemoteHttpResponse(202, RemoteJson.toJson(result));
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
