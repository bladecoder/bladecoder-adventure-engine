package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Structural MODEL checks using the same bundled contract as the authoring toolkit. */
final class RemoteEditSchema {
  private final JsonValue schema;

  RemoteEditSchema() {
    try (InputStream stream =
        RemoteEditSchema.class.getResourceAsStream("/edit/chapter.schema.json")) {
      if (stream == null) throw new IllegalStateException("Missing bundled MODEL contract");
      schema = new JsonReader().parse(stream);
    } catch (IOException failure) {
      throw new IllegalStateException(failure);
    }
  }

  void validate(JsonValue value, String definition) {
    check(value, schema.require("$defs").require(definition), definition);
  }

  private boolean matches(JsonValue value, JsonValue rule, String path) {
    try {
      check(value, rule, path);
      return true;
    } catch (IllegalArgumentException invalid) {
      return false;
    }
  }

  private boolean type(JsonValue value, String name) {
    switch (name) {
      case "object":
        return value.isObject();
      case "array":
        return value.isArray();
      case "string":
        return value.isString();
      case "number":
        return value.isNumber() && Double.isFinite(value.asDouble());
      case "integer":
        return value.isLong();
      case "boolean":
        return value.isBoolean();
      case "null":
        return value.isNull();
      default:
        throw new IllegalStateException("Unsupported schema type");
    }
  }

  private void check(JsonValue value, JsonValue rule, String path) {
    if (rule.has("$ref")) {
      String ref = rule.getString("$ref");
      check(value, schema.require("$defs").require(ref.substring("#/$defs/".length())), path);
      return;
    }
    if (rule.has("const")
        && !Objects.equals(
            rule.get("const").toJson(JsonWriter.OutputType.json),
            value.toJson(JsonWriter.OutputType.json)))
      throw new IllegalArgumentException(path + ": unexpected constant");
    JsonValue types = rule.get("type");
    if (types != null) {
      boolean valid = false;
      if (types.isArray()) {
        for (JsonValue item : types) valid |= type(value, item.asString());
      } else valid = type(value, types.asString());
      if (!valid) throw new IllegalArgumentException(path + ": invalid type");
    }
    for (String choice : new String[] {"oneOf", "anyOf"})
      if (rule.has(choice)) {
        int count = 0;
        for (JsonValue option : rule.get(choice)) if (matches(value, option, path)) count++;
        if (count == 0 || choice.equals("oneOf") && count != 1)
          throw new IllegalArgumentException(path + ": does not match a supported MODEL type");
      }
    if (rule.has("not") && matches(value, rule.get("not"), path))
      throw new IllegalArgumentException(path + ": disallowed value");
    if (rule.has("enum")) {
      boolean found = false;
      for (JsonValue option : rule.get("enum"))
        if (Objects.equals(
            option.toJson(JsonWriter.OutputType.json), value.toJson(JsonWriter.OutputType.json)))
          found = true;
      if (!found) throw new IllegalArgumentException(path + ": unsupported value");
    }
    if (value.isString()
        && rule.has("pattern")
        && !value.asString().matches(rule.getString("pattern")))
      throw new IllegalArgumentException(path + ": invalid identifier");
    if (value.isArray()) {
      if (value.size < rule.getInt("minItems", 0)
          || value.size > rule.getInt("maxItems", Integer.MAX_VALUE))
        throw new IllegalArgumentException(path + ": invalid array size");
      if (rule.has("items")) {
        int index = 0;
        for (JsonValue item : value) check(item, rule.get("items"), path + "/" + index++);
      }
    }
    if (value.isObject()) {
      if (rule.has("required"))
        for (JsonValue field : rule.get("required"))
          if (!value.has(field.asString()))
            throw new IllegalArgumentException(path + ": missing " + field.asString());
      JsonValue properties = rule.get("properties"), additional = rule.get("additionalProperties");
      Set<String> seen = new HashSet<>();
      for (JsonValue field : value) {
        if (!seen.add(field.name))
          throw new IllegalArgumentException(path + ": duplicate field " + field.name);
        JsonValue property = properties == null ? null : properties.get(field.name);
        if (property != null) check(field, property, path + "/" + field.name);
        else if (additional != null) {
          if (additional.isBoolean() && !additional.asBoolean())
            throw new IllegalArgumentException(path + ": unknown field " + field.name);
          if (additional.isObject()) check(field, additional, path + "/" + field.name);
        }
      }
    }
  }
}
