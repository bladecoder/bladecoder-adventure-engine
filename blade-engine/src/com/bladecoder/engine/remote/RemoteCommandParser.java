package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Parses and validates the JSON body accepted by {@code POST /command}. */
final class RemoteCommandParser {
    RemoteCommand parse(String body) throws RemoteBadRequestException {
        if (body == null || body.trim().isEmpty())
            throw new RemoteBadRequestException("A JSON command body is required");

        JsonValue value;
        try {
            value = new JsonReader().parse(body);
        } catch (Exception e) {
            throw new RemoteBadRequestException("Invalid JSON command");
        }

        if (value == null || !value.isObject())
            throw new RemoteBadRequestException("Command must be a JSON object");

        String type = requiredString(value, "type");
        if ("actorVerb".equals(type))
            return RemoteCommand.actorVerb(requiredString(value, "actorId"), requiredString(value, "verb"),
                    optionalString(value, "target"));
        if ("sceneVerb".equals(type))
            return RemoteCommand.sceneVerb(requiredString(value, "verb"));
        if ("dialogOption".equals(type))
            return RemoteCommand.dialogOption(requiredInt(value, "option"));
        if ("goto".equals(type))
            return RemoteCommand.gotoPosition(requiredFloat(value, "x"), requiredFloat(value, "y"));
        if ("saveGame".equals(type))
            return RemoteCommand.saveGame(requiredString(value, "target"));
        if ("screenshot".equals(type))
            return RemoteCommand.screenshot(requiredString(value, "target"));

        throw new RemoteBadRequestException("Unknown command type: " + type);
    }

    private String requiredString(JsonValue value, String name) throws RemoteBadRequestException {
        String string = optionalString(value, name);
        if (string == null || string.trim().isEmpty())
            throw new RemoteBadRequestException("Missing or empty field: " + name);
        return string;
    }

    private String optionalString(JsonValue value, String name) throws RemoteBadRequestException {
        JsonValue child = value.get(name);
        if (child == null || child.isNull())
            return null;
        if (!child.isString())
            throw new RemoteBadRequestException("Field must be a string: " + name);
        return child.asString();
    }

    private int requiredInt(JsonValue value, String name) throws RemoteBadRequestException {
        JsonValue child = value.get(name);
        if (child == null || !child.isLong())
            throw new RemoteBadRequestException("Field must be an integer: " + name);
        return child.asInt();
    }

    private float requiredFloat(JsonValue value, String name) throws RemoteBadRequestException {
        JsonValue child = value.get(name);
        if (child == null || (!child.isLong() && !child.isDouble()))
            throw new RemoteBadRequestException("Field must be a number: " + name);
        return child.asFloat();
    }
}
