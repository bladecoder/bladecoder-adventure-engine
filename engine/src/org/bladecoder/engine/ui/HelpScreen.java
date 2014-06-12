package org.bladecoder.engine.ui;

import java.text.MessageFormat;
import java.util.Locale;

import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HelpScreen implements Screen, InputProcessor {
	
	private final static String PIE_FILENAME = "ui/helpPie";
	private final static String DESKTOP_FILENAME = "ui/helpDesktop";

	private Texture tex;

	float width, height;

	UI ui;
	
	boolean pieMode;
	String localeFilename;

	public HelpScreen(UI ui, boolean pieMode) {
		this.pieMode = pieMode;
		this.ui = ui;
		
		Locale locale = Locale.getDefault();
		
		String filename = null;
		
		if(pieMode)
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

		batch.setProjectionMatrix(ui.getCamera().combined);
		batch.begin();	
		batch.draw(tex, 0, 0, width, height);
		ui.getPointer().draw(batch);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
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
		ui.runCommand(MenuScreen.BACK_COMMAND, null);
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
