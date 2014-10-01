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
import org.bladecoder.engine.ui.UI.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class InitScreen implements Screen {
	private final static String FILENAME = "ui/blade_logo.png";
	private final static float FADE_TIME = .6f;
	private final static float SCREEN_TIME = .8f;

	private Texture tex;	
	
	private UI ui;
	
	private float time;
	private float fadeTime;
	private float scale = 1f;
	
	private final Viewport viewport = new ScreenViewport();
	
	public InitScreen(UI ui) {
		this.ui = ui;
	}

	@Override
	public void render(float delta) {
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();	
		
		if(time > FADE_TIME * 2 + SCREEN_TIME) {  // EXIT INIT SCREEN
			batch.setColor(Color.WHITE);
			ui.setCurrentScreen(Screens.MENU_SCREEN);
		} else if(time > FADE_TIME + SCREEN_TIME) {  // FADE_OUT
			batch.setColor(1, 1, 1,  1 - fadeTime/FADE_TIME);			
		} else if(time < FADE_TIME) { // FADE IN
			batch.setColor(1, 1, 1,  fadeTime/FADE_TIME);	
		} else {
			fadeTime = 0;
		}		
		
		batch.draw(tex, (viewport.getScreenWidth() - tex.getWidth()* scale  ) /2, (viewport.getScreenHeight() - tex.getHeight()* scale)  /2,
				tex.getWidth() * scale, tex.getHeight() * scale);
		batch.setColor(1, 1, 1, 1);
	
		time += delta;
		fadeTime += delta;
		batch.end();
	}

	@Override
	public void resize(int width, int height) {	
		viewport.update(width, height, true);
		scale = width / (float)EngineAssetManager.getInstance().getResolution().portraitWidth;
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
		time = fadeTime = 0;
		Gdx.input.setInputProcessor(null);
		retrieveAssets();
		ui.getPointer().reset();
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
