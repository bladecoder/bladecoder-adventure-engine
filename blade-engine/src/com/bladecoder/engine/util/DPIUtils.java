package com.bladecoder.engine.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class DPIUtils {
    public final static float BASE_DPI = 160.0f;

    /**
     * Current DPI
     */
    public final static float DPI = BASE_DPI * getLogicalDensity();

    /**
     * The Google recommendations are 48 dp -> 9mm for touchable elements
     */
    public final static float TOUCH_MIN_SIZE = 48 * getLogicalDensity();

    /**
     * The Google recommendations of space between UI objects is 8 dp
     */
    public final static float UI_SPACE = 8 * getLogicalDensity();

    /**
     * The Google recommendations of space from bottom or top is 16 dp
     */
    public final static float MARGIN_SIZE = 16 * getLogicalDensity();

    /**
     * The Google recommendations are 56 dp for action buttons
     */
    public final static float BUTTON_SIZE = 56 * getLogicalDensity();

    /**
     * The Google recommendations are 24 dp for icons inside action buttons
     */
    public final static float ICON_SIZE = 24 * getLogicalDensity();

    /**
     * The Google recommendations are 8 dp for space between ui elements
     */
    public final static float SPACING = 8 * getLogicalDensity();

    /**
     * The screen height in DP
     */
    public final static float SCREEN_HEIGHT_DP = Gdx.graphics.getHeight() / getLogicalDensity();

    public final static float NORMAL_MULTIPLIER = 1.0f; // 3-5"
    public final static float LARGE_MULTIPLIER = 1.5f; // 5-7"
    public final static float XLARGE_MULTIPLIER = 2f; // 8-10"
    public final static float XXLARGE_MULTIPLIER = 2.5f; // > 10"

    public static float getLogicalDensity() {
        return Gdx.graphics.getDensity() / Gdx.graphics.getBackBufferScale();
    }

    /**
     * Calcs the button size based in screen size
     *
     * @return The recommended size in pixels
     */
    public static float getPrefButtonSize() {
        return getSizeMultiplier() * BUTTON_SIZE;
    }

    /**
     * Calcs the minimum size based in screen size
     *
     * @return The recommended size in pixels
     */
    public static float getTouchMinSize() {
        return getSizeMultiplier() * TOUCH_MIN_SIZE;
    }

    /**
     * Calcs the margin size based in screen size
     *
     * @return The recommended size in pixels
     */
    public static float getMarginSize() {
        return getSizeMultiplier() * MARGIN_SIZE;
    }

    /**
     * Calcs the space between ui elements based in screen size
     *
     * @return The recommended size in pixels
     */
    public static float getSpacing() {
        return getSizeMultiplier() * SPACING;
    }

//	public static float getSizeMultiplier() {
//		float inches = pixelsToInches(Gdx.graphics.getWidth());
//
//		if (inches > 15)
//			return XXLARGE_MULTIPLIER;
//
//		if (inches > 9)
//			return XLARGE_MULTIPLIER;
//
//		if (inches > 6)
//			return LARGE_MULTIPLIER;
//
//		return NORMAL_MULTIPLIER;
//
//	}

    public static float getSizeMultiplier() {
        // FIX: In Wayland, the Gdx.graphics.getWidth() does not return the correct value in the first call.
        int width = Gdx.graphics.isFullscreen() && Gdx.app.getType() == Application.ApplicationType.Desktop ? Gdx.graphics.getDisplayMode().width : Gdx.graphics.getWidth();
        float inches = pixelsToInches(width);
        float s = inches / 6f;

        return Math.max(1.0f, s);

    }

    public static int dpToPixels(int dp) {
        return (int) (dp * getLogicalDensity());
    }

    public static int pixelsToDP(int pixels) {
        return (int) (pixels / getLogicalDensity());
    }

    public static float pixelsToInches(int pixels) {
        return pixels / DPI;
    }

    public static float ptToPixels(float pts) {
        return pts * 72 / DPI;
    }
}
