package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Gdx;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.i18n.I18N;
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
        state.put("scene", sceneState);

        if (scene.getPlayer() != null) {
            Map<String, Object> player = new LinkedHashMap<String, Object>();
            player.put("id", scene.getPlayer().getId());
            state.put("player", player);
        }

        List<Map<String, Object>> actors = new ArrayList<Map<String, Object>>();
        for (BaseActor actor : scene.getActors().values()) {
            if (actor instanceof InteractiveActor) {
                InteractiveActor interactiveActor = (InteractiveActor) actor;
                if (!interactiveActor.canInteract())
                    continue;
                Map<String, Object> actorState = new LinkedHashMap<String, Object>();
                actorState.put("id", interactiveActor.getId());
                actorState.put("description", getTranslatedDescription(interactiveActor));
                actorState.put("verbs", RemoteActorVerbResolver.getAvailableVerbs(interactiveActor, false));
                actors.add(actorState);
            }
        }
        state.put("actors", actors);
        state.put("inventory", getInventoryState());
        state.put("dialogOptions", getTranslatedDialogOptions());
        return state;
    }

    private List<Map<String, Object>> getInventoryState() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        Inventory inventory = world.getInventory();
        if (inventory == null)
            return items;

        for (int i = 0; i < inventory.getNumItems(); i++) {
            SpriteActor item = inventory.get(i);
            Map<String, Object> itemState = new LinkedHashMap<String, Object>();
            itemState.put("id", item.getId());
            itemState.put("description", getTranslatedDescription(item));
            itemState.put("verbs", RemoteActorVerbResolver.getAvailableVerbs(item, true));
            items.add(itemState);
        }
        return items;
    }

    private String getTranslatedDescription(InteractiveActor actor) {
        return getTranslatedText(actor.getDesc());
    }

    private List<String> getTranslatedDialogOptions() {
        List<String> options = new ArrayList<String>();
        if (world.hasDialogOptions()) {
            for (String option : world.getDialogOptions())
                options.add(getTranslatedText(option));
        }
        return options;
    }

    private String getTranslatedText(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == I18N.PREFIX)
            return world.getI18N().getString(text.substring(1));
        return text;
    }
}
