package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.WorldEventListener;

/** Bounded, non-destructive event history exposed to a remote player. */
final class RemoteEventLog implements WorldEventListener {
    private static final int MAX_EVENTS = 1000;

    private final List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> previousEvents;
    private boolean truncated;

    synchronized void beginAction() {
        previousEvents = new ArrayList<Map<String, Object>>(events);
        events.clear();
        truncated = false;
    }

    synchronized void commitAction() {
        previousEvents = null;
    }

    synchronized void rollbackAction() {
        if (previousEvents != null) {
            events.clear();
            events.addAll(previousEvents);
            previousEvents = null;
        }
    }

    synchronized Map<String, Object> getEventsState() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("events", new ArrayList<Map<String, Object>>(events));
        result.put("truncated", truncated);
        return result;
    }

    synchronized void recordInventoryChanged() {
        add("inventoryChanged");
    }

    @Override
    public synchronized void text(Text text) {
        if (text == null)
            return;
        Map<String, Object> event = add("text");
        event.put("text", text.str);
        event.put("textType", text.type == null ? null : text.type.name());
        if (text.actorId != null)
            event.put("actorId", text.actorId);
    }

    @Override
    public synchronized void sceneChanged(String sceneId) {
        Map<String, Object> event = add("sceneChanged");
        event.put("scene", sceneId);
    }

    @Override
    public synchronized void cutMode(boolean value) {
        Map<String, Object> event = add("cutModeChanged");
        event.put("cutMode", value);
    }

    @Override
    public synchronized void dialogOptionsChanged() {
        add("dialogOptionsChanged");
    }

    @Override
    public synchronized void inventoryChanged() {
        recordInventoryChanged();
    }

    @Override
    public synchronized void pause(boolean value) {
        Map<String, Object> event = add("pauseChanged");
        event.put("paused", value);
    }

    @Override
    public synchronized void animationStarted(String actorId, String animationId, boolean finite) {
        if (!finite)
            return;
        Map<String, Object> event = add("animationStarted");
        event.put("actorId", actorId);
        event.put("animation", animationId);
    }

    @Override
    public synchronized void gameEnded() {
        add("gameEnded");
    }

    private Map<String, Object> add(String type) {
        if (events.size() == MAX_EVENTS) {
            events.remove(0);
            truncated = true;
        }
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", type);
        events.add(event);
        return event;
    }
}
