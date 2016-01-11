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

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;


/**
 * This is a Custom FitViewport with the next differences with the libgdx FitViewport
 * 
 *   - The camera uses screen coordinates. This is used to draw fonts and UI 1:1
 *   - The world dimensions is used only to calculate the dimensions of the viewport
 * 
 * @author rgarcia
 */
public class SceneFitViewport extends Viewport {

	/** Creates a new viewport using a new {@link OrthographicCamera}. */
	public SceneFitViewport () {
		setCamera(new OrthographicCamera());
	}
	
	@Override
	public void update (int screenWidth, int screenHeight, boolean centerCamera) {		
		Vector2 scaled = Scaling.fit.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);
		setScreenSize(Math.round(scaled.x), Math.round(scaled.y));
		// center the viewport in the middle of the screen
		setScreenPosition((screenWidth - getScreenWidth()) / 2,
				(screenHeight - getScreenHeight()) / 2);
		
		apply(centerCamera);
	}
	
	@Override
	public void apply (boolean centerCamera) {
		HdpiUtils.glViewport(getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
		getCamera().viewportWidth = getScreenWidth();
		getCamera().viewportHeight = getScreenHeight();
		if (centerCamera)
			getCamera().position.set(getScreenWidth() / 2, getScreenHeight() / 2, 0);
		getCamera().update();
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
