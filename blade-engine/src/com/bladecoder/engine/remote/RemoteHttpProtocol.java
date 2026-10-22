package com.bladecoder.engine.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Bounded HTTP parser shared by the legacy player API and debug API. */
final class RemoteHttpProtocol {
    private static final int MAX_BODY_LENGTH = 64 * 1024;
    private RemoteHttpProtocol() { }

    static RemoteHttpRequest readRequest(InputStream input) throws IOException, RemoteBadRequestException {
        String first = readLine(input);
        if (first == null) throw new RemoteBadRequestException("Empty request");
        String[] parts = first.split(" ");
        if (parts.length != 3 || !parts[1].startsWith("/")) throw new RemoteBadRequestException("Invalid HTTP request line");
        Map<String,String> headers = new LinkedHashMap<String,String>();
        String line;
        int count = 0;
        while ((line = readLine(input)) != null && !line.isEmpty()) {
            if (++count > 64) throw new RemoteBadRequestException("Too many headers");
            int colon = line.indexOf(':');
            if (colon <= 0) throw new RemoteBadRequestException("Invalid HTTP header");
            String key = line.substring(0,colon).trim().toLowerCase(Locale.ROOT);
            if (headers.containsKey(key)) throw new RemoteBadRequestException("Duplicate HTTP header");
            headers.put(key,line.substring(colon+1).trim());
        }
        int length = 0;
        try { if (headers.containsKey("content-length")) length = Integer.parseInt(headers.get("content-length")); }
        catch (NumberFormatException e) { throw new RemoteBadRequestException("Invalid Content-Length"); }
        if (headers.containsKey("transfer-encoding")) throw new RemoteBadRequestException("Transfer-Encoding is not supported");
        if (length < 0 || length > MAX_BODY_LENGTH) throw new RemoteBadRequestException("Request body is too large");
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int n = input.read(body,offset,length-offset);
            if (n < 0) throw new RemoteBadRequestException("Unexpected end of body");
            offset += n;
        }
        String path = parts[1];
        Map<String,String> query = new LinkedHashMap<String,String>();
        int q = path.indexOf('?');
        if (q >= 0) {
            try {
                for (String field : path.substring(q+1).split("&")) {
                    if (field.isEmpty()) continue;
                    String[] pair = field.split("=",2);
                    String key = URLDecoder.decode(pair[0],"UTF-8");
                    if (query.containsKey(key)) throw new RemoteBadRequestException("Duplicate query parameter");
                    query.put(key,URLDecoder.decode(pair.length == 2 ? pair[1] : "","UTF-8"));
                }
            } catch (IllegalArgumentException e) { throw new RemoteBadRequestException("Invalid query encoding"); }
            path = path.substring(0,q);
        }
        return new RemoteHttpRequest(parts[0],path,new String(body,StandardCharsets.UTF_8),query,headers);
    }

    static void writeResponse(OutputStream output, RemoteHttpResponse response) throws IOException {
        byte[] body = response.bytes != null ? response.bytes : response.body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + response.status + " " + reasonPhrase(response.status) + "\r\n"
                + "Content-Type: " + response.contentType + "\r\nContent-Length: " + body.length
                + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));output.write(body);output.flush();
    }

    static String errorJson(String message) {
        Map<String,Object> data = new LinkedHashMap<String,Object>();data.put("error",message);return RemoteJson.toJson(data);
    }

    private static String readLine(InputStream input) throws IOException, RemoteBadRequestException {
        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = input.read()) != -1) {
            if (ch == '\n') return result.toString();
            if (ch != '\r') result.append((char)ch);
            if (result.length() > 8192) throw new RemoteBadRequestException("HTTP line too long");
        }
        return result.length() == 0 ? null : result.toString();
    }

    private static String reasonPhrase(int status) {
        switch(status) {
        case 200:return "OK";case 202:return "Accepted";case 400:return "Bad Request";
        case 401:return "Unauthorized";case 404:return "Not Found";case 405:return "Method Not Allowed";
        case 409:return "Conflict";case 429:return "Too Many Requests";
        case 503:return "Service Unavailable";case 504:return "Gateway Timeout";default:return "Internal Server Error";
        }
    }
}

final class RemoteHttpRequest {
    final String method,path,body;
    final Map<String,String> query,headers;
    RemoteHttpRequest(String method,String path,String body) {
        this(method,path,body,new LinkedHashMap<String,String>(),new LinkedHashMap<String,String>());
    }
    RemoteHttpRequest(String method,String path,String body,Map<String,String> query,Map<String,String> headers) {
        this.method=method;this.path=path;this.body=body;this.query=query;this.headers=headers;
    }
}

final class RemoteHttpResponse {
    final int status;
    final String body,contentType;
    final byte[] bytes;
    RemoteHttpResponse(int status,String body) {
        this.status=status;this.body=body;this.contentType="application/json; charset=utf-8";this.bytes=null;
    }
    RemoteHttpResponse(byte[] bytes,String contentType) {
        this.status=200;this.body=null;this.bytes=bytes;this.contentType=contentType;
    }
}
