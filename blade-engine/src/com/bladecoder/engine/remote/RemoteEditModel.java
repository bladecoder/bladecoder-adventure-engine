package com.bladecoder.engine.remote;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.*;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.ActionProperty;
import com.bladecoder.engine.actions.CameraAction;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.EngineLogger;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** MODEL authoring workspace. Never serializes a running game's STATE. */
final class RemoteEditModel {
  private final Path root;
  private final boolean loadTypedModels;
  private final boolean allowMissingReferences;
  private final List<String> missingReferences = new ArrayList<>();
  private final RemoteEditSchema schema = new RemoteEditSchema();
  private Map<String, JsonValue> documents = new LinkedHashMap<>();
  private Map<String, World> models = new LinkedHashMap<>();
  private final Map<String, byte[]> originals = new LinkedHashMap<>();
  private final Set<String> dirty = new LinkedHashSet<>();
  long revision;

  RemoteEditModel(Path project) throws IOException {
    this(project, true);
  }

  /** The headless protocol fixture keeps its filesystem checks without creating GL resources. */
  RemoteEditModel(Path project, boolean loadTypedModels) throws IOException {
    this(project, loadTypedModels, false);
  }

  RemoteEditModel(Path project, boolean loadTypedModels, boolean allowMissingReferences)
      throws IOException {
    this.loadTypedModels = loadTypedModels && !allowMissingReferences;
    this.allowMissingReferences = allowMissingReferences;
    root = project.toRealPath().resolve("assets/model").toRealPath();
    if (!root.startsWith(project.toRealPath()))
      throw new IOException("Model directory escapes project");
    if (Files.exists(root.resolve(".edit-recovery")))
      throw new IOException(
          "Unfinished edit save: restore files from .edit-recovery before editing");
    try (DirectoryStream<Path> files = Files.newDirectoryStream(root)) {
      for (Path file : files) {
        String name = file.getFileName().toString();
        if (name.equals("world") || name.equals("world.json") || name.endsWith(".chapter.json")) {
          if (Files.isSymbolicLink(file))
            throw new IOException("Symlink model files are unsupported");
          byte[] bytes = Files.readAllBytes(file);
          originals.put(name, bytes);
          documents.put(name, new JsonReader().parse(new String(bytes, StandardCharsets.UTF_8)));
        }
      }
    }
    validate(documents);
    if (loadTypedModels) models = loadModels(documents);
  }

  private String worldName() {
    return documents.containsKey("world") ? "world" : "world.json";
  }

  String initialChapter() {
    return documents.get(worldName()).getString("initChapter");
  }

  JsonValue world() {
    return documents.get(worldName());
  }

  JsonValue chapter(String id) {
    return documents.get(id + ".chapter.json");
  }

  Object status() {
    return RemoteDebugState.map(
        "revision",
        revision,
        "dirty",
        new ArrayList<>(dirty),
        "missing_references",
        new ArrayList<>(missingReferences));
  }

  private void missing(String message, List<String> warnings) {
    if (!allowMissingReferences) throw new IllegalArgumentException(message);
    warnings.add(message);
  }

  static JsonValue copy(JsonValue value) {
    if (value.isNull()) return new JsonValue(JsonValue.ValueType.nullValue);
    return new JsonReader().parse(value.toJson(JsonWriter.OutputType.json));
  }

  private static String collection(String kind) {
    switch (kind) {
      case "scene":
        return "scenes";
      case "actor":
        return "actors";
      case "layer":
        return "layers";
      case "verb":
        return "verbs";
      case "action":
        return "actions";
      case "dialog":
        return "dialogs";
      case "dialog_option":
        return "options";
      case "animation":
        return "fanims";
      case "sound":
        return "sounds";
      case "renderer":
        return "renderer";
      case "geometry":
        return "bbox";
      default:
        throw new IllegalArgumentException("Unsupported entity kind: " + kind);
    }
  }

  private JsonValue resolve(Map<String, JsonValue> docs, JsonValue ref) {
    String kind = ref.getString("kind");
    if (kind.equals("world")) return required(docs.get(worldName()));
    if (kind.equals("chapter"))
      return required(docs.get(safeId(ref.getString("id")) + ".chapter.json"));
    JsonValue parent = resolve(docs, ref.require("parent"));
    JsonValue container = required(parent.get(collection(kind)));
    if (kind.equals("renderer") || kind.equals("geometry")) return container;
    return required(
        container.isArray()
            ? container.get(ref.getInt("index"))
            : container.get(ref.getString("id")));
  }

