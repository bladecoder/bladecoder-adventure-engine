package com.bladecoder.engine.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Minimal HTTP/1.1 parser and JSON response writer for the local server. */
final class RemoteHttpProtocol {
    private static final int MAX_BODY_LENGTH = 64 * 1024;

    private RemoteHttpProtocol() {
    }

    static RemoteHttpRequest readRequest(InputStream input) throws IOException, RemoteBadRequestException {
        String requestLine = readLine(input);
        if (requestLine == null)
            throw new RemoteBadRequestException("Empty request");

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length != 3)
            throw new RemoteBadRequestException("Invalid HTTP request line");

        int contentLength = 0;
        String line;
        while ((line = readLine(input)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon <= 0)
                throw new RemoteBadRequestException("Invalid HTTP header");
            String headerName = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String headerValue = line.substring(colon + 1).trim();
            if ("content-length".equals(headerName)) {
                try {
                    contentLength = Integer.parseInt(headerValue);
                } catch (NumberFormatException e) {
                    throw new RemoteBadRequestException("Invalid Content-Length");
                }
            } else if ("transfer-encoding".equals(headerName)) {
                throw new RemoteBadRequestException("Transfer-Encoding is not supported");
            }
        }

        if (contentLength < 0 || contentLength > MAX_BODY_LENGTH)
            throw new RemoteBadRequestException("Request body is too large");

        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int count = input.read(body, offset, contentLength - offset);
            if (count == -1)
                throw new RemoteBadRequestException("Unexpected end of request body");
            offset += count;
        }

        String path = requestParts[1];
        int queryStart = path.indexOf('?');
        if (queryStart >= 0)
            path = path.substring(0, queryStart);
        return new RemoteHttpRequest(requestParts[0], path, new String(body, StandardCharsets.UTF_8));
    }

    static void writeResponse(OutputStream output, RemoteHttpResponse response) throws IOException {
        byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + response.status + " " + reasonPhrase(response.status) + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n" + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(body);
        output.flush();
    }

    static String errorJson(String message) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("error", message);
        return RemoteJson.toJson(error);
    }

    private static String readLine(InputStream input) throws IOException, RemoteBadRequestException {
        StringBuilder line = new StringBuilder();
        int next;
        while ((next = input.read()) != -1) {
            if (next == '\n')
                return line.toString();
            if (next != '\r')
                line.append((char) next);
            if (line.length() > 8192)
                throw new RemoteBadRequestException("HTTP header is too large");
        }
        return line.length() == 0 ? null : line.toString();
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
}

final class RemoteHttpRequest {
    final String method;
    final String path;
    final String body;

    RemoteHttpRequest(String method, String path, String body) {
        this.method = method;
        this.path = path;
        this.body = body;
    }
}

final class RemoteHttpResponse {
    final int status;
    final String body;

    RemoteHttpResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
}
