package com.bladecoder.engine.remote;

/** Process-selected capability boundary. HTTP clients cannot change it. */
public enum RemoteControlMode {
  PLAYER,
  DEBUG,
  EDIT;

  public static RemoteControlMode parse(String value) {
    if ("player".equals(value)) return PLAYER;
    if ("debug".equals(value)) return DEBUG;
    if ("edit".equals(value)) return EDIT;
    throw new IllegalArgumentException("HTTP mode must be player, debug or edit");
  }
}
