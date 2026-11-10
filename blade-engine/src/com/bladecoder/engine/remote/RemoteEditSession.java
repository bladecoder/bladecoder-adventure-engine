package com.bladecoder.engine.remote;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.*;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.*;
import com.bladecoder.engine.serialization.BladeJson;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.Deflater;

/** Isolated static preview and render-thread authoring endpoint. */
final class RemoteEditSession {
  final String token = UUID.randomUUID().toString();
  private final RemoteEditModel model;
  private final Map<String, String> requests = new LinkedHashMap<>();
  private final Map<String, Object> results = new LinkedHashMap<>();
  private final Map<String, byte[]> artifacts = new LinkedHashMap<>();
  private final ShapeRenderer lines = new ShapeRenderer();
  private World preview;
  private final SpriteBatch batch = new SpriteBatch();
  private Scene scene;
  private String chapterId, sceneId;

  RemoteEditSession(String project, String chapter) throws Exception {
    model = new RemoteEditModel(Paths.get(project));
    chapterId = chapter == null ? model.initialChapter() : chapter;
    select(chapterId, null);
  }

  private void select(String chapter, String selected) throws Exception {
    JsonValue source = model.chapter(chapter);
    if (source == null) throw new IllegalArgumentException("Unknown chapter");
    World next = new World();
    try {
      next.loadWorldDesc();
      JsonValue world = model.world();
      float scale = EngineAssetManager.getInstance().getScale();
      next.setWidth((int) (world.getInt("width") * scale));
      next.setHeight((int) (world.getInt("height") * scale));
      next.getSerializer()
          .read(new BladeJson(next, BladeJson.Mode.MODEL, false), RemoteEditModel.copy(source));
      String id = selected == null ? source.getString("initScene") : selected;
      Scene nextScene = next.getScenes().get(id);
      if (nextScene == null) throw new IllegalArgumentException("Unknown scene");
      next.setChapter(chapter);
      next.setCurrentScene(nextScene, false, null);
      next.getMusicManager().setEnabled(false);
      nextScene.getSoundManager().setEnabled(false);
      for (BaseActor actor : nextScene.getActors().values())
        if (actor instanceof SpriteActor) {
          ActorRenderer renderer = ((SpriteActor) actor).getRenderer();
          if (renderer != null) renderer.setPreviewEventsEnabled(false);
        }
      nextScene.loadAssets();
      EngineAssetManager.getInstance().finishLoading();
      nextScene.retrieveAssets();
      nextScene.resetCamera(next.getWidth(), next.getHeight());
      for (SceneLayer layer : nextScene.getLayers()) layer.update();
      if (preview != null) preview.dispose();
      preview = next;
      scene = nextScene;
      chapterId = chapter;
      sceneId = id;
    } catch (Exception failure) {
      next.dispose();
      throw failure;
    }
  }

  void render(boolean debugLines) {
    Gdx.input.setInputProcessor(null);
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    if (scene == null) return;
    scene.getCamera().viewportWidth = preview.getWidth();
    scene.getCamera().viewportHeight = preview.getHeight();
    scene.getCamera().update();
    scene.draw(batch);
    if (debugLines) {
      lines.setProjectionMatrix(scene.getCamera().combined);
      scene.drawBBoxLines(lines);
      lines.end();
    }
  }

  RemoteHttpResponse route(RemoteHttpRequest request) throws Exception {
    if (!("Bearer " + token).equals(request.headers.get("authorization")))
      return response(401, "Authentication required");
    FutureTask<RemoteHttpResponse> task = new FutureTask<>(() -> routeOnRenderThread(request));
    Gdx.app.postRunnable(task);
    try {
      return task.get(30, TimeUnit.SECONDS);
    } catch (TimeoutException failure) {
      task.cancel(false);
      return response(504, "Edit operation pending; query its request id before retrying");
    }
  }

  private RemoteHttpResponse response(int status, Object value) {
    return new RemoteHttpResponse(status, RemoteJson.toJson(value));
  }

