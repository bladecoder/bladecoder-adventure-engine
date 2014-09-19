package org.bladecoder.engine.util;

import com.badlogic.gdx.Gdx;

public class DPIUtils {
	public final static float BASE_DPI = 160.0f;
	
	/**
	 * Current DPI
	 */
	public final static float DPI = BASE_DPI * Gdx.graphics.getDensity();

	/**
	 * The Google recommendations are 48 dp -> 9mm for touchable elements
	 */
	public final static float MIN_SIZE = 48 * Gdx.graphics.getDensity();

	/**
	 * The Google recommendations of space between UI objects is 8 dp
	 */
	public final static float UI_SPACE = 8 * Gdx.graphics.getDensity();

	/**
	 * The Google recommendations of space from bottom or top is 16 dp
	 */
	public final static float MARGIN_SIZE = 16 * Gdx.graphics.getDensity();

	/**
	 * The Google recommendations are 56 dp for action buttons
	 */
	public final static float BUTTON_SIZE = 56 * Gdx.graphics.getDensity();

	/**
	 * The screen height in DP
	 */
	public final static float SCREEN_HEIGHT_DP = Gdx.graphics.getHeight()
			/ Gdx.graphics.getDensity();

	public final static float NORMAL_MULTIPLIER = 1.0f; // 3-5"
	public final static float LARGE_MULTIPLIER = 1.3f; // 5-7"
	public final static float XLARGE_MULTIPLIER = 1.5f; // 8-10"
	public final static float XXLARGE_MULTIPLIER = 2f; // > 10"

	/**
	 * Calcs the button size based in screen size
	 *
	 * @return The recommended size in pixels
	 */
	public static float getPrefButtonSize(int screenWidth, int screenHeight) {
		return getSizeMultiplier(screenWidth, screenHeight) * BUTTON_SIZE;
	}

	/**
	 * Calcs the button size based in screen size
	 *
	 * @return The recommended size in pixels
	 */
	public static float getPrefButtonSize() {
		return getPrefButtonSize(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
	}
	
	/**
	 * Calcs the minimum size based in screen size
	 *
	 * @return The recommended size in pixels
	 */
	public static float getMinSize(int screenWidth, int screenHeight) {
		return getSizeMultiplier(screenWidth, screenHeight) * MIN_SIZE;
	}

	/**
	 * Calcs the minimum size based in screen size
	 *
	 * @return The recommended size in pixels
	 */
	public static float getMinSize() {
		return getMinSize(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
	}
	
	/**
	 * Calcs the margin size based in screen size
	 *
	 * @return The recommended size in pixels
	 */
	public static float getMarginSize(int screenWidth, int screenHeight) {
		return getSizeMultiplier(screenWidth, screenHeight) * MARGIN_SIZE;
	}
	
	public static float getMarginSize() {
		return getMarginSize(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
	}

	public static float getSizeMultiplier(int screenWidth, int screenHeight) {
		float inches = pixelsToInches(screenWidth);

		if (inches > 15)
			return XXLARGE_MULTIPLIER;

		if (inches > 8)
			return XLARGE_MULTIPLIER;

		if (inches > 6)
			return LARGE_MULTIPLIER;

		return NORMAL_MULTIPLIER;

	}
	
	public static float getSizeMultiplier() {
		return getSizeMultiplier(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
	}

	public static int dpToPixels(int dp) {
		return (int) (dp * Gdx.graphics.getDensity());
	}
	
	public static int pixelsToDP(int pixels) {
		return (int) (pixels / Gdx.graphics.getDensity());
	}
	
	public static float pixelsToInches(int pixels) {
		return (float)pixels / DPI;
	}
	
	public static float ptToPixels(float pts) {
		return pts * 72 / DPI;
	}
}
