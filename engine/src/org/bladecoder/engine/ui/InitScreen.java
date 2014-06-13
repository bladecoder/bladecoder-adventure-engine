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

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class InitScreen implements Screen {
	private final static String FILENAME = "ui/blade_logo.png";
	private final static float FADE_TIME = 1f;
	private final static float SCREEN_TIME = 1.5f;

	private Texture tex;	
	
	private UI ui;
	private boolean loadingGame = false;
	private boolean restart = false;
	
	private float time;
	private float fadeTime;
	
	private final Viewport viewport = new ScreenViewport();
	
	public InitScreen(UI ui, boolean restart) {
		this.ui = ui;
		this.restart = restart;
	}

	@Override
	public void render(float delta) {
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();	
		
		if(fadeTime < FADE_TIME && !loadingGame) { // FADE IN
			batch.setColor(1, 1, 1,  fadeTime/FADE_TIME);	
		} else	if(fadeTime < FADE_TIME && loadingGame) {  // FADE_OUT
			batch.setColor(1, 1, 1,  1 - fadeTime/FADE_TIME);
		} if(fadeTime >= FADE_TIME && loadingGame) {  // EXIT INIT SCREEN
			ui.setScreen(State.SCENE_SCREEN);
		} if(time > SCREEN_TIME && !loadingGame) { // LOAD GAME
			loadingGame = true;
			fadeTime = 0;
			
			try {
				if(restart) World.restart();
				else if(World.getInstance().isDisposed())
					World.getInstance().load();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);
				dispose();
				Gdx.app.exit();
			}
		}		
		
		batch.draw(tex, (viewport.getViewportWidth() - tex.getWidth()) /2, (viewport.getViewportHeight() - tex.getHeight()) /2);
		batch.setColor(1, 1, 1, 1);
	
		time += delta;
		fadeTime += delta;
		batch.end();
	}

	@Override
	public void resize(int width, int height) {	
		viewport.update(width, height, true);
	}

	public void retrieveAssets() {
		tex = new Texture(EngineAssetManager.getInstance().getResAsset(FILENAME));
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);	
	}

	@Override
	public void dispose() {
		tex.dispose();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
		retrieveAssets();
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