  private RemoteHttpResponse routeOnRenderThread(RemoteHttpRequest request) {
    try {
      String path = request.path.substring("/edit/v1".length());
      if (request.method.equals("GET") && path.equals("/session"))
        return response(
            200,
            RemoteDebugState.map(
                "mode",
                "edit",
                "protocol",
                1,
                "capabilities",
                Arrays.asList("entity.crud", "batch", "model.save", "view.select", "capture"),
                "limits",
                RemoteDebugState.map("body_bytes", 65536, "commands", 1024, "artifacts", 128),
                "model",
                model.status(),
                "chapter",
                chapterId,
                "scene",
                sceneId,
                "viewport",
                RemoteDebugState.map(
                    "width",
                    preview.getWidth(),
                    "height",
                    preview.getHeight(),
                    "x",
                    scene.getCamera().position.x,
                    "y",
                    scene.getCamera().position.y,
                    "scale",
                    EngineAssetManager.getInstance().getScale())));
      if (request.method.equals("GET") && path.startsWith("/commands/"))
        return results.containsKey(path.substring(10))
            ? response(200, results.get(path.substring(10)))
            : response(404, "Unknown request");
      if (request.method.equals("GET") && path.startsWith("/artifacts/")) {
        byte[] data = artifacts.get(path.substring(11));
        return data == null
            ? response(404, "Unknown artifact")
            : new RemoteHttpResponse(data, "image/png");
      }
      JsonValue body = request.body.isEmpty() ? null : new JsonReader().parse(request.body);
      if (request.method.equals("POST") && path.equals("/query"))
        return response(200, model.query(body));
      if (!request.method.equals("POST") || !path.equals("/commands"))
        return response(404, "Unknown edit endpoint");
      String id = body.getString("request_id");
      if (requests.containsKey(id))
        return requests.get(id).equals(request.body)
            ? response(200, results.get(id))
            : response(409, "Request id conflict");
      if (requests.size() >= 1024) return response(429, "Session command history full");
      if (body.getLong("expected_revision") != model.revision)
        return response(409, "Revision conflict");
      String operation = body.getString("operation");
      JsonValue args = body.require("arguments");
      Object value;
      if (operation.equals("model.save")) {
        model.save(model.revision);
        value = model.status();
      } else if (operation.equals("view.select")) {
        select(args.getString("chapter", chapterId), args.getString("scene", null));
        value = model.status();
      } else if (operation.equals("capture")) {
        render(args.getBoolean("debug_lines", false));
        Pixmap image =
            ScreenUtils.getFrameBufferPixmap(
                0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        FileHandle tmp = new FileHandle(File.createTempFile("blade-edit-", ".png"));
        try {
          PixmapIO.writePNG(tmp, image, Deflater.DEFAULT_COMPRESSION, true);
          if (artifacts.size() >= 128) artifacts.remove(artifacts.keySet().iterator().next());
          artifacts.put(id, tmp.readBytes());
        } finally {
          image.dispose();
          tmp.delete();
        }
        value = RemoteDebugState.map("artifact", id, "revision", model.revision);
      } else {
        JsonValue commands;
        if (operation.equals("batch")) commands = args.require("operations");
        else {
          commands = new JsonValue(JsonValue.ValueType.array);
          commands.addChild(RemoteEditModel.copy(body));
        }
        model.applyWithPreview(
            commands,
            model.revision,
            () -> {
              String chapter =
                  model.chapter(chapterId) == null ? model.initialChapter() : chapterId;
              JsonValue valueModel = model.chapter(chapter);
              select(chapter, valueModel.require("scenes").has(sceneId) ? sceneId : null);
            });
        value = model.status();
      }
      Object result =
          RemoteDebugState.map("status", "completed", "request_id", id, "result", value);
      requests.put(id, request.body);
      results.put(id, result);
      return response(200, result);
    } catch (IllegalStateException conflict) {
      return response(409, RemoteDebugState.map("error", conflict.getMessage()));
    } catch (Exception failure) {
      return response(400, RemoteDebugState.map("error", failure.getMessage()));
    }
  }

  void close() {
    if (preview != null) preview.dispose();
    model.close();
    lines.dispose();
    batch.dispose();
  }
}
