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

import java.io.BufferedReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class CreditsScreen extends ScreenAdapter implements BladeScreen {
	private static final String CREDITS_FILENAME = "ui/credits";
	private static final float SPEED = 10 * DPIUtils.getSpacing(); // px/sec.

	// title and texts pair sequence
	private final List<String> credits = new ArrayList<>();

	private CreditScreenStyle style;

	private int stringHead = 0;
	private float scrollY = 0;

	private UI ui;

	private Music music;

	private final Map<String, Texture> images = new HashMap<>();

	private Viewport viewport;

	private final GlyphLayout layout = new GlyphLayout();

	private final InputProcessor inputProcessor = new InputAdapter() {
		@Override
		public boolean keyUp(int keycode) {
			if (keycode == Input.Keys.ESCAPE
					|| keycode == Input.Keys.BACK)
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			// FIXME: This should probably return true when we process ESCAPE or BACK?
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			ui.setCurrentScreen(Screens.MENU_SCREEN);
			return true;
		}
	};

	@Override
	public void render(float delta) {
		final SpriteBatch batch = ui.getBatch();
		final int width = (int) viewport.getWorldWidth();
		final int height = (int) viewport.getWorldHeight();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		if (style.background != null) {
			style.background.draw(batch, 0, 0, width, height);
		}

		scrollY += delta * SPEED * EngineAssetManager.getInstance().getScale();

		float y = scrollY;

		if (stringHead >= credits.size())
			ui.setCurrentScreen(Screens.MENU_SCREEN);

		for (int i = stringHead; i < credits.size(); i++) {
			String s = credits.get(i);

			char type = 'c'; // types are 'c' -> credit, 't' -> title, 'i' -> image, 's' -> space, 'm' -> music

			if (s.indexOf('#') != -1) {
				type = s.charAt(0);
				s = s.substring(2);
			}

			switch (type) {
				case 't':
					y = processCreditTitle(batch, width, height, y, i, s);
					break;
				case 'i':
					y = processCreditImage(batch, width, height, y, i, s);
					break;
				case 's':
					y = processCreditSpace(height, y, i, s);
					break;
				case 'm':
					processCreditMusic(s);
					break;
				default:
					y = processCreditDefault(batch, width, height, y, i, s);
					break;
			}

			if (y < 0) {
				break;
			}
		}

		batch.end();
	}

	private float processCreditTitle(SpriteBatch batch, int width, int height, float y, int i, String s) {
		final float lineHeight = style.titleFont.getLineHeight();
		y -= lineHeight * 2;

		drawCenteredScreenX(batch, style.titleFont, s, y, width);
		y -= lineHeight;

		if (y > height + lineHeight) {
			stringHead = i + 1;
			scrollY -= lineHeight;
			scrollY -= lineHeight * 2;
		}
		return y;
	}

	private float processCreditImage(SpriteBatch batch, int width, int height, float y, int i, String s) {
		Texture img = images.get(s);
		final int lineHeight = img.getHeight();
		batch.draw(img, (width - img.getWidth()) / 2, y - lineHeight);

		y -= lineHeight;

		if (y > height) {
			stringHead = i + 1;
			scrollY -= lineHeight;
		}
		return y;
	}

	private float processCreditSpace(int height, float y, int i, String s) {
		int lineHeight = (int) (Integer.valueOf(s) * EngineAssetManager.getInstance().getScale());
		y -= lineHeight;

		if (y > height) {
			stringHead = i + 1;
			scrollY -= lineHeight;
		}
		return y;
	}

	private void processCreditMusic(String s) {
		if (music != null)
			music.dispose();

		music = Gdx.audio.newMusic(EngineAssetManager.getInstance().getAsset("music/" + s));
		music.play();
	}

	private float processCreditDefault(SpriteBatch batch, int width, int height, float y, int i, String s) {
		drawCenteredScreenX(batch, style.font, s, y, width);

		final float lineHeight = style.font.getLineHeight();
		y -= lineHeight;

		if (y > height + lineHeight) {
			stringHead = i + 1;
			scrollY -= lineHeight;
		}
		return y;
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		for (Texture t : images.values())
			t.dispose();

		images.clear();
		credits.clear();

		if (music != null) {
			music.stop();
			music.dispose();
		}
	}

	private void retrieveAssets() {
		style = ui.getSkin().get(CreditScreenStyle.class);

		final Locale locale = Locale.getDefault();

		String localeFilename = MessageFormat.format("{0}_{1}.txt", CREDITS_FILENAME, locale.getLanguage());

		if (!EngineAssetManager.getInstance().assetExists(localeFilename))
			localeFilename = MessageFormat.format("{0}.txt", CREDITS_FILENAME);

		BufferedReader reader = EngineAssetManager.getInstance().getAsset(localeFilename).reader(4096, "utf-8");

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				credits.add(line);
			}
		} catch (Exception e) {
			EngineLogger.error(e.getMessage());

			ui.setCurrentScreen(Screens.MENU_SCREEN);
		}

		scrollY += style.titleFont.getLineHeight();

		// Load IMAGES
		for (String s : credits) {
			if (s.indexOf('#') != -1 && s.charAt(0) == 'i') {
				s = s.substring(2);

				Texture tex = new Texture(EngineAssetManager.getInstance().getResAsset("ui/" + s));
				tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

				images.put(s, tex);
			}
		}
	}

	@Override
	public void show() {
		retrieveAssets();
		Gdx.input.setInputProcessor(inputProcessor);

//		int wWidth = EngineAssetManager.getInstance().getResolution().portraitWidth;
//		int wHeight = EngineAssetManager.getInstance().getResolution().portraitHeight;

//		viewport = new ExtendViewport(wWidth, wHeight);
		viewport = new ScreenViewport();

		stringHead = 0;
		scrollY = 0;
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
	}

	public void drawCenteredScreenX(SpriteBatch batch, BitmapFont font, CharSequence str, float y, int viewportWidth) {
		float x = 0;

		layout.setText(font, str, Color.WHITE, viewportWidth, Align.center, true);

		//x = (viewportWidth - layout.width)/2;

		font.draw(batch, layout, x, y);
	}

	/**
	 * The style for the CreditsScreen
	 */
	public static class CreditScreenStyle {
		/**
		 * Optional.
		 */
		public Drawable background;
		/**
		 * if 'bg' not specified try to load the bgFile
		 */
		public String bgFile;
		public BitmapFont titleFont;
		public BitmapFont font;

		public CreditScreenStyle() {
		}

		public CreditScreenStyle(CreditScreenStyle style) {
			background = style.background;
			bgFile = style.bgFile;
			titleFont = style.titleFont;
			font = style.font;
		}
	}
}
