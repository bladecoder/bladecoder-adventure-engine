package com.bladecoder.engine.remote;

/** Process-selected capability boundary. HTTP clients cannot change it. */
public enum RemoteControlMode {
    PLAYER, DEBUG;

    public static RemoteControlMode parse(String value) {
        if ("player".equals(value)) return PLAYER;
        if ("debug".equals(value)) return DEBUG;
        throw new IllegalArgumentException("HTTP mode must be player or debug");
    }
}
