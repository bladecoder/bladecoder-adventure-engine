package org.bladecoder.engine.ui;

import java.io.BufferedReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.TextUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;

public class CreditsScreen implements Screen {

	private final static String CREDITS_FILENAME = "ui/credits";
	private static final String FONT_TITLE_STYLE = "CREDITS_TITLE_FONT";
	private static final String FONT_STYLE = "CREDITS_FONT";
	private static final float SPEED = 40; // px/sec.

	// title and texts pair sequence
	private List<String> credits = new ArrayList<String>();

	private BitmapFont creditsFont;
	private BitmapFont titlesFont;

	private int width, height, stringHead = 0;
	private float scrollY = 0; 

	CommandListener l;
	Pointer pointer;

	Music music;
	
	private HashMap<String, Texture> images = new HashMap<String, Texture>();

	public CreditsScreen(Pointer pointer, CommandListener l) {
		this.pointer = pointer;
		this.l = l;
	}

	@Override
	public void draw(SpriteBatch batch) {
		scrollY+= Gdx.graphics.getDeltaTime() * SPEED * EngineAssetManager.getInstance().getScale();
		
		float y = scrollY;

		if (stringHead >= credits.size())
			l.runCommand(CommandScreen.BACK_COMMAND, null);

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

		pointer.draw(batch);
	}

	@Override
	public void resize(Rectangle v) {
		this.width = (int) v.width;
		this.height = (int) v.height;
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
		creditsFont.dispose();
		titlesFont.dispose();
		
		for(Texture t:images.values())
			t.dispose();
		
		if(music != null) {
			music.stop();
			music.dispose();
		}
			
	}

	@Override
	public void createAssets() {
		// Not needed
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {
		titlesFont = EngineAssetManager.getInstance().loadFont(FONT_TITLE_STYLE);
		creditsFont = EngineAssetManager.getInstance().loadFont(FONT_STYLE);

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
			
			l.runCommand(CommandScreen.BACK_COMMAND, null);
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
}
