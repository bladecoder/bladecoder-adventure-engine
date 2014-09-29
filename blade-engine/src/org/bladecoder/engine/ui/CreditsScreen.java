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

import java.io.BufferedReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.TextUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class CreditsScreen implements Screen, InputProcessor {

	private final static String CREDITS_FILENAME = "ui/credits";
	private static final String FONT_TITLE_STYLE = "credits-title";
	private static final String FONT_STYLE = "credits";
	private static final float SPEED = 40; // px/sec.

	// title and texts pair sequence
	private List<String> credits = new ArrayList<String>();

	private BitmapFont creditsFont;
	private BitmapFont titlesFont;

	private int stringHead = 0;
	private float scrollY = 0; 

	private final UI ui;

	private Music music;
	
	private final HashMap<String, Texture> images = new HashMap<String, Texture>();
	
	private Viewport viewport;

	public CreditsScreen(UI ui) {
		this.ui = ui;
	}

	@Override
	public void render(float delta) {		
		SpriteBatch batch = ui.getBatch();
		int width = (int)viewport.getWorldWidth();
		int height = (int)viewport.getWorldHeight();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();	
		
		scrollY+= delta * SPEED * EngineAssetManager.getInstance().getScale();
		
		float y = scrollY;

		if (stringHead >= credits.size())
			ui.setScreen(State.MENU_SCREEN);

		for (int i = stringHead; i < credits.size(); i++) {
			String s = credits.get(i);
			
			char type = 'c'; // types are 'c' -> credit, 't' -> title, 'i' -> image, 's' -> space, 'm' -> music
			
			if(s.indexOf('#') != -1 ) {
				type = s.charAt(0);
				s = s.substring(2);
			}
			
			if(type == 't') {				
				y -= titlesFont.getLineHeight() * 2;
				
				TextUtils.drawCenteredScreenX(batch, titlesFont, s, y, width);
				y -= titlesFont.getLineHeight();
				
				if (y > height + titlesFont.getLineHeight()) {
					stringHead = i + 1;
					scrollY -= titlesFont.getLineHeight();
					scrollY -= titlesFont.getLineHeight() * 2;
				}
			} else if(type == 'i') {
				Texture img = images.get(s);
				batch.draw(img, (width - img.getWidth()) / 2, y -  img.getHeight());
				
				y -= img.getHeight();
				
				if (y > height) {
					stringHead = i + 1;
					scrollY -=  img.getHeight();
				}
			} else if(type == 's') {
				int space =(int) (Integer.valueOf(s) * EngineAssetManager.getInstance().getScale());
				y -= space;
				
				if (y -  space > height) {
					stringHead = i + 1;
					scrollY -= space;
				}
			} else if(type == 'm') {
				if(music != null)
					music.dispose();
				
				music = Gdx.audio.newMusic(EngineAssetManager.getInstance().getAsset("music/" + s));
				music.play();
			} else {
				TextUtils.drawCenteredScreenX(batch, creditsFont, s, y, width);
				y -= creditsFont.getLineHeight();
				
				if (y > height + creditsFont.getLineHeight()) {
					stringHead = i + 1;
					scrollY -= creditsFont.getLineHeight();
				}
			}
			
			if(y < 0) {
				break;
			}
		}

		ui.getPointer().draw(batch, viewport);
		
		batch.end();
	}

	@Override
	public void resize(int width, int height) {		
		viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		
		for(Texture t:images.values())
			t.dispose();
		
		images.clear();
		credits.clear();
		
		if(music != null) {
			music.stop();
			music.dispose();
		}
			
	}

	private void retrieveAssets(TextureAtlas atlas) {
		titlesFont = ui.getSkin().getFont(FONT_TITLE_STYLE);
		creditsFont = ui.getSkin().getFont(FONT_STYLE);

		Locale locale = Locale.getDefault();

		String localeFilename = MessageFormat.format("{0}_{1}.txt", CREDITS_FILENAME,
				locale.getLanguage());

		if (!EngineAssetManager.getInstance().assetExists(localeFilename))
			localeFilename = MessageFormat.format("{0}.txt", CREDITS_FILENAME);

		BufferedReader reader = EngineAssetManager.getInstance().getAsset(localeFilename)
				.reader(4096, "utf-8");

		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				credits.add(line);
			}
		} catch (Exception e) {
			EngineLogger.error(e.getMessage());
			
			ui.setScreen(State.MENU_SCREEN);
		}
		
		scrollY += titlesFont.getLineHeight();
		
		// Load IMAGES
		for (int i = 0; i < credits.size(); i++) {
			String s = credits.get(i);
			
			
			if(s.indexOf('#') != -1 && s.charAt(0) == 'i') {
				s = s.substring(2);
				
				Texture tex = new Texture(EngineAssetManager.getInstance().getResAsset("ui/" + s));
				tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
				
				images.put(s, tex);
			}
		}
	}

	@Override
	public void show() {
		retrieveAssets(ui.getUIAtlas());
		Gdx.input.setInputProcessor(this);
		
//		int wWidth = EngineAssetManager.getInstance().getResolution().portraitWidth;
//		int wHeight = EngineAssetManager.getInstance().getResolution().portraitHeight;
		
//		viewport = new ExtendViewport(wWidth, wHeight);
		viewport = new ScreenViewport();
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
		if (keycode == Input.Keys.ESCAPE
				|| keycode == Input.Keys.BACK)
			ui.setScreen(State.MENU_SCREEN);		
		return false;
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
		ui.setScreen(State.MENU_SCREEN);
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