  private static JsonValue required(JsonValue value) {
    if (value == null) throw new IllegalArgumentException("Entity does not exist");
    return value;
  }

  private static String safeId(String id) {
    if (id.isEmpty()
        || id.equals(".")
        || id.equals("..")
        || id.contains("/")
        || id.contains("\\")
        || id.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid identifier");
    return id;
  }

  Object query(JsonValue args) {
    if (!args.has("target"))
      return RemoteDebugState.map(
          "revision", revision, "documents", new ArrayList<>(documents.keySet()));
    JsonValue value = resolve(documents, args.require("target"));
    return RemoteDebugState.map("revision", revision, "value", RemoteDebugState.value(value));
  }

  interface Preview {
    void validate() throws Exception;
  }

  void applyWithPreview(JsonValue commands, long expected, Preview preview) throws Exception {
    Map<String, JsonValue> old = documents;
    Map<String, World> oldModels = models;
    Set<String> oldDirty = new LinkedHashSet<>(dirty);
    long oldRevision = revision;
    try {
      applyInternal(commands, expected);
      preview.validate();
      disposeModels(oldModels);
    } catch (Exception failure) {
      if (models != oldModels) disposeModels(models);
      documents = old;
      models = oldModels;
      dirty.clear();
      dirty.addAll(oldDirty);
      revision = oldRevision;
      throw failure;
    }
  }

  void apply(JsonValue commands, long expected) {
    Map<String, World> oldModels = models;
    applyInternal(commands, expected);
    disposeModels(oldModels);
  }

  private void applyInternal(JsonValue commands, long expected) {
    if (expected != revision) throw new IllegalStateException("Revision conflict");
    Map<String, JsonValue> next = new LinkedHashMap<>();
    for (Map.Entry<String, JsonValue> doc : documents.entrySet())
      next.put(doc.getKey(), copy(doc.getValue()));
    for (JsonValue command : commands)
      mutate(next, command.getString("operation"), command.require("arguments"));
    validate(next);
    Map<String, World> nextModels = loadTypedModels ? loadModels(next) : new LinkedHashMap<>();
    Set<String> changed = new LinkedHashSet<>(documents.keySet());
    changed.addAll(next.keySet());
    try {
      for (String name : changed) {
        if (Objects.equals(encoded(documents.get(name)), encoded(next.get(name)))) continue;
        if (!loadTypedModels) continue;
        if (name.equals(worldName())) {
          World model = nextModels.values().iterator().next();
          next.put(name, new JsonReader().parse(model.getSerializer().worldDescToJson()));
        } else if (name.endsWith(".chapter.json") && nextModels.containsKey(name)) {
          next.put(
              name, new JsonReader().parse(nextModels.get(name).getSerializer().modelToJson()));
        }
      }
      validate(next);
    } catch (RuntimeException failure) {
      disposeModels(nextModels);
      throw failure;
    }
    for (String name : changed)
      if (!Objects.equals(encoded(documents.get(name)), encoded(next.get(name)))) dirty.add(name);
    documents = next;
    models = nextModels;
    revision++;
  }

  private Map<String, World> loadModels(Map<String, JsonValue> docs) {
    Map<String, World> loaded = new LinkedHashMap<>();
    JsonValue description = required(docs.get(worldName()));
    float scale = EngineAssetManager.getInstance().getScale();
    try {
      for (Map.Entry<String, JsonValue> entry : docs.entrySet()) {
        if (!entry.getKey().endsWith(".chapter.json")) continue;
        World model = new World();
        try {
          model.setWidth((int) (description.getInt("width") * scale));
          model.setHeight((int) (description.getInt("height") * scale));
          model.setInitChapter(description.getString("initChapter"));
          BladeJson json = new BladeJson(model, BladeJson.Mode.MODEL, false);
          json.setIgnoreUnknownFields(true);
          model.getVerbManager().read(json, copy(description));
          model.getSerializer().read(json, copy(entry.getValue()));
          model.setChapter(
              entry.getKey().substring(0, entry.getKey().length() - ".chapter.json".length()));
          loaded.put(entry.getKey(), model);
        } catch (RuntimeException failure) {
          model.dispose();
          throw failure;
        }
      }
      return loaded;
    } catch (RuntimeException failure) {
      disposeModels(loaded);
      throw failure;
    }
  }

  private static void disposeModels(Map<String, World> values) {
    for (World model : values.values()) model.dispose();
  }

  void close() {
    disposeModels(models);
    models.clear();
  }

  private static String encoded(JsonValue value) {
    return value == null ? null : value.toJson(JsonWriter.OutputType.json);
  }

  private void mutate(Map<String, JsonValue> docs, String operation, JsonValue args) {
    JsonValue ref = args.require("target");
    String kind = ref.getString("kind");
    if (operation.equals("entity.update")) {
      JsonValue entity = resolve(docs, ref);
      if (kind.equals("geometry")) {
        JsonValue owner = resolve(docs, ref.require("parent"));
        owner.remove("bbox");
        owner.addChild("bbox", copy(args.require("value")));
        return;
      }
      if (!entity.isObject() || !args.require("fields").isObject())
        throw new IllegalArgumentException("fields must update an object entity");
      for (JsonValue field : args.require("fields")) {
        if (entity.has(field.name)
            && entity.get(field.name).type() != field.type()
            && !(entity.get(field.name).isNumber() && field.isNumber())
            && !entity.get(field.name).isNull()
            && !field.isNull())
          throw new IllegalArgumentException("Property type mismatch: " + field.name);
        entity.remove(field.name);
        entity.addChild(field.name, copy(field));
      }
      JsonValue unset = args.get("unset");
      if (unset != null) for (JsonValue field : unset) entity.remove(field.asString());
      return;
    }
    if (kind.equals("world"))
      throw new IllegalArgumentException("World cannot be created, deleted or renamed");
    String oldId = ref.getString("id", null);
    if (kind.equals("chapter")) {
      String name = safeId(oldId) + ".chapter.json";
      if (operation.equals("entity.create")) {
        if (docs.containsKey(name)) throw new IllegalArgumentException("Duplicate chapter");
        docs.put(name, copy(args.require("value")));
      } else if (operation.equals("entity.delete")) required(docs.remove(name));
      else if (operation.equals("entity.rename")) {
        String dest = safeId(args.getString("id")) + ".chapter.json";
        if (docs.containsKey(dest)) throw new IllegalArgumentException("Duplicate chapter");
        docs.put(dest, required(docs.remove(name)));
      } else throw new IllegalArgumentException("Unknown operation");
      return;
    }
    JsonValue parent = resolve(docs, ref.require("parent"));
    String key = collection(kind);
    JsonValue container = parent.get(key);
    boolean singleton = kind.equals("renderer") || kind.equals("geometry");
    if (container == null && operation.equals("entity.create") && !singleton) {
      container =
          new JsonValue(
              Arrays.asList("actions", "options", "layers").contains(key)
                  ? JsonValue.ValueType.array
                  : JsonValue.ValueType.object);
      parent.addChild(key, container);
    }
    if (singleton) {
      if (operation.equals("entity.create")) {
        if (container != null) throw new IllegalArgumentException("Entity exists");
        parent.addChild(key, copy(args.require("value")));
      } else if (operation.equals("entity.delete")) required(parent.remove(key));
      else throw new IllegalArgumentException("Singleton cannot be renamed");
      return;
    }
    required(container);
    if (container.isArray()) {
      int index = ref.getInt("index");
      if (operation.equals("entity.delete")) required(container.remove(index));
      else if (operation.equals("entity.create") && index == container.size)
        container.addChild(copy(args.require("value")));
      else
        throw new IllegalArgumentException(
            "Create list entities by appending; update/delete by index");
      return;
    }
    if (operation.equals("entity.create")) {
      if (container.has(oldId)) throw new IllegalArgumentException("Entity exists");
      container.addChild(oldId, copy(args.require("value")));
    } else if (operation.equals("entity.delete")) required(container.remove(oldId));
    else if (operation.equals("entity.rename")) {
      String id = safeId(args.getString("id"));
      if (container.has(id)) throw new IllegalArgumentException("Entity exists");
      JsonValue value = required(container.remove(oldId));
      if (value.has("id")) {
        value.remove("id");
        value.addChild("id", new JsonValue(id));
      }
      container.addChild(id, value);
    } else throw new IllegalArgumentException("Unknown operation");
  }

  private void validate(Map<String, JsonValue> docs) {
    List<String> warnings = new ArrayList<>();
    JsonValue world = required(docs.get(worldName()));
    schema.validate(world, "world");
    if (world.getInt("width") <= 0 || world.getInt("height") <= 0)
      throw new IllegalArgumentException("World dimensions must be positive");
    String initial = world.getString("initChapter");
    if (!docs.containsKey(initial + ".chapter.json"))
      missing("Missing initial chapter: " + initial, warnings);
    for (Map.Entry<String, JsonValue> doc : docs.entrySet())
      if (doc.getKey().endsWith(".chapter.json")) {
        JsonValue chapter = doc.getValue(), scenes = chapter.require("scenes");
        schema.validate(chapter, "chapter");
        if (!scenes.isObject())
          throw new IllegalArgumentException("Invalid scenes in " + doc.getKey());
        if (!scenes.has(chapter.getString("initScene")))
          missing(
              "Missing initial scene in " + doc.getKey() + ": " + chapter.getString("initScene"),
              warnings);
        for (JsonValue scene : scenes) {
          if (!scene.name.equals(scene.getString("id")))
            throw new IllegalArgumentException("Scene key/id mismatch");
          JsonValue actors = scene.require("actors");
          String player = scene.getString("player", null);
          if (player != null && !actors.has(player))
            missing(
                doc.getKey() + "/scenes/" + scene.name + "/player: missing actor " + player,
                warnings);
          references(scene, scenes, scene, docs, doc.getKey() + "/scenes/" + scene.name, warnings);
          for (JsonValue actor : actors) {
            if (!actor.name.equals(actor.getString("id")))
              throw new IllegalArgumentException("Actor key/id mismatch");
            JsonValue bbox = actor.get("bbox");
            if (bbox != null) {
              if (!bbox.isArray() || bbox.size % 2 != 0 || (bbox.size > 0 && bbox.size < 6))
                throw new IllegalArgumentException("Invalid polygon: " + actor.name);
              for (JsonValue coordinate : bbox)
                if (!coordinate.isNumber() || !Double.isFinite(coordinate.asDouble()))
                  throw new IllegalArgumentException("Invalid polygon coordinate");
            }
            String layer = actor.getString("layer", null);
            if (layer != null && scene.has("layers")) {
              boolean found = false;
              for (JsonValue candidate : scene.get("layers"))
                if (layer.equals(candidate.getString("name"))) found = true;
              if (!found)
                missing(
                    doc.getKey()
                        + "/scenes/"
                        + scene.name
                        + "/actors/"
                        + actor.name
                        + "/layer: missing layer "
                        + layer,
                    warnings);
            }
            JsonValue renderer = actor.get("renderer");
            if (renderer != null && !hasInitialAnimation(renderer))
              missing(
                  doc.getKey()
                      + "/scenes/"
                      + scene.name
                      + "/actors/"
                      + actor.name
                      + ": missing initial animation",
                  warnings);
          }
        }
      }
    if (allowMissingReferences) {
      for (String warning : warnings)
        if (!missingReferences.contains(warning)) {
          if (Gdx.app == null) System.err.println("EDIT WARNING: " + warning);
          else EngineLogger.error("EDIT WARNING: " + warning);
        }
      missingReferences.clear();
      missingReferences.addAll(warnings);
    }
  }

  static boolean hasInitialAnimation(JsonValue renderer) {
    if (!renderer.has("initAnimation") || renderer.get("initAnimation").isNull()) return true;
    if (!renderer.has("fanims")) return false;
    // Use the runtime's own mirroring/directional lookup without loading assets or advancing it.
    AtlasRenderer lookup = new AtlasRenderer();
    for (JsonValue animation : renderer.get("fanims")) {
      AnimationDesc descriptor = new AnimationDesc();
      descriptor.id = animation.name;
      lookup.addAnimation(descriptor);
    }
    return lookup.hasAnimation(renderer.getString("initAnimation"));
  }

  private void references(
      JsonValue node,
      JsonValue scenes,
      JsonValue scene,
      Map<String, JsonValue> docs,
      String path,
      List<String> warnings) {
    if (node.isObject() && node.has("class")) {
      Class<?> type = ActionFactory.getClassTags().get(node.getString("class"));
      if (type == null)
        for (Class<?> candidate : ActionFactory.getClassTags().values())
          if (candidate.getName().equals(node.getString("class"))) type = candidate;
      if (type != null)
        for (Field field : type.getDeclaredFields()) {
          ActionProperty property = field.getAnnotation(ActionProperty.class);
          JsonValue value = node.get(field.getName());
          if (property == null || value == null || !value.isString()) continue;
          String ref = value.asString(), semantic = property.type().name();
          // Camera's documented "none" sentinel disables following; it is not an actor reference.
          if (type == CameraAction.class
              && field.getName().equals("followActor")
              && ref.equals("none")) continue;
          if (ref.isEmpty()
              || ref.startsWith("$")
              || ref.equals("inventory")
              || ref.equals("previous")) continue;
          if (semantic.equals("CHAPTER") && !docs.containsKey(ref + ".chapter.json"))
            missing(path + "/" + field.getName() + ": missing chapter " + ref, warnings);
          if (semantic.equals("SOUND")) {
            boolean found = false;
            for (JsonValue document : docs.values())
              if (document.has("sounds") && document.get("sounds").has(ref)) found = true;
            if (!found) missing(path + "/" + field.getName() + ": missing sound " + ref, warnings);
          }
          if (semantic.equals("SCENE") && !scenes.has(ref))
            missing(path + "/" + field.getName() + ": missing scene " + ref, warnings);
          if (semantic.contains("ACTOR") && !semantic.equals("ACTOR_ANIMATION")) {
            JsonValue owner = scene;
            String actor = ref;
            int separator = ref.indexOf('#');
            if (separator >= 0) {
              owner = scenes.get(ref.substring(0, separator));
              actor = ref.substring(separator + 1);
              if (owner == null) {
                missing(path + "/" + field.getName() + ": missing scene in " + ref, warnings);
                continue;
              }
            }
            if (owner != null && !actor.startsWith("$") && !owner.require("actors").has(actor)) {
              boolean exists = false;
              if (separator < 0)
                for (JsonValue candidate : scenes)
                  if (candidate.require("actors").has(actor)) exists = true;
              if (!exists)
                missing(path + "/" + field.getName() + ": missing actor " + ref, warnings);
            }
          }
        }
    }
    for (JsonValue child : node)
      references(
          child,
          scenes,
          scene,
          docs,
          path + "/" + (child.name == null ? "[]" : child.name),
          warnings);
  }

  void save(long expected) throws IOException {
    Path lockFile = root.getParent().getParent().resolve(".blade-edit.lock");
    try (FileChannel channel =
        FileChannel.open(
            lockFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS)) {
      try (FileLock lock = channel.tryLock()) {
        if (lock == null) throw new IOException("Another editor is saving this project");
        saveLocked(expected);
      } catch (OverlappingFileLockException busy) {
        throw new IOException("Another editor is saving this project", busy);
      }
    }
  }

  private void saveLocked(long expected) throws IOException {
    if (expected != revision) throw new IllegalStateException("Revision conflict");
    for (String name : documents.keySet())
      if (Files.isSymbolicLink(root.resolve(name)))
        throw new IOException("Symlink model file: " + name);
    for (String name : originals.keySet())
      if (!Files.exists(root.resolve(name))
          || !Arrays.equals(originals.get(name), Files.readAllBytes(root.resolve(name))))
        throw new IOException("External modification: " + name);
    for (String name : dirty)
      if (!originals.containsKey(name) && Files.exists(root.resolve(name)))
        throw new IOException("External file exists: " + name);
    if (dirty.isEmpty()) return;
    Path recovery = root.resolve(".edit-recovery");
    Files.createDirectory(recovery);
    try {
      for (String name : dirty)
        if (originals.containsKey(name)) Files.write(recovery.resolve(name), originals.get(name));
      Files.write(
          recovery.resolve("manifest"), String.join("\n", dirty).getBytes(StandardCharsets.UTF_8));
      for (String name : dirty) {
        if (!documents.containsKey(name)) {
          Files.delete(root.resolve(name));
          continue;
        }
        Path tmp = Files.createTempFile(root, ".edit-", ".tmp");
        try {
          String content;
          if (!loadTypedModels)
            content = documents.get(name).prettyPrint(JsonWriter.OutputType.json, 0) + "\n";
          else if (name.equals(worldName()))
            content = models.values().iterator().next().getSerializer().worldDescToJson();
          else content = models.get(name).getSerializer().modelToJson();
          Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
          Files.move(
              tmp,
              root.resolve(name),
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(tmp);
        }
      }
    } catch (IOException failure) {
      for (String name : dirty) {
        if (originals.containsKey(name)) Files.write(root.resolve(name), originals.get(name));
        else Files.deleteIfExists(root.resolve(name));
      }
      try (DirectoryStream<Path> files = Files.newDirectoryStream(recovery)) {
        for (Path file : files) Files.delete(file);
      }
      Files.delete(recovery);
      throw failure;
    }
    originals.clear();
    for (String name : documents.keySet())
      originals.put(name, Files.readAllBytes(root.resolve(name)));
    try (DirectoryStream<Path> files = Files.newDirectoryStream(recovery)) {
      for (Path file : files) Files.delete(file);
    }
    Files.delete(recovery);
    dirty.clear();
  }
}
