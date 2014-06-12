package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class LoadingScreen implements Screen {
	private final static float INIT_TIME = 1f;
	
	int pos = 0;
	int numSquares = 3;
	
	float x, y;
	
	float squareWidth = 30f;
	float squareHeight = 30f;
	float margin = 10f;
	
	float initTime = 0;
	
	float delta;
	
	private UI ui;
	
	public LoadingScreen(UI ui) {	
		delta = 0;
		this.ui = ui;
	}

	@Override
	public void render(float delta) {
		if(!World.getInstance().isDisposed()) {
			World.getInstance().update(delta);
		}

		AssetState assetState = World.getInstance().getAssetState();

		if (assetState == AssetState.LOADED) {
			ui.setScreen(State.SCENE_SCREEN);
			return;
		}		
		
		// Only show the screen when time > INIT_TIME
		if(initTime < INIT_TIME) {
			initTime += delta;
			return;
		}			
		
		SpriteBatch batch = ui.getBatch();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);			
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(ui.getCamera().combined);
		batch.begin();			
				
		update(delta);
		
		for(int i=0; i < numSquares; i++) {

			if(i == pos)
				RectangleRenderer.draw(ui.getBatch(), x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.WHITE);
			else
				RectangleRenderer.draw(ui.getBatch(), x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.GRAY);
		}
		batch.end();
	}
	
	private void update(float d) {
		delta += d;
		
		if(delta > 0.4) {
			pos = (pos + 1) % numSquares;
			delta = 0;
		}
	}

	@Override
	public void resize(int width, int height) {
		x = (width - (squareWidth * numSquares + margin * (numSquares -1))) / 2; 
		y = (height - squareHeight) / 2;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void hide() {
		
	}

	@Override
	public void pause() {
		
	}

	@Override
	public void resume() {
		
	}
	
}
