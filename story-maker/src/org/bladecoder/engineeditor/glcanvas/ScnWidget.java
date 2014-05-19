package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.utils.EditorLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

public class ScnWidget extends Widget {
	// TMPs to avoid GC calls
	private final Vector3 tmpV3 = new Vector3();
	private final Vector2 tmpV2 = new Vector2();

	private final SpriteBatch sceneBatch = new SpriteBatch();
	private final CanvasDrawer drawer = new CanvasDrawer();
	private final FARenderer faRenderer = new FARenderer();
	private final ScnWidgetInputListener inputListner = new ScnWidgetInputListener(
			this);
	
	private final Rectangle bounds = new Rectangle();
	private final Rectangle scissors = new Rectangle();

	private Scene scn;
	private Texture backgroundTexture;
	private Actor selectedActor = null;
	private boolean inScene = false;
	private boolean animation = true;

	private static final int[] zoomLevels = { 5, 10, 16, 25, 33, 50, 66, 100,
			150, 200, 300, 400, 600, 800, 1000 };
	private int zoomLevel = 100;

	public ScnWidget() {

		setSize(150, 150);
		backgroundTexture = Assets.inst().get(
				"res/images/transparent-light.png", Texture.class);
		backgroundTexture.setWrap(Texture.TextureWrap.Repeat,
				Texture.TextureWrap.Repeat);

		faRenderer.setViewport(getWidth(), getHeight());

		setLayoutEnabled(true);

		addListener(inputListner);
	}

	public void setScene(Scene scn) {
		this.scn = scn;
		if (scn != null)
			drawer.setCamera(scn.getCamera());

		invalidate();
	}

	public void setSelectedActor(Actor a) {
		selectedActor = a;
		faRenderer.setActor(a);
		setFrameAnimation(null);
	}

	public Scene getScene() {
		return scn;
	}

	@Override
	public void act(float delta) {
		faRenderer.update(delta);

		if (scn != null && animation) {
			scn.update(delta);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate();
		
		Color tmp = batch.getColor();
		batch.setColor(Color.WHITE);

		// BACKGROUND
		batch.disableBlending();
		float tw = backgroundTexture.getWidth();
		float th = backgroundTexture.getHeight();
		batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight(),
				0f, 0f, getWidth() / tw, getHeight() / th);
		batch.enableBlending();

		if (scn != null) {
			Vector3 v = new Vector3(getX(), getY(), 0);
			v = v.prj(batch.getTransformMatrix());			
			
			batch.end();

//			System.out.println("X: " + v.x+ " Y:" +  v.y);
			Gdx.gl.glViewport((int) v.x, (int)v.y, (int) getWidth(),
					(int) (getHeight()));

			getStage().calculateScissors(bounds, scissors);
			
			if (ScissorStack.pushScissors(scissors)) {
				// WORLD CAMERA
				sceneBatch.setProjectionMatrix(scn.getCamera().combined);
				sceneBatch.begin();
				scn.draw(sceneBatch);
				sceneBatch.end();
				ScissorStack.popScissors();
			}

			drawer.drawBGBounds();
			drawer.drawBBoxActors(scn);

			if (selectedActor != null) {
				drawer.drawSelectedActor(selectedActor);
			}

			getStage().getViewport().update();

			// SCREEN CAMERA
			batch.begin();

			if (!inScene) {
				faRenderer.draw((SpriteBatch) batch);
			}
			
			
			batch.setColor(tmp);

		} else {
			RectangleRenderer.draw((SpriteBatch) batch, getX(), getY(),
					getWidth(), getHeight(), Color.RED);
		}

	}

	public void setInSceneSprites(boolean v) {
		inScene = v;
	}

	public void setAnimation(boolean v) {
		animation = v;
	}

	public void setFrameAnimation(FrameAnimation fa) {
		faRenderer.setFrameAnimation(fa);
	}

	@Override
	public void layout() {
		EditorLogger.debug("LAYOUT SIZE CHANGED - X: " + getX() + " Y: "
				+ getY() + " Width: " + getWidth() + " Height: " + getHeight());
		EditorLogger.debug("Last Point coords - X: " + (getX() + getWidth())
				+ " Y: " + (getY() + getHeight()));
		localToScreenCoords(tmpV2
				.set(getX() + getWidth(), getY() + getHeight()));
		EditorLogger.debug("Screen Last Point coords:  " + tmpV2);

		faRenderer.setViewport(getWidth(), getHeight());
		bounds.set(getX(), getY(), getWidth(), getHeight());

		// SETS WORLD CAMERA
		if (scn != null) {

			float aspect = getWidth() / getHeight();

			float wWidth = Ctx.project.getWorld().getWidth();
			float wHeight = Ctx.project.getWorld().getHeight();
			float aspectWorld = wWidth / wHeight;

			if (aspectWorld > aspect) {
				wHeight = wWidth / aspect;
			} else {
				wWidth = wHeight * aspect;
			}

			zoomLevel = 100;

			scn.getCamera().setToOrtho(false, wWidth, wHeight);
			// worldCamera.translate(-width / 2, -height / 2, 0);
			scn.getCamera().zoom = 1f;
			scn.getCamera().update();

			translate(new Vector2((-getWidth() + wWidth) / 2,
					(-getHeight() + wHeight) / 2));
		}
	}

	public void zoom(int amount) {
		if (zoomLevel == zoomLevels[0] && amount < 0) {
			zoomLevel = zoomLevels[1];
		} else if (zoomLevel == zoomLevels[zoomLevels.length - 1] && amount > 0) {
			zoomLevel = zoomLevels[zoomLevels.length - 2];
		} else {
			for (int i = 1; i < zoomLevels.length - 1; i++) {
				if (zoomLevels[i] == zoomLevel) {
					zoomLevel = amount > 0 ? zoomLevels[i - 1]
							: zoomLevels[i + 1];
					break;
				}
			}
		}

		scn.getCamera().zoom = 100f / zoomLevel;
		scn.getCamera().update();
	}

	public void translate(Vector2 delta) {
		// EditorLogger.debug("TRANSLATING - X: " + delta.x + " Y: " + delta.y);
		scn.getCamera().translate(-delta.x, -delta.y, 0);
		scn.getCamera().update();
	}

	public void localToScreenCoords(Vector2 coords) {
		localToStageCoordinates(coords);
		getStage().stageToScreenCoordinates(coords);
	}

	public void localToWorldCoords(Vector2 coords) {
		localToStageCoordinates(coords);
		getStage().stageToScreenCoordinates(coords);

		tmpV3.set(coords.x, coords.y, 0);
		getScene().getCamera().unproject(tmpV3, getX(), getY(), getWidth(),
				getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}

	public void screenToWorldCoords(Vector2 coords) {
		tmpV2.set(0, 0);
		localToStageCoordinates(tmpV2);
//		getStage().stageToScreenCoordinates(tmpV2);
		tmpV3.set(coords.x, coords.y, 0);
		getScene().getCamera().unproject(tmpV3, tmpV2.x, tmpV2.y, getWidth(),
				getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}
}
