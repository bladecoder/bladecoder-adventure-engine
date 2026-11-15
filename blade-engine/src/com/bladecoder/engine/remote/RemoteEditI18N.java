package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.i18n.I18N;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Extracts MODEL literals with the same key layout used by the graphical editor. */
final class RemoteEditI18N {
  private RemoteEditI18N() {}

  static Map<String, byte[]> extract(
      Map<String, JsonValue> documents, Map<String, byte[]> currentProperties) {
    Map<String, PropertyFile> files = new LinkedHashMap<>();
    Map<String, byte[]> result = new LinkedHashMap<>();

    for (Map.Entry<String, byte[]> entry : currentProperties.entrySet())
      files.put(entry.getKey(), new PropertyFile(entry.getValue()));

    JsonValue world = documents.get(documents.containsKey("world") ? "world" : "world.json");
    PropertyFile worldProperties = file(files, "world.properties");
    extractVerbs(world.get("verbs"), null, null, worldProperties);

    for (Map.Entry<String, JsonValue> entry : documents.entrySet()) {
      if (!entry.getKey().endsWith(".chapter.json")) continue;
      String id = entry.getKey().substring(0, entry.getKey().length() - ".chapter.json".length());
      PropertyFile properties = file(files, id + ".properties");
      JsonValue scenes = entry.getValue().get("scenes");
      if (scenes == null) continue;
      for (JsonValue scene : scenes) {
        extractVerbs(scene.get("verbs"), scene.name, null, properties);
        JsonValue actors = scene.get("actors");
        if (actors == null) continue;
        for (JsonValue actor : actors) {
          localize(actor, "desc", key(scene.name, actor.name, "desc"), properties);
          extractVerbs(actor.get("verbs"), scene.name, actor.name, properties);
          extractDialogs(actor.get("dialogs"), scene.name, actor.name, properties);
          JsonValue renderer = actor.get("renderer");
          if (renderer != null && renderer.getString("class", "").endsWith("TextRenderer"))
            localize(renderer, "text", key(scene.name, actor.name, "text"), properties);
        }
      }
    }

    for (Map.Entry<String, PropertyFile> entry : files.entrySet())
      result.put(entry.getKey(), entry.getValue().bytes());
    return result;
  }

  private static PropertyFile file(Map<String, PropertyFile> files, String name) {
    PropertyFile value = files.get(name);
    if (value == null) {
      value = new PropertyFile(new byte[0]);
      files.put(name, value);
    }
    return value;
  }

  private static void extractVerbs(
      JsonValue verbs, String scene, String actor, PropertyFile properties) {
    if (verbs == null) return;
    for (JsonValue verb : verbs) {
      JsonValue actions = verb.get("actions");
      if (actions == null) continue;
      int index = 0;
      for (JsonValue action : actions) {
        ArrayList<String> fields = new ArrayList<>();
        for (JsonValue field : action)
          if (field.name != null
              && field.name.toLowerCase(Locale.ROOT).endsWith("text")
              && field.isString()) fields.add(field.name);
        for (String field : fields)
          localize(action, field, key(scene, actor, verb.name, index, field), properties);
        index++;
      }
    }
  }

  private static void extractDialogs(
      JsonValue dialogs, String scene, String actor, PropertyFile properties) {
    if (dialogs == null) return;
    for (JsonValue dialog : dialogs) {
      JsonValue options = dialog.get("options");
      if (options == null) continue;
      int index = 0;
      for (JsonValue option : options) {
        localize(option, "text", key(scene, actor, dialog.name, index, "text"), properties);
        localize(
            option,
            "responseText",
            key(scene, actor, dialog.name, index, "responseText"),
            properties);
        index++;
      }
    }
  }

  private static void localize(
      JsonValue owner, String field, String baseKey, PropertyFile properties) {
    JsonValue value = owner.get(field);
    if (value == null || !value.isString()) return;
    String text = value.asString();
    if (text.isEmpty() || text.charAt(0) == I18N.PREFIX) return;
    String translationKey = properties.add(baseKey, text);
    owner.remove(field);
    owner.addChild(field, new JsonValue(I18N.PREFIX + translationKey));
  }

  private static String key(String scene, String actor, String property) {
    return scene + "." + actor + "." + property;
  }

  private static String key(String scene, String actor, String parent, int index, String property) {
    return (scene == null ? "default" : scene)
        + (actor == null ? "" : "." + actor)
        + "."
        + parent
        + "."
        + index
        + "."
        + property;
  }

  private static final class PropertyFile {
    private final byte[] original;
    private final Set<String> keys = new LinkedHashSet<>();
    private final StringBuilder additions = new StringBuilder();

    PropertyFile(byte[] original) {
      this.original = original == null ? new byte[0] : original;
      Properties parsed = new Properties();
      try {
        parsed.load(
            new InputStreamReader(new ByteArrayInputStream(this.original), StandardCharsets.UTF_8));
        keys.addAll(parsed.stringPropertyNames());
      } catch (Exception failure) {
        throw new IllegalArgumentException("Invalid translation bundle", failure);
      }
    }

    String add(String base, String value) {
      String key = base.replaceAll("\\d*$", "");
      if (key.isEmpty()) key = base;
      int suffix = 0;
      while (keys.contains(key)) key = base + (++suffix);
      keys.add(key);
      additions.append(escape(key, true)).append('=').append(escape(value, false)).append('\n');
      return key;
    }

    byte[] bytes() {
      if (additions.length() == 0) return original;
      StringBuilder output = new StringBuilder(new String(original, StandardCharsets.UTF_8));
      if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') output.append('\n');
      output.append(additions);
      return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value, boolean key) {
      StringBuilder output = new StringBuilder();
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        switch (c) {
          case '\\':
            output.append("\\\\");
            break;
          case '\t':
            output.append("\\t");
            break;
          case '\n':
            output.append("\\n");
            break;
          case '\r':
            output.append("\\r");
            break;
          case '\f':
            output.append("\\f");
            break;
          default:
            if ((i == 0 && c == ' ') || "=:#!".indexOf(c) >= 0 || (key && c == ' '))
              output.append('\\');
            output.append(c);
        }
      }
      return output.toString();
    }
  }
}
