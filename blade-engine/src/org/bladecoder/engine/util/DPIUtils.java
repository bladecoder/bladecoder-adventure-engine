package org.bladecoder.engine.util;

import com.badlogic.gdx.Gdx;

public class DPIUtils {
	/**
	 * Current DPI
	 */
	public final static float DPI = 160.0f * Gdx.graphics.getDensity();
	
	/**
	 * The Google recommendations are 48 dp -> 9mm for touchable elements
	 */
	public final static float MIN_SIZE = 60 * Gdx.graphics.getDensity();
	
	/**
	 * The screen height in DP
	 */
	public final static float SCREEN_HEIGHT_DP = Gdx.graphics.getHeight() / Gdx.graphics.getDensity();
	
	/**
	 * Calcs the minimum size for a button based in DPI.
	 *
	 * @return The recommended size in pixels
	 */
	public static int getButtonPrefSize() {
		return (int)(Math.max(MIN_SIZE, Gdx.graphics.getHeight()/10));
	}
	
	public static int toPixels(int dp) {
		return (int)(dp * Gdx.graphics.getDensity());
	}
}
