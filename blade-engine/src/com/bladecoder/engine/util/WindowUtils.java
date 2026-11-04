package com.bladecoder.engine.util;

import com.badlogic.gdx.Gdx;

/** Converts desktop window sizes to the units expected by the backend. */
public final class WindowUtils {
  private static float windowScale = 1;

  private WindowUtils() {}

  /** Set by the desktop launcher; native logical-coordinate backends keep scale 1. */
  public static void setWindowScale(float scale) {
    if (scale <= 0 || Float.isNaN(scale) || Float.isInfinite(scale))
      throw new IllegalArgumentException("Invalid window scale: " + scale);
    windowScale = scale;
  }

  public static int toWindowUnits(int logicalSize) {
    return Math.round(logicalSize * windowScale);
  }

  public static boolean setWindowedMode(int width, int height) {
    return Gdx.graphics.setWindowedMode(toWindowUnits(width), toWindowUnits(height));
  }
}
