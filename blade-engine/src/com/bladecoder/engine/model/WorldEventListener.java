/*******************************************************************************
 * Copyright 2026 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *******************************************************************************/
package com.bladecoder.engine.model;

/** Optional observer for game events that are visible to a player. */
public interface WorldEventListener {
    default void text(Text text) {
    }

    default void sceneChanged(String sceneId) {
    }

    default void cutMode(boolean value) {
    }

    default void dialogOptionsChanged() {
    }

    default void inventoryChanged() {
    }

    default void pause(boolean value) {
    }

    default void animationStarted(String actorId, String animationId, boolean finite) {
    }
}
