package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Gdx;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI;

/** Builds remote state responses, marshalling game-state reads to the render thread. */
final class RemoteGameStateProvider {
    private static final long STATE_TIMEOUT_MS = 2000;

    private final UI ui;
    private final World world;
    private final RemoteServerStatus serverStatus;

    RemoteGameStateProvider(UI ui, World world, RemoteServerStatus serverStatus) {
        this.ui = ui;
        this.world = world;
        this.serverStatus = serverStatus;
    }

    Map<String, Object> getHealthState() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("status", serverStatus.isRunning() ? "running" : "stopped");
        state.put("port", serverStatus.getConfiguredPort());
        state.put("queueLength", serverStatus.getQueueLength());
        if (serverStatus.getLastError() != null)
            state.put("lastError", serverStatus.getLastError());
        return state;
    }

    String getStateJson() {
        if (Gdx.app == null)
            return null;

        final String[] result = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = RemoteJson.toJson(getGameState());
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            return latch.await(STATE_TIMEOUT_MS, TimeUnit.MILLISECONDS) ? result[0] : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private Map<String, Object> getGameState() {
        Map<String, Object> state = getHealthState();
        state.put("paused", world.isPaused());
        state.put("cutMode", world.inCutMode());
        state.put("assetState", world.getAssetState() == null ? null : world.getAssetState().name());
        state.put("recorderPlaying", ui.getRecorder().isPlaying());
        state.put("testerBotEnabled", ui.getTesterBot().isEnabled());

        Scene scene = world.getCurrentScene();
        if (scene == null)
            return state;

        Map<String, Object> sceneState = new LinkedHashMap<String, Object>();
        sceneState.put("id", scene.getId());
        sceneState.put("state", scene.getState());
        state.put("scene", sceneState);

        if (scene.getPlayer() != null) {
            Map<String, Object> player = new LinkedHashMap<String, Object>();
            player.put("id", scene.getPlayer().getId());
            player.put("x", scene.getPlayer().getX());
            player.put("y", scene.getPlayer().getY());
            state.put("player", player);
        }

        List<Map<String, Object>> actors = new ArrayList<Map<String, Object>>();
        for (BaseActor actor : scene.getActors().values()) {
            if (actor instanceof InteractiveActor) {
                InteractiveActor interactiveActor = (InteractiveActor) actor;
                Map<String, Object> actorState = new LinkedHashMap<String, Object>();
                actorState.put("id", interactiveActor.getId());
                actorState.put("description", interactiveActor.getDesc());
                actorState.put("state", interactiveActor.getState());
                actorState.put("visible", interactiveActor.isVisible());
                actorState.put("canInteract", interactiveActor.canInteract());
                actorState.put("x", interactiveActor.getX());
                actorState.put("y", interactiveActor.getY());
                actorState.put("verbs", new ArrayList<String>(interactiveActor.getVerbManager().getVerbs().keySet()));
                actors.add(actorState);
            }
        }
        state.put("actors", actors);
        state.put("dialogOptions", world.hasDialogOptions() ? new ArrayList<String>(world.getDialogOptions())
                : new ArrayList<String>());
        return state;
    }
}
