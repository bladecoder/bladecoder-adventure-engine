/*******************************************************************************
 * Copyright 2026 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *******************************************************************************/
package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.List;

/** Dispatches player-visible events for one {@link World}. */
public class WorldEventDispatcher {
    private final List<WorldEventListener> listeners = new ArrayList<WorldEventListener>();

    public void addListener(WorldEventListener listener) {
        if (listener != null && !listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(WorldEventListener listener) {
        listeners.remove(listener);
    }

    public void text(Text text) {
        for (WorldEventListener listener : snapshot())
            listener.text(text);
    }

    public void sceneChanged(String sceneId) {
        for (WorldEventListener listener : snapshot())
            listener.sceneChanged(sceneId);
    }

    public void cutMode(boolean value) {
        for (WorldEventListener listener : snapshot())
            listener.cutMode(value);
    }

    public void dialogOptionsChanged() {
        for (WorldEventListener listener : snapshot())
            listener.dialogOptionsChanged();
    }

    public void inventoryChanged() {
        for (WorldEventListener listener : snapshot())
            listener.inventoryChanged();
    }

    public void pause(boolean value) {
        for (WorldEventListener listener : snapshot())
            listener.pause(value);
    }

    public void animationStarted(String actorId, String animationId, boolean finite) {
        for (WorldEventListener listener : snapshot())
            listener.animationStarted(actorId, animationId, finite);
    }

    public void gameEnded() {
        for (WorldEventListener listener : snapshot())
            listener.gameEnded();
    }

    private List<WorldEventListener> snapshot() {
        return new ArrayList<WorldEventListener>(listeners);
    }
}
