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

import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.ui.UI.Screens;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LoadingScreen implements Screen {
	private final static float INIT_TIME = 3f;
	
	private int pos = 0;
	private int numSquares = 3;
	
	private float x, y;
	
	private float squareWidth = 30f;
	private float squareHeight = 30f;
	private float margin = 10f;
	
	private float initTime = 0;
	
	private float delta = 0;
	
	private UI ui;
	
	private final Viewport viewport = new ScreenViewport();
	
	public LoadingScreen(UI ui) {	
		this.ui = ui;
	}

	@Override
	public void render(float delta) {
		if(!World.getInstance().isDisposed()) {
			World.getInstance().update(delta);
		}

		AssetState assetState = World.getInstance().getAssetState();

		if (assetState == AssetState.LOADED) {
			ui.setCurrentScreen(Screens.SCENE_SCREEN);
			return;
		}		
		
		// Only show the screen when time > INIT_TIME
		if(initTime < INIT_TIME) {
			initTime += delta;
			return;
		}			
		
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();			
				
		update(delta);
		
		for(int i=0; i < numSquares; i++) {

			if(i == pos)
				RectangleRenderer.draw(ui.getBatch(), x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.WHITE);
			else
				RectangleRenderer.draw(ui.getBatch(), x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.GRAY);
		}
		batch.end();
	}
	
	private void update(float d) {
		delta += d;
		
		if(delta > 0.4) {
			pos = (pos + 1) % numSquares;
			delta = 0;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
		
		x = (viewport.getWorldWidth() - (squareWidth * numSquares + margin * (numSquares -1))) / 2; 
		y = (viewport.getWorldHeight() - squareHeight) / 2;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
		initTime = 0;
		delta = 0;
	}

	@Override
	public void hide() {
		
	}

	@Override
	public void pause() {
		
	}

	@Override
	public void resume() {
		
	}
	
}
