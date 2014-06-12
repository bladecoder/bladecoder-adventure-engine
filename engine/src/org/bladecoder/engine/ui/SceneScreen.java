package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Transition;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class SceneScreen implements Screen {

	UI ui;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private Actor selectedActor = null;

	private boolean pieMode;
	boolean dragging = false;

	private Recorder recorder;

	public SceneScreen(UI ui, boolean pieMode) {
		this.ui = ui;

		recorder = new Recorder();

		pie = new PieMenu(recorder);
		textManagerUI = new TextManagerUI(this);
		inventoryUI = new InventoryUI(this, recorder);
		dialogUI = new DialogUI(this, recorder);

		this.pieMode = pieMode;
	}

	public Recorder getRecorder() {
		return recorder;
	}

	private void update(float delta) {
		World w = World.getInstance();
		
		if(!World.getInstance().isDisposed()) {
			World.getInstance().update(delta);
		}

		AssetState assetState = World.getInstance().getAssetState();

		if (assetState != AssetState.LOADED) {
			ui.setScreen(State.LOADING_SCREEN);
			return;
		}
		
		recorder.update(delta);

		if (w.getCurrentDialog() == null && !w.inCutMode()) {

			Actor a = null;

			if (w.getInventory().isVisible()) {
				ui.getPointer().getPosition(unproject);
				a = inventoryUI.getItemAt(unproject.x, unproject.y);
			}

			if (a == null) {
				w.getSceneCamera().getInputUnProject(
						ui.getCamera().getViewport(), unproject);

				a = w.getCurrentScene().getActorAt(unproject.x, unproject.y);
			}

			ui.getPointer().setTarget(a);
		} else {
			ui.getPointer().setTarget(null);

			if (pie.isVisible()) {
				pie.hide();
				ui.getPointer().setFreezeHotSpot(false);
			}

			inventoryUI.cancelDragging();
		}
	}

	@Override
	public void render(float delta) {
		update(delta);
		
		World w = World.getInstance();
		SpriteBatch batch = ui.getBatch();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// WORLD CAMERA
		w.draw();

		if (EngineLogger.debugMode()
				&& EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
			renderer.setProjectionMatrix(w.getSceneCamera().combined);
			w.getCurrentScene().drawBBoxLines(renderer);
			renderer.end();
		}

		if (w.getAssetState() != AssetState.LOADED)
			return;
		
		// SCREEN CAMERA
		batch.setProjectionMatrix(ui.getCamera().combined);
		batch.begin();

		if (EngineLogger.debugMode()) {
			w.getSceneCamera().getInputUnProject(
					ui.getCamera().getViewport(), unproject);

			StringBuilder sb = new StringBuilder();
			sb.append("Mouse ( ");
			sb.append((int) unproject.x);
			sb.append(", ");
			sb.append((int) unproject.y);
			sb.append(") FPS:");
			sb.append(Gdx.graphics.getFramesPerSecond());

			if (w.getCurrentScene().getBackgroundMap() != null) {
				sb.append(" Map: ");
				sb.append((int) (w.getCurrentScene().getBackgroundMap()
						.getDepth(unproject.x, unproject.y) * 10));
			}

			String strDebug = sb.toString();

			TextBounds b = EngineLogger.getDebugFont().getBounds(strDebug);
			RectangleRenderer.draw(batch, 0,
					ui.getCamera().getViewport().height - b.height - 10,
					b.width, b.height + 10, Color.BLACK);
			EngineLogger.getDebugFont().draw(batch, strDebug, 0,
					ui.getCamera().getViewport().height);
		}

		if (World.getInstance().getCurrentDialog() != null
				&& !recorder.isPlaying()) { // DIALOG MODE

			if (!World.getInstance().inCutMode()) {
				ui.getPointer().getPosition(unproject);
				dialogUI.draw(batch, (int) unproject.x, (int) unproject.y);
			}

			textManagerUI.draw(batch);
			ui.getPointer().draw(batch, false);
		} else {

			textManagerUI.draw(batch);

			ui.getPointer().getPosition(unproject);
			inventoryUI.draw(batch, (int) unproject.x, (int) unproject.y);

			if (pieMode)
				pie.draw(batch);

			if (!World.getInstance().inCutMode() && !recorder.isPlaying())
				ui.getPointer().draw(batch, dragging);
		}

		Transition t = World.getInstance().getCurrentScene().getTransition();

		if (t != null) {
			t.draw(batch, ui.getCamera().getViewport().width, ui.getCamera()
					.getViewport().height);
		}

		recorder.draw(batch);

		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		pie.resize(width, height);
		inventoryUI.resize(width, height);
		dialogUI.resize(width, height);
		textManagerUI.resize(width, height);
	}

	public void dispose() {
		textManagerUI.dispose();
		dialogUI.dispose();
		renderer.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
		pie.retrieveAssets(atlas);
		inventoryUI.retrieveAssets(atlas);
		textManagerUI.retrieveAssets(atlas);
	}
	
	/**
	 * Create assets not managed (fonts)
	 */
	private void createAssets() {
		textManagerUI.createAssets();
		dialogUI.createAssets();
	}

	private final Vector3 unproject = new Vector3();
	
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		World w = World.getInstance();

		if (w.isPaused() || recorder.isPlaying())
			return;
		
		ui.getCamera().getInputUnProject(unproject);

		switch (type) {
		case TouchEventListener.TOUCH_UP:

			if (w.inCutMode() && !recorder.isRecording()) {
				w.getTextManager().next();
			} else if (w.getCurrentDialog() != null) {
				dialogUI.touchEvent(TouchEventListener.TOUCH_UP, unproject.x, unproject.y, pointer,
						button);
			} else if (w.getCurrentScene().getOverlay() != null) {
				w.getCurrentScene().getOverlay().click();
			} else if (dragging) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_UP, unproject.x, unproject.y,
						pointer, button);
				dragging = false;
			} else if (button == 1 && !pieMode) {
				ui.getPointer().toggleSelectedVerb();
			} else if (pie.isVisible()) {
				ui.getPointer().setFreezeHotSpot(false);
				pie.touchEvent(TouchEventListener.TOUCH_UP, unproject.x, unproject.y, pointer,
						button);
			} else if (inventoryUI.contains(x, y)) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_UP, unproject.x, unproject.y,
						pointer, button);
			} else {
				sceneClick();
			}
			break;

		case TouchEventListener.TOUCH_DOWN:
			if (pie.isVisible()) {
				pie.touchEvent(TouchEventListener.TOUCH_DOWN, unproject.x, unproject.y, pointer,
						button);
			} else if (!w.inCutMode() && inventoryUI.contains(unproject.x, unproject.y)) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_DOWN, unproject.x, unproject.y,
						pointer, button);
			}

			break;

		case TouchEventListener.DRAG:
			if (inventoryUI.contains(unproject.x, unproject.y)
					&& inventoryUI.getItemAt(unproject.x, unproject.y) != null && !dragging) {

				inventoryUI.touchEvent(TouchEventListener.DRAG, unproject.x, unproject.y, pointer,
						button);

				dragging = true;

				if (pie.isVisible()) {
					pie.hide();
					ui.getPointer().setFreezeHotSpot(false);
				}
			}
			break;
		}
	}

	private void sceneClick() {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(
				ui.getCamera().getViewport(), unproject);

		Scene s = w.getCurrentScene();

		Actor a = s.getActorAt(unproject.x, unproject.y);

		if (a != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(a.toString());
			}

			actorClick(a);
		} else if (s.getPlayer() != null) {
			if (s.getPlayer().getVerb("goto") != null) {
				if (recorder.isRecording()) {
					recorder.add(s.getPlayer().getId(), "goto", null);
				}

				s.getPlayer().runVerb("goto");
			} else {
				Vector2 pos = new Vector2(unproject.x, unproject.y);

				if (recorder.isRecording()) {
					recorder.add(pos);
				}

				s.getPlayer().goTo(pos, null);
			}
		}
	}

	private void actorClick(Actor a) {
		if (a.getVerb("leave") != null) {
			if (recorder.isRecording()) {
				recorder.add(a.getId(), "leave", null);
			}

			a.runVerb("leave");
		} else if (!pieMode) {
			if (recorder.isRecording()) {
				recorder.add(a.getId(), ui.getPointer().getSelectedVerb(), null);
			}

			a.runVerb(ui.getPointer().getSelectedVerb());
		} else {
			ui.getPointer().getPosition(unproject);
			pie.show(a, unproject.x, unproject.y);
			ui.getPointer().setFreezeHotSpot(true);
		}
	}

	public void runCommand(String command, Object param) {

		if (command.equals(CommandListener.RUN_VERB_COMMAND)) {
			selectedActor = (Actor) param;
			actorClick(selectedActor);
		} else if (command.equals(DialogUI.DIALOG_END_COMMAND)) {
			World.getInstance().setCurrentDialog(null);
		} else {
			ui.runCommand(command, param);
		}
	}

	public void resetUI() {

		if (pie.isVisible()) {
			pie.hide();
			ui.getPointer().setFreezeHotSpot(false);
		}

		ui.getPointer().setTarget(null);

		dragging = false;
	}

	public InventoryUI getInventoryUI() {
		return inventoryUI;
	}

	@Override
	public void show() {
		createAssets();
		retrieveAssets(ui.getUIAtlas());
		
		Gdx.input.setInputProcessor(new SceneInputProcessor(this));

		if (World.getInstance().isDisposed()) {
			try {
				World.getInstance().load();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);

				dispose();
				Gdx.app.exit();
			}
		}

		World.getInstance().resume();
	}

	@Override
	public void hide() {
		World.getInstance().pause();
		ui.getPointer().setTarget(null);
		dispose();
	}

	@Override
	public void pause() {
		World.getInstance().pause();
	}

	@Override
	public void resume() {
		if(Gdx.app.getType() == ApplicationType.Android) {
			// RESTORE GL CONTEXT
			createAssets();
		}
		
		World.getInstance().resume();
	}

	public Rectangle getViewPort() {
		return ui.getCamera().getViewport();
	}

}
