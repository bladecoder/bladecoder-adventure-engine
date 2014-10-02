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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;

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
		Gdx.gl.glViewport(getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
		getCamera().viewportWidth = getScreenWidth();
		getCamera().viewportHeight = getScreenHeight();
		if (centerCamera)
			getCamera().position.set(getScreenWidth() / 2, getScreenHeight() / 2, 0);
		getCamera().update();
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
		
		if (out.x >= getScreenWidth())
			out.x = getScreenWidth() - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= getScreenHeight())
			out.y = getScreenHeight() - 1;
		else if (out.y < 0)
			out.y = 0;
		
		return out;
	}
	
	@Override
	public Vector3 unproject(Vector3 out) {
		super.unproject(out);
		
		if (out.x >= getScreenWidth())
			out.x = getScreenWidth() - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= getScreenHeight())
			out.y = getScreenHeight() - 1;
		else if (out.y < 0)
			out.y = 0;
		
		return out;
	}	
}
