package com.bladecoder.engine.remote;


import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI;

/** Executes remote commands on the LibGDX render thread. */
final class RemoteCommandExecutor {
    private static final int SCREENSHOT_WIDTH = 1920;

    private final UI ui;
    private final World world;
    private final RemoteErrorReporter errorReporter;
    private final RemoteEventLog eventLog;

    RemoteCommandExecutor(UI ui, World world, RemoteErrorReporter errorReporter, RemoteEventLog eventLog) {
        this.ui = ui;
        this.world = world;
        this.errorReporter = errorReporter;
        this.eventLog = eventLog;
    }

    RemoteCommandResult execute(RemoteCommand command) {
        String preconditionError = validatePreconditions(command);
        if (preconditionError != null)
            return reject(preconditionError);

        if (world.isPaused() && command.type != RemoteCommand.Type.PAUSE)
            return reject("Remote command cannot run while the game is paused. Use the pause command to resume it first.");

        showSceneScreen();

        boolean resetEvents = resetsEvents(command);
        if (resetEvents)
            eventLog.beginAction();

        try {
            executeCommand(command);
            if (resetEvents)
                eventLog.commitAction();
            if (resetEvents)
                eventLog.recordInventoryChanged();
            return RemoteCommandResult.success();
        } catch (CommandRejectedException e) {
            if (resetEvents)
                eventLog.rollbackAction();
            return reject(e.getMessage());
        } catch (Exception e) {
            if (resetEvents)
                eventLog.rollbackAction();
            String message = "Remote command failed: " + e.getMessage();
            errorReporter.reportError(message, e);
            return RemoteCommandResult.failed(message);
        }
    }

    private String validatePreconditions(RemoteCommand command) {
        if (world.isDisposed() && !canRunWhenDisposed(command))
            return "Remote command requires an active game. Start a new game, load a saved game, or continue first.";
        return null;
    }

    private void executeCommand(RemoteCommand command) throws Exception {
        switch (command.type) {
        case NEW_GAME:
            world.newGame();
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            return;
        case LOAD_GAME:
            world.loadGameState(command.target);
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            return;
        case CONTINUE:
            world.load();
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            return;
        case PAUSE:
            if (world.isPaused())
                world.resume();
            else
                world.pause();
            return;
        default:
            break;
        }

        Scene scene = world.getCurrentScene();
        if (scene == null)
            throw new CommandRejectedException("No current scene available for remote command");

        switch (command.type) {
        case ACTOR_VERB:
            executeActorVerb(scene, command);
            return;
        case SCENE_VERB:
            scene.runVerb(command.verb);
            return;
        case DIALOG_OPTION:
            if (!world.hasDialogOptions() || command.option < 0 || command.option >= world.getDialogOptions().size())
                throw new CommandRejectedException("Remote command dialog option is not available: " + command.option);
            world.selectDialogOption(command.option);
            return;
        case GOTO:
            if (scene.getPlayer() == null)
                throw new CommandRejectedException("Remote goto command requires a scene player");
            scene.getPlayer().goTo(new Vector2(command.x, command.y), null, false);
            return;
        case SAVE_GAME:
            world.getSerializer().saveGameState(command.target, true);
            return;
        case SCREENSHOT:
            world.takeScreenshot(command.target, SCREENSHOT_WIDTH);
            return;
        default:
            throw new CommandRejectedException("Unknown remote command");
        }
    }

    private void executeActorVerb(Scene scene, RemoteCommand command) throws CommandRejectedException {
        BaseActor baseActor = scene.getActor(command.actorId, true);
        if (!(baseActor instanceof InteractiveActor))
            throw new CommandRejectedException("Remote command actor not found: " + command.actorId);
        InteractiveActor actor = (InteractiveActor) baseActor;
        if (!actor.canInteract())
            throw new CommandRejectedException("Remote command actor cannot be interacted with: " + command.actorId);
        if (Verb.USE_VERB.equals(command.verb)) {
            executeUseVerb(scene, command, actor);
            return;
        }

        if (!RemoteActorVerbResolver.canRun(actor, isInventoryItem(actor), command.verb)) {
            throw new CommandRejectedException("Remote command verb is not available: " + command.verb);
        }

        actor.runVerb(command.verb, command.target);
    }

    private void executeUseVerb(Scene scene, RemoteCommand command, InteractiveActor sourceActor)
            throws CommandRejectedException {
        if (command.target == null)
            throw new CommandRejectedException("Remote use command requires a target actor");

        BaseActor targetBaseActor = scene.getActor(command.target, true);
        if (!(targetBaseActor instanceof InteractiveActor))
            throw new CommandRejectedException("Remote command target actor not found: " + command.target);
        InteractiveActor targetActor = (InteractiveActor) targetBaseActor;

        if (sourceActor == targetActor || (!isInventoryItem(sourceActor) && !isInventoryItem(targetActor))) {
            throw new CommandRejectedException(
                    "Remote use command requires an inventory item and cannot be used between two scene actors");
        }

        runUseVerb(sourceActor, targetActor);
    }

    /** Matches the inventory UI selection algorithm for use verbs. */
    private void runUseVerb(InteractiveActor sourceActor, InteractiveActor targetActor) {
        Verb targetVerb = targetActor.getVerb(Verb.USE_VERB, sourceActor.getId());
        Verb sourceVerb = sourceActor.getVerb(Verb.USE_VERB, targetActor.getId());
        Verb bestMatch = sourceVerb;

        if (bestMatch == null) {
            bestMatch = targetVerb;
        } else if (targetVerb != null && sourceVerb != null && targetActor.getId().equals(sourceVerb.getTarget())
                && !sourceActor.getId().equals(targetVerb.getTarget())) {
            bestMatch = targetVerb;
        }

        if (bestMatch == sourceVerb)
            sourceActor.runVerb(Verb.USE_VERB, targetActor.getId());
        else
            targetActor.runVerb(Verb.USE_VERB, sourceActor.getId());
    }

    private boolean isInventoryItem(InteractiveActor actor) {
        return world.getInventory() != null && world.getInventory().get(actor.getId()) == actor;
    }

    private boolean canRunWhenDisposed(RemoteCommand command) {
        return command.type == RemoteCommand.Type.NEW_GAME || command.type == RemoteCommand.Type.LOAD_GAME
                || command.type == RemoteCommand.Type.CONTINUE;
    }

    private boolean resetsEvents(RemoteCommand command) {
        return command.type != RemoteCommand.Type.PAUSE && command.type != RemoteCommand.Type.CONTINUE
                && command.type != RemoteCommand.Type.SCREENSHOT && command.type != RemoteCommand.Type.SAVE_GAME;
    }

    private void showSceneScreen() {
        boolean wasPaused = world.isPaused();
        if (!world.isDisposed() && !(ui.getCurrentScreen() instanceof SceneScreen)) {
            ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            if (wasPaused)
                world.pause();
        }
    }

    private RemoteCommandResult reject(String message) {
        errorReporter.reportError(message);
        return RemoteCommandResult.rejected(message);
    }

    private static class CommandRejectedException extends Exception {
        private static final long serialVersionUID = 1L;

        CommandRejectedException(String message) {
            super(message);
        }
    }
}
