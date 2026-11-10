package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/** Authoring transactions tested without graphics or a running game. */
final class RemoteEditModelTest {
  static JsonValue json(String text) {
    return new JsonReader().parse(text);
  }

  static int run() throws Exception {
    RemoteProtocolTest.check(
        RemoteEditModel.copy(new JsonValue(JsonValue.ValueType.nullValue)).isNull(),
        "nullable MODEL fields survive copying");
    RemoteProtocolTest.check(
        RemoteEditModel.hasInitialAnimation(
            json("{initAnimation:stand.left,fanims:{stand.right:{}}}")),
        "mirrored initial animation is valid");
    RemoteProtocolTest.check(
        !RemoteEditModel.hasInitialAnimation(
            json("{initAnimation:walk.left,fanims:{stand.right:{}}}")),
        "missing animation is still rejected");
    Path project = Files.createTempDirectory("blade-edit-model-");
    Path root = Files.createDirectories(project.resolve("assets/model"));
    try {
      Files.write(
          root.resolve("world"),
          "{width:800,height:600,initChapter:\"00\",verbs:{}}".getBytes(StandardCharsets.UTF_8));
      Files.write(
          root.resolve("00.chapter.json"),
          "{initScene:room,scenes:{room:{id:room,layers:[],actors:{hero:{id:hero,class:AnchorActor,bbox:[],pos:{x:0,y:0}}},player:hero,verbs:{init:{id:init,actions:[{class:Camera,followActor:none}]}}}}}"
              .getBytes(StandardCharsets.UTF_8));
      RemoteEditModel model = new RemoteEditModel(project, false);
      RemoteProtocolTest.check(model.revision == 0, "camera followActor none is a valid sentinel");
      JsonValue badCamera =
          json(
              "[{operation:entity.create,arguments:{target:{kind:verb,id:bad,parent:{kind:scene,id:room,parent:{kind:chapter,id:\"00\"}}},value:{id:bad,actions:[{class:Camera,target:none}]}}}]");
      try {
        model.apply(badCamera, 0);
        throw new AssertionError("Camera target none accepted as a sentinel");
      } catch (IllegalArgumentException expected) {
      }
      RemoteProtocolTest.check(
          model.revision == 0, "camera sentinel exception does not weaken target validation");
      JsonValue edit =
          json(
              "[{operation:entity.update,arguments:{target:{kind:actor,id:hero,parent:{kind:scene,id:room,parent:{kind:chapter,id:\"00\"}}},fields:{pos:{x:10,y:20}}}}]");
      model.apply(edit, 0);
      RemoteProtocolTest.check(model.revision == 1, "edit advances revision");
      RemoteProtocolTest.check(
          !new String(Files.readAllBytes(root.resolve("00.chapter.json")), StandardCharsets.UTF_8)
              .contains("10"),
          "edit does not implicitly save");
      try {
        model.apply(edit, 0);
        throw new AssertionError("stale revision accepted");
      } catch (IllegalStateException expected) {
      }
      JsonValue deletion =
          json(
              "[{operation:entity.delete,arguments:{target:{kind:actor,id:hero,parent:{kind:scene,id:room,parent:{kind:chapter,id:\"00\"}}}}}]");
      try {
        model.apply(deletion, 1);
        throw new AssertionError("broken player reference accepted");
      } catch (IllegalArgumentException expected) {
      }
      RemoteProtocolTest.check(model.revision == 1, "failed batch preserves revision");
      try {
        model.applyWithPreview(
            edit,
            1,
            () -> {
              throw new IllegalArgumentException("missing atlas");
            });
        throw new AssertionError();
      } catch (IllegalArgumentException expected) {
      }
      RemoteProtocolTest.check(model.revision == 1, "preview failure rolls back model");
      model.save(1);
      RemoteProtocolTest.check(
          new String(Files.readAllBytes(root.resolve("00.chapter.json")), StandardCharsets.UTF_8)
              .contains("10"),
          "explicit save persists authored content");
      RemoteEditModel reopened = new RemoteEditModel(project, false);
      RemoteProtocolTest.check(reopened.chapter("00") != null, "saved model reopens");
      Files.write(
          root.resolve("00.chapter.json"), "external edit".getBytes(StandardCharsets.UTF_8));
      try {
        model.save(1);
        throw new AssertionError("external edit overwritten");
      } catch (IOException expected) {
      }
      return 6;
    } finally {
      try (Stream<Path> paths = Files.walk(project)) {
        paths
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                });
      }
    }
  }
}
