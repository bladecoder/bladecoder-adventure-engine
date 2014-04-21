package org.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class ScreenCamera {

	private OrthographicCamera screenCamera = null;

	private Rectangle viewport = null;
	
	public ScreenCamera() {
		setViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	public void create(int width, int height) {
		screenCamera = new OrthographicCamera();
		screenCamera.setToOrtho(false, width, height);
		screenCamera.update();
	}

	public Rectangle getViewport() {
		return viewport;
	}

	public void setViewport(float screenWidth, float screenHeight, float worldWidth, float worldHeight) {
		// calculate new viewport
		float winAspectRatio = (float) screenWidth / (float) screenHeight;
		float worldAspectRatio = (float) worldWidth / (float) worldHeight;
		float scale = 1f;
		Vector2 crop = new Vector2(0f, 0f);

		if (winAspectRatio > worldAspectRatio) {
			scale = screenHeight / worldHeight;
			crop.x = (screenWidth - worldWidth * scale) / 2f;
		} else if (winAspectRatio < worldAspectRatio) {
			scale = screenWidth / worldWidth;
			crop.y = (screenHeight - worldHeight * scale) / 2f;
		} else {
			scale = screenWidth / worldWidth;
		}

		float w2 = worldWidth * scale;
		float h2 = worldHeight * scale;
		viewport = new Rectangle(crop.x, crop.y, w2, h2);

		Gdx.gl.glViewport((int) viewport.x, (int) viewport.y, (int) viewport.width,
				(int) viewport.height);

		create((int) viewport.width, (int) viewport.height);
	}


	public OrthographicCamera getCamera() {
		return screenCamera;
	}


	public Vector3 getInputUnProject() {
		Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);

		screenCamera.unproject(touchPos, viewport.x, viewport.y, viewport.width, viewport.height);

		if (touchPos.x >= viewport.width)
			touchPos.x = viewport.width - 1;
		else if (touchPos.x < 0)
			touchPos.x = 0;

		if (touchPos.y >= viewport.height)
			touchPos.y = viewport.height - 1;
		else if (touchPos.y < 0)
			touchPos.y = 0;

		return touchPos;
	}
}
