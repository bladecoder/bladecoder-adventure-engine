package com.bladecoder.engine.remote;

import java.util.Map;

/** Small JSON writer for the primitive maps and lists exposed by the API. */
final class RemoteJson {
    private RemoteJson() {
    }

    static String toJson(Object object) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, object);
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String) {
            appendString(builder, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first)
                    builder.append(',');
                appendString(builder, String.valueOf(entry.getKey()));
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
            appendString(builder, String.valueOf(value));
        }
    }

    private static void appendString(StringBuilder builder, String value) {
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
}
