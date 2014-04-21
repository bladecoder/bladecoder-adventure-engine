package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;

public class InitScreen implements Screen {
	private final static String FILENAME = "ui/blade_logo.png";
	private final static float FADE_TIME = 1f;
	private final static float SCREEN_TIME = 1.5f;

	private Texture tex;	
	
	CommandListener l;
	boolean loadingGame = false;
	boolean restart = false;
	
	float time;
	float fadeTime;
	float width, height;
	
	public InitScreen(CommandListener l, boolean restart) {
		this.l = l;
		this.restart = restart;
	}
	
	@Override
	public void touchEvent(int type, float x, float y, int pointer, int button) {

	}

	@Override
	public void draw(SpriteBatch batch) {
		if(fadeTime < FADE_TIME && !loadingGame) { // FADE IN
			batch.setColor(1, 1, 1,  fadeTime/FADE_TIME);	
		} else	if(fadeTime < FADE_TIME && loadingGame) {  // FADE_OUT
			batch.setColor(1, 1, 1,  1 - fadeTime/FADE_TIME);
		} if(fadeTime >= FADE_TIME && loadingGame) {  // EXIT INIT SCREEN
			l.runCommand(CommandScreen.BACK_COMMAND, null);
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
		
		batch.draw(tex, (width - tex.getWidth()) /2, (height - tex.getHeight()) /2);
		batch.setColor(1, 1, 1, 1);
	
		time += Gdx.graphics.getDeltaTime();
		fadeTime += Gdx.graphics.getDeltaTime();
	}

	@Override
	public void resize(Rectangle v) {		
		width = v.width;
		height = v.height;
	}

	@Override
	public void createAssets() {
		
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {		
		tex = new Texture(EngineAssetManager.getInstance().getResAsset(FILENAME));
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);	
	}

	@Override
	public void dispose() {
		tex.dispose();
	}
}
