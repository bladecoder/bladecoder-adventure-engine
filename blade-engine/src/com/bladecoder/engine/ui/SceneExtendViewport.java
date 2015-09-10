/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.util.EngineLogger;

/**
 * This is a Custom ExtendViewport:
 * 
 *   - The camera uses screen coordinates. This is used to draw fonts and UI 1:1
 *   - The world dimensions is used only to calculate the dimensions of the viewport
 *   - The dimensions of the viewport is calculated to extend the world between 4:3 and 16:9
 * 
 * @author rgarcia
 */
public class SceneExtendViewport extends Viewport {
	private float minWorldWidth, minWorldHeight;
	private float maxWorldWidth, maxWorldHeight;

	/** Creates a new viewport using a new {@link OrthographicCamera}. */
	public SceneExtendViewport() {
		setCamera(new OrthographicCamera());
	}

	@Override
	public void update(int screenWidth, int screenHeight, boolean centerCamera) {

		Vector2 s = Scaling.fill.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);

		if (s.x > screenWidth)
			setWorldWidth(minWorldWidth);
		else
			setWorldHeight(minWorldHeight);

		// Fit min size to the screen.
		// super.setWorldSize(minWorldWidth, minWorldHeight);

		Vector2 scaled = Scaling.fit.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);

		// Extend in the short direction.
		setScreenWidth(Math.round(scaled.x));
		setScreenHeight(Math.round(scaled.y));

		if (getScreenWidth() < screenWidth) {
			float toViewportSpace = getScreenHeight() / getWorldHeight();
			float toWorldSpace = getWorldHeight() / getScreenHeight();
			float lengthen = (screenWidth - getScreenWidth()) * toWorldSpace;
			if (maxWorldWidth > 0)
				lengthen = Math.min(lengthen, maxWorldWidth - minWorldWidth);
			setWorldWidth(getWorldWidth() + lengthen);
			setScreenWidth(getScreenWidth() + Math.round(lengthen * toViewportSpace));
		} else if (getScreenHeight() < screenHeight) {
			float toViewportSpace = getScreenWidth() / getWorldWidth();
			float toWorldSpace = getWorldWidth() / getScreenWidth();
			float lengthen = (screenHeight - getScreenHeight()) * toWorldSpace;
			if (maxWorldHeight > 0)
				lengthen = Math.min(lengthen, maxWorldHeight - minWorldHeight);
			setWorldHeight(getWorldHeight() + lengthen);
			setScreenHeight(getScreenHeight() + Math.round(lengthen * toViewportSpace));
		}

		// Center
		setScreenX((screenWidth - getScreenWidth()) / 2);
		setScreenY((screenHeight - getScreenHeight()) / 2);

		apply(centerCamera);

		EngineLogger.debug("SCREEN VIEWPORT: " + getScreenWidth() + "x" + getScreenHeight());
		EngineLogger.debug("SCREEN WORLD: " + getWorldWidth() + "x" + getWorldHeight());
	}

	@Override
	public void apply(boolean centerCamera) {
		Gdx.gl.glViewport(getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
		getCamera().viewportWidth = getScreenWidth();
		getCamera().viewportHeight = getScreenHeight();
		if (centerCamera)
			getCamera().position.set(getScreenWidth() / 2, getScreenHeight() / 2, 0);
		getCamera().update();
	}

	@Override
	public void setWorldSize(float worldWidth, float worldHeight) {
		super.setWorldSize(worldWidth, worldHeight);

		// The minimum height aspect is 4:3, the maximum is the world aspect
		minWorldWidth = Math.min(worldHeight * 4f / 3f, worldWidth);

		// The minimum width aspect is 16:9, the maximum is the world aspect
		minWorldHeight = Math.min(worldWidth * 9f / 16f, worldHeight);

		maxWorldWidth = worldWidth;
		maxWorldHeight = worldHeight;
	}

	@Override
	public Vector2 unproject(Vector2 out) {
		super.unproject(out);
		
		out.x = MathUtils.clamp(out.x, 0, getScreenWidth() - 1);
		out.y = MathUtils.clamp(out.y, 0, getScreenHeight() - 1);

		return out;
	}

	@Override
	public Vector3 unproject(Vector3 out) {
		super.unproject(out);

		out.x = MathUtils.clamp(out.x, 0, getScreenWidth() - 1);
		out.y = MathUtils.clamp(out.y, 0, getScreenHeight() - 1);

		return out;
	}
}
