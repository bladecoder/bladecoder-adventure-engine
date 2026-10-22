package com.bladecoder.engine.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Standalone contract checks, no graphics, network ports or external test library. */
public class RemoteProtocolTest {
    static int checks;
    static void check(boolean condition,String label) {
        if(!condition)throw new AssertionError(label);checks++;
    }
    static RemoteHttpRequest parse(String request) throws Exception {
        return RemoteHttpProtocol.readRequest(new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8)));
    }
    static void rejects(String request) throws Exception {
        try {parse(request);throw new AssertionError("Malformed HTTP accepted");}
        catch(RemoteBadRequestException expected){checks++;}
    }
    public static void main(String[] args) throws Exception {
        check(RemoteControlMode.parse("player")==RemoteControlMode.PLAYER,"player mode");
        check(RemoteControlMode.parse("debug")==RemoteControlMode.DEBUG,"debug mode");
        try{RemoteControlMode.parse("other");throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        RemoteHttpRequest request=parse("GET /debug/v1/events?after=7&wait_ms=100 HTTP/1.1\r\nAuthorization: Bearer x\r\n\r\n");
        check(request.path.equals("/debug/v1/events"),"path without query");
        check(request.query.get("after").equals("7"),"cursor parsed");
        check(request.headers.get("authorization").equals("Bearer x"),"authorization parsed");
        rejects("POST /command HTTP/1.1\r\nContent-Length: -1\r\n\r\n");
        rejects("POST /command HTTP/1.1\r\nContent-Length: 999999\r\n\r\n");
        rejects("POST /command HTTP/1.1\r\nContent-Length: 1\r\nContent-Length: 1\r\n\r\nx");
        rejects("POST /command HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n");
        rejects("GET /events?after=1&after=2 HTTP/1.1\r\n\r\n");
        rejects("GET /events?after=%zz HTTP/1.1\r\n\r\n");
        rejects("POST /command HTTP/1.1\r\nContent-Length: 10\r\n\r\nx");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        RemoteHttpProtocol.writeResponse(bytes,new RemoteHttpResponse(new byte[]{0,1,2},"image/png"));
        String output=new String(bytes.toByteArray(),StandardCharsets.ISO_8859_1);
        check(output.contains("Content-Type: image/png") && output.contains("Content-Length: 3"),"binary artifact response");
        RemoteDebugEvents events=new RemoteDebugEvents();
        for(int i=0;i<2050;i++)events.add("test",i);
        Map<String,Object> all=events.read(0,0);
        check((Boolean)all.get("lost"),"event overflow explicit");
        check(((List<?>)all.get("events")).size()==2048,"bounded event buffer");
        check(((Number)all.get("next_cursor")).longValue()==2050,"monotonic cursor");
        Map<String,Object> tail=events.read(2049,0);
        check(!((Boolean)tail.get("lost")) && ((List<?>)tail.get("events")).size()==1,"incremental non-destructive read");
        check(events.read(2049,0).equals(tail),"second observer does not consume events");
        checks+=RemoteDebugInputTest.run();
        checks+=ContentOverridesTest.run();
        checks+=RemoteScenarioTest.run();
        checks+=RemoteSourceTraceTest.run();
        System.out.println("REMOTE_PROTOCOL_PASS checks="+checks);
    }
}
