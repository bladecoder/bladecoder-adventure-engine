package com.bladecoder.engine.remote;

import java.io.IOException;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI;

/** Executes remote commands on the LibGDX render thread. */
final class RemoteCommandExecutor {
    private static final int SCREENSHOT_WIDTH = 1920;

    private final UI ui;
    private final World world;
    private final RemoteErrorReporter errorReporter;

    RemoteCommandExecutor(UI ui, World world, RemoteErrorReporter errorReporter) {
        this.ui = ui;
        this.world = world;
        this.errorReporter = errorReporter;
    }

    void execute(RemoteCommand command) {
        if (world.isDisposed() && !canRunWhenDisposed(command)) {
            errorReporter.reportError("Remote command requires an active game. Start a new game, load a saved game, or continue first.");
            return;
        }

        showSceneScreen();

        if (world.isPaused() && command.type != RemoteCommand.Type.PAUSE) {
            errorReporter.reportError("Remote command cannot run while the game is paused. Use the pause command to resume it first.");
            return;
        }

        switch (command.type) {
        case NEW_GAME:
            executeNewGame();
            return;
        case LOAD_GAME:
            executeLoadGame(command);
            return;
        case CONTINUE:
            executeContinue();
            return;
        case PAUSE:
            togglePause();
            return;
        default:
            break;
        }

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

    private boolean canRunWhenDisposed(RemoteCommand command) {
        return command.type == RemoteCommand.Type.NEW_GAME || command.type == RemoteCommand.Type.LOAD_GAME
                || command.type == RemoteCommand.Type.CONTINUE;
    }

    private void showSceneScreen() {
        boolean wasPaused = world.isPaused();
        if (!world.isDisposed() && !(ui.getCurrentScreen() instanceof SceneScreen)) {
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            if (wasPaused)
                world.pause();
        }
    }

    private void executeNewGame() {
        try {
            world.newGame();
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
        } catch (Exception e) {
            errorReporter.reportError("Remote new game failed: " + e.getMessage(), e);
        }
    }

    private void executeLoadGame(RemoteCommand command) {
        try {
            world.loadGameState(command.target);
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
        } catch (IOException e) {
            errorReporter.reportError("Remote load game failed: " + e.getMessage(), e);
        }
    }

    private void executeContinue() {
        try {
            world.load();
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
        } catch (Exception e) {
            errorReporter.reportError("Remote continue failed: " + e.getMessage(), e);
        }
    }

    private void togglePause() {
        if (world.isPaused())
            world.resume();
        else
            world.pause();
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
