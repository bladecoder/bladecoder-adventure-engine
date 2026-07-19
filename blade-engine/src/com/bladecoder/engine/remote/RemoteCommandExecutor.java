package com.bladecoder.engine.remote;

import java.io.IOException;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;

/** Executes remote commands on the LibGDX render thread. */
final class RemoteCommandExecutor {
    private static final int SCREENSHOT_WIDTH = 1920;

    private final World world;
    private final RemoteErrorReporter errorReporter;

    RemoteCommandExecutor(World world, RemoteErrorReporter errorReporter) {
        this.world = world;
        this.errorReporter = errorReporter;
    }

    void execute(RemoteCommand command) {
        Scene scene = world.getCurrentScene();
        if (scene == null) {
            errorReporter.reportError("No current scene available for remote command");
            return;
        }

        switch (command.type) {
        case ACTOR_VERB:
            executeActorVerb(scene, command);
            break;
        case SCENE_VERB:
            scene.runVerb(command.verb);
            break;
        case DIALOG_OPTION:
            executeDialogOption(command);
            break;
        case GOTO:
            executeGoto(scene, command);
            break;
        case SAVE_GAME:
            executeSaveGame(command);
            break;
        case SCREENSHOT:
            world.takeScreenshot(command.target, SCREENSHOT_WIDTH);
            break;
        default:
            errorReporter.reportError("Unknown remote command");
            break;
        }
    }

    private void executeActorVerb(Scene scene, RemoteCommand command) {
        BaseActor baseActor = scene.getActor(command.actorId, true);
        if (baseActor instanceof InteractiveActor) {
            ((InteractiveActor) baseActor).runVerb(command.verb, command.target);
        } else {
            errorReporter.reportError("Remote command actor not found: " + command.actorId);
        }
    }

    private void executeDialogOption(RemoteCommand command) {
        if (!world.hasDialogOptions() || command.option < 0 || command.option >= world.getDialogOptions().size()) {
            errorReporter.reportError("Remote command dialog option is not available: " + command.option);
        } else {
            world.selectDialogOption(command.option);
        }
    }

    private void executeGoto(Scene scene, RemoteCommand command) {
        if (scene.getPlayer() == null) {
            errorReporter.reportError("Remote goto command requires a scene player");
        } else {
            scene.getPlayer().goTo(new Vector2(command.x, command.y), null, false);
        }
    }

    private void executeSaveGame(RemoteCommand command) {
        try {
            world.getSerializer().saveGameState(command.target, true);
        } catch (IOException e) {
            errorReporter.reportError("Remote save game failed: " + e.getMessage());
        }
    }
}
