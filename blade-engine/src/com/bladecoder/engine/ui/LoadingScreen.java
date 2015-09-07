package com.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.RectangleRenderer;

public class LoadingScreen extends ScreenAdapter implements BladeScreen {
	private final static float INIT_TIME = 1f;

	private int pos = 0;
	private int numSquares = 3;

	private float x, y;

	private float squareWidth = 30f;
	private float squareHeight = 30f;
	private float margin = 10f;

	private float initTime = 0;

	private float delta = 0;

	private UI ui;

	private final Viewport viewport = new ScreenViewport();

	@Override
	public void render(float delta) {
		final World world = World.getInstance();
		if (!world.isDisposed()) {
			world.update(delta);
		}

		final AssetState assetState = world.getAssetState();

		if (assetState == AssetState.LOADED) {
			ui.setCurrentScreen(Screens.SCENE_SCREEN);
			return;
		}

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Only show the screen when time > INIT_TIME
		if (initTime < INIT_TIME) {
			initTime += delta;
			return;
		}

		final SpriteBatch batch = ui.getBatch();

		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		update(delta);

		for (int i = 0; i < numSquares; i++) {
			final Color color = i == pos ? Color.WHITE : Color.GRAY;
			RectangleRenderer.draw(ui.getBatch(), x + i * (squareWidth + margin), y, squareWidth, squareHeight, color);
		}
		batch.end();
	}

	private void update(float d) {
		delta += d;

		if (delta > 0.4) {
			pos = (pos + 1) % numSquares;
			delta = 0;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);

		x = (viewport.getWorldWidth() - (squareWidth * numSquares + margin * (numSquares - 1))) / 2;
		y = (viewport.getWorldHeight() - squareHeight) / 2;
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
		initTime = 0;
		delta = 0;
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
	}
}
