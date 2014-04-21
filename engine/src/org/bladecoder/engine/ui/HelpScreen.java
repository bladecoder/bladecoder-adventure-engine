package org.bladecoder.engine.ui;

import java.text.MessageFormat;
import java.util.Locale;

import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;

public class HelpScreen implements Screen {
	
	private final static String PIE_FILENAME = "ui/helpPie";
	private final static String DESKTOP_FILENAME = "ui/helpDesktop";

	private Texture tex;

	float width, height;

	CommandListener l;
	Pointer pointer;
	
	boolean pieMode;

	public HelpScreen(Pointer pointer, CommandListener l, boolean pieMode) {
		this.pieMode = pieMode;
		this.pointer = pointer;
		this.l = l;
	}

	@Override
	public void draw(SpriteBatch batch) {
		batch.draw(tex, 0, 0, width, height);
		pointer.draw(batch);
	}

	@Override
	public void resize(Rectangle v) {
		this.width = (int)v.width;
		this.height = (int)v.height;
	}

	@Override
	public void touchEvent(int type, float x0, float y0, int pointer, int button) {
		if (l == null)
			return;
		
		switch (type) {
		case TOUCH_UP:
			
			l.runCommand(CommandScreen.BACK_COMMAND, null);
			
			break;
		}
	}

	@Override
	public void dispose() {
		tex.dispose();
	}


	@Override
	public void createAssets() {
		// Not needed
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {
		Locale locale = Locale.getDefault();
		
		String filename = null;
		
		if(pieMode)
			filename = PIE_FILENAME;
		else
			filename = DESKTOP_FILENAME;
		
		String localeFilename = MessageFormat.format("{0}_{1}.png", filename, locale.getLanguage());
		
		if(!EngineAssetManager.getInstance().assetExists(localeFilename))
			localeFilename = MessageFormat.format("{0}.png", filename);
		
		tex = new Texture(EngineAssetManager.getInstance().getResAsset(localeFilename));
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);			
	}	
}
