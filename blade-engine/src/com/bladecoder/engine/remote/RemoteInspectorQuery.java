package com.bladecoder.engine.remote;

import java.util.List;
import java.util.Map;

/** Focused read-only inspection of an already selected diagnostic view. */
public final class RemoteInspectorQuery {
    private RemoteInspectorQuery() {}
    public static Object select(Object data, String pointer) {
        if (pointer == null || pointer.isEmpty()) return data;
        if (!pointer.startsWith("/") || pointer.replace("~0", "").replace("~1", "").contains("~"))
            throw new IllegalArgumentException("Use an RFC 6901 JSON pointer relative to the selected view");
        Object value = data;
        for (String encoded : pointer.substring(1).split("/", -1)) {
            String key = encoded.replace("~1", "/").replace("~0", "~");
            if (value instanceof Map && ((Map<?, ?>) value).containsKey(key)) {
                value = ((Map<?, ?>) value).get(key);
            } else if (value instanceof List && key.matches("0|[1-9][0-9]*")) {
                int index;
                try { index = Integer.parseInt(key); }
                catch (NumberFormatException error) { throw new IllegalArgumentException("Array index out of range: " + key); }
                List<?> list = (List<?>) value;
                if (index >= list.size()) throw new IllegalArgumentException("Array index out of range: " + key);
                value = list.get(index);
            } else throw new IllegalArgumentException("JSON pointer not found: " + pointer);
        }
        return value;
    }
}
