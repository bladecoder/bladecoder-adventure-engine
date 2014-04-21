package org.bladecoder.engine.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;

public class CommandScreen implements Screen {

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

	CommandListener l;
	Pointer pointer;
	
	int viewPortWidth, viewPortHeight;

	public CommandScreen(Pointer pointer, CommandListener l) {
		commands.add(BACK_COMMAND);
		commands.add(RELOAD_COMMAND);
		commands.add(HELP_COMMAND);
		commands.add(CREDITS_COMMAND);
		commands.add(QUIT_COMMAND);
		
		this.pointer = pointer;
		this.l = l;
	}

	@Override
	public void draw(SpriteBatch batch) {
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

		pointer.draw(batch);
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {

		commandsRegions = new ArrayList<AtlasRegion>();

		for (String c : commands) {
			AtlasRegion r = atlas.findRegion(c);
			
			if(r != null)
				commandsRegions.add(r);
		}
	}

	@Override
	public void resize(Rectangle v) {
		viewPortWidth = (int)v.width;
		viewPortHeight = (int)v.height;
		
		commandWidth = commandsRegions.get(0).getRegionWidth() * commands.size()
				+ (MARGIN * commands.size());
		commandHeight = commandsRegions.get(0).getRegionHeight();
	}

	@Override
	public void touchEvent(int type, float x0, float y0, int pointer, int button) {
		if (l == null)
			return;
		
		switch (type) {
		case TOUCH_DOWN:
			selCommand = getCommand(x0,y0);
			break;
			
		case TOUCH_UP:
			
			if(selCommand != null) {
				l.runCommand(selCommand, null);
				selCommand = null;
			}
			
			break;
		}
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
		// Not needed
	}

	@Override
	public void createAssets() {
		// Not needed
	}
}
