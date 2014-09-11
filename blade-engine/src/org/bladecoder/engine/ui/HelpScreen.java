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

import java.text.MessageFormat;
import java.util.Locale;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.ui.UI.State;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HelpScreen implements Screen, InputProcessor {
	
	private final static String PIE_FILENAME = "ui/helpPie";
	private final static String DESKTOP_FILENAME = "ui/helpDesktop";

	private Texture tex;

	private final UI ui;
	
	private String localeFilename;
	private final Viewport viewport = new ScreenViewport();

	public HelpScreen(UI ui) {
		this.ui = ui;
		
		Locale locale = Locale.getDefault();
		
		String filename = null;
		
		if(ui.isPieMode())
			filename = PIE_FILENAME;
		else
			filename = DESKTOP_FILENAME;
		
		localeFilename = MessageFormat.format("{0}_{1}.png", filename, locale.getLanguage());
		
		if(!EngineAssetManager.getInstance().assetExists(localeFilename))
			localeFilename = MessageFormat.format("{0}.png", filename);
	}

	@Override
	public void render(float delta) {
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();	
		batch.draw(tex, 0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		ui.getPointer().draw(batch, viewport);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		tex.dispose();
	}

	@Override
	public void show() {
		tex = new Texture(EngineAssetManager.getInstance().getResAsset(localeFilename));
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);	
		
		Gdx.input.setInputProcessor(this);
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
	
	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		switch (keycode) {
		case Input.Keys.ESCAPE:
		case Input.Keys.BACK:
			ui.setScreen(State.SCENE_SCREEN);
			break;
		}

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		ui.setScreen(State.SCENE_SCREEN);
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
