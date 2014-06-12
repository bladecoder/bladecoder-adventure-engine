package org.bladecoder.engine.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class MenuScreen implements Screen, InputProcessor {

	public static final String BACK_COMMAND = "back";
	public static final String QUIT_COMMAND = "quit";
	public static final String RELOAD_COMMAND = "reload";
	public static final String HELP_COMMAND = "help";
	public static final String CREDITS_COMMAND = "credits";

	private List<String> commands = new ArrayList<String>();
	private List<AtlasRegion> commandsRegions;

	float commandWidth, commandHeight;
	String selCommand = null;

	private static final float MARGIN = 30;

	UI ui;
	
	int viewPortWidth, viewPortHeight;

	public MenuScreen(UI ui) {
		commands.add(BACK_COMMAND);
		commands.add(RELOAD_COMMAND);
		commands.add(HELP_COMMAND);
		commands.add(CREDITS_COMMAND);
		commands.add(QUIT_COMMAND);
		
		this.ui = ui;
	}

	@Override
	public void render(float delta) {
		
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(ui.getCamera().combined);
		batch.begin();		
		
		float x = (viewPortWidth - commandWidth) / 2;
		float y = (viewPortHeight - commandHeight) / 2;

		for(int i = 0; i < commandsRegions.size(); i++) {
			AtlasRegion r = commandsRegions.get(i);
			
			if(selCommand != null && commands.get(i).equals(selCommand))
				batch.setColor(Color.GRAY);
				
			batch.draw(r, x, y);
			batch.setColor(Color.WHITE);
			
			x += commandHeight + MARGIN;
		}

		ui.getPointer().draw(batch);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewPortWidth = width;
		viewPortHeight = height;
		
		commandWidth = commandsRegions.get(0).getRegionWidth() * commands.size()
				+ (MARGIN * commands.size());
		commandHeight = commandsRegions.get(0).getRegionHeight();
	}
	
	private String getCommand(float x0, float y0) {
		
		float x = (viewPortWidth - commandWidth) / 2;
		float y = (viewPortHeight - commandHeight) / 2;
		
		Rectangle bbox = new Rectangle();

		for (String c : commands) {
			bbox.set(x, y, commandHeight, commandHeight);

			if (bbox.contains(x0, y0)) {
				return c;
			}

			x += commandHeight + MARGIN;
		}
		
		return null;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void show() {
		commandsRegions = new ArrayList<AtlasRegion>();

		for (String c : commands) {
			AtlasRegion r = ui.getUIAtlas().findRegion(c);
			
			if(r != null)
				commandsRegions.add(r);
		}
		
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

	private final Vector3 unproject = new Vector3();
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		ui.getCamera().getInputUnProject(unproject);
		selCommand = getCommand(unproject.x, unproject.y);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if(selCommand != null) {
			ui.runCommand(selCommand, null);
			selCommand = null;
		}
		
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
