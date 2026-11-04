package com.bladecoder.engineeditor.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.util.DPIUtils;
import org.lwjgl.glfw.GLFW;

/** Keeps editor layout in desktop units when XWayland exposes scaled pixel coordinates. */
public final class EditorDpi {
  private static float scale = 1;

  private EditorDpi() {}

  public static ScreenViewport createViewport() {
    scale = 1;
    if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_X11) {
      float[] x = {1}, y = {1};
      GLFW.glfwGetWindowContentScale(
          ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), x, y);
      if (x[0] > 0 && Float.isFinite(x[0])) scale = x[0];
    }
    ScreenViewport viewport = new ScreenViewport();
    viewport.setUnitsPerPixel(1 / scale);
    return viewport;
  }

  /** BladeSkin already sizes fonts in backend units; convert once to layout units. */
  public static void scaleFonts(Skin skin) {
    for (BitmapFont font : skin.getAll(BitmapFont.class).values()) {
      font.getData().setScale(font.getData().scaleX / scale, font.getData().scaleY / scale);
    }
  }

  public static float getWidth() {
    return Gdx.graphics.getWidth() / scale;
  }

  public static float getHeight() {
    return Gdx.graphics.getHeight() / scale;
  }

  public static float getSpacing() {
    return DPIUtils.getSpacing() / scale;
  }

  public static float getMarginSize() {
    return DPIUtils.MARGIN_SIZE / scale;
  }
}
