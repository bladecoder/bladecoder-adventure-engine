package org.bladecoder.engine.ui;

import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;

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
	
	public LoadingScreen() {	
		delta = 0;
	}

	@Override
	public void draw(SpriteBatch batch) {
		
		// Only show the screen when time > INIT_TIME
		if(initTime < INIT_TIME) {
			initTime += Gdx.graphics.getDeltaTime();
			return;
		}	
		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		update(Gdx.graphics.getDeltaTime());
		
		for(int i=0; i < numSquares; i++) {

			if(i == pos)
				RectangleRenderer.draw(batch, x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.WHITE);
			else
				RectangleRenderer.draw(batch, x + i * (squareWidth + margin), y, squareWidth, squareHeight, Color.GRAY);
		}
	}
	
	private void update(float d) {
		delta += d;
		
		if(delta > 0.4) {
			pos = (pos + 1) % numSquares;
			delta = 0;
		}
	}

	@Override
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		
	}

	@Override
	public void resize(Rectangle v) {
		x = (v.width - (squareWidth * numSquares + margin * (numSquares -1))) / 2; 
		y = (v.height - squareHeight) / 2;
	}

	@Override
	public void createAssets() {

	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {

	}

	@Override
	public void dispose() {

	}
	
}
