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
package org.bladecoder.engine.ui;

import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SceneExtendViewport extends Viewport {
	private float minWorldWidth, minWorldHeight;
	private float maxWorldWidth, maxWorldHeight;

	/** Creates a new viewport using a new {@link OrthographicCamera}. */
	public SceneExtendViewport() {
		camera = new OrthographicCamera();
	}

	@Override
	public void update(int screenWidth, int screenHeight, boolean centerCamera) {
		// Fit min size to the screen.
		worldWidth = minWorldWidth;
		worldHeight = minWorldHeight;
		Vector2 scaled = Scaling.fit.apply(worldWidth, worldHeight,
				screenWidth, screenHeight);

		// Extend in the short direction.
		viewportWidth = Math.round(scaled.x);
		viewportHeight = Math.round(scaled.y);
		if (viewportWidth < screenWidth) {
			float toViewportSpace = viewportHeight / worldHeight;
			float toWorldSpace = worldHeight / viewportHeight;
			float lengthen = (screenWidth - viewportWidth) * toWorldSpace;
			if (maxWorldWidth > 0)
				lengthen = Math.min(lengthen, maxWorldWidth - minWorldWidth);
			worldWidth += lengthen;
			viewportWidth += Math.round(lengthen * toViewportSpace);
		} else if (viewportHeight < screenHeight) {
			float toViewportSpace = viewportWidth / worldWidth;
			float toWorldSpace = worldWidth / viewportWidth;
			float lengthen = (screenHeight - viewportHeight) * toWorldSpace;
			if (maxWorldHeight > 0)
				lengthen = Math.min(lengthen, maxWorldHeight - minWorldHeight);
			worldHeight += lengthen;
			viewportHeight += Math.round(lengthen * toViewportSpace);
		}

		// Center
		viewportX = (screenWidth - viewportWidth) / 2;
		viewportY = (screenHeight - viewportHeight) / 2;

		Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
		camera.viewportWidth = viewportWidth;
		camera.viewportHeight = viewportHeight;
		if (centerCamera)
			camera.position.set(viewportWidth / 2, viewportHeight / 2, 0);
		camera.update();

		EngineLogger.debug("VIEWPORT: " + viewportWidth + "x" + viewportHeight);
	}

	@Override
	public void setWorldSize(float worldWidth, float worldHeight) {
		super.setWorldSize(worldWidth, worldHeight);

		// The minimum aspect is 16:9, the maximum is the world aspect
		minWorldWidth = worldWidth;
		minWorldHeight = Math.min(worldWidth * 9f / 16f, worldHeight);

		maxWorldWidth = worldWidth;
		maxWorldHeight = worldHeight;
	}

	public void getInputUnProject(Vector2 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY());

		unproject(out);
	}

	public void getInputUnProject(Vector3 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY(), 0);

		unproject(out);
	}

	@Override
	public Vector2 unproject(Vector2 out) {
		super.unproject(out);

		if (out.x >= viewportWidth)
			out.x = viewportWidth - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= viewportHeight)
			out.y = viewportHeight - 1;
		else if (out.y < 0)
			out.y = 0;

		return out;
	}

	@Override
	public Vector3 unproject(Vector3 out) {
		super.unproject(out);

		if (out.x >= viewportWidth)
			out.x = viewportWidth - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= viewportHeight)
			out.y = viewportHeight - 1;
		else if (out.y < 0)
			out.y = 0;

		return out;
	}
}
