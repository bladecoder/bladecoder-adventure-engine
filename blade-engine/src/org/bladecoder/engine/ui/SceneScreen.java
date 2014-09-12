/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Transition;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.DPIUtils;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SceneScreen implements Screen {

	private UI ui;

	private Stage stage;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private InventoryButton inventoryButton;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private boolean pieMode;

	private final Recorder recorder = new Recorder();
	private InputMultiplexer multiplexer = new InputMultiplexer();

	// private final SceneFitViewport viewport = new SceneFitViewport();
	private final SceneExtendViewport viewport = new SceneExtendViewport();

	private final Vector3 unprojectTmp = new Vector3();
	private final Vector2 unproject2Tmp = new Vector2();

	// Actor under the cursor
	private Actor currentActor = null;

	private boolean drawHotspots = false;
	private final boolean showDesc = Config.getProperty(Config.SHOW_DESC_PROP,
			true);

	private static enum UIStates {
		SCENE_MODE, CUT_MODE, PLAY_MODE, PAUSE_MODE, INVENTORY_MODE, DIALOG_MODE
	};

	private UIStates state = UIStates.SCENE_MODE;

	private final GestureDetector inputProcessor = new GestureDetector(
			new GestureDetector.GestureListener() {
				@Override
				public boolean touchDown(float x, float y, int pointer,
						int button) {
					return true;
				}

				@Override
				public boolean tap(float x, float y, int count, int button) {
					EngineLogger.debug("Event TAP button: " + button);

					World w = World.getInstance();

					if (state == UIStates.PAUSE_MODE
							|| state == UIStates.PLAY_MODE)
						return true;

					if (drawHotspots)
						drawHotspots = false;
					else {
						viewport.getInputUnProject(unprojectTmp);

						if (state == UIStates.CUT_MODE
								&& !recorder.isRecording()) {
							w.getTextManager().next();
						} else if (state == UIStates.INVENTORY_MODE) {
							inventoryUI.hide();
						} else if (state == UIStates.SCENE_MODE) {
							sceneClick(button == 1);
						}
					}

					return true;
				}

				@Override
				public boolean longPress(float x, float y) {
					EngineLogger.debug("Event LONG PRESS");

					if (state == UIStates.SCENE_MODE) {
						drawHotspots = true;
					}

					return false;
				}

				@Override
				public boolean pan(float x, float y, float deltaX, float deltaY) {
					return true;
				}

				@Override
				public boolean panStop(float x, float y, int pointer, int button) {
					return true;
				}

				@Override
				public boolean fling(float velocityX, float velocityY,
						int button) {
					return false;
				}

				@Override
				public boolean zoom(float initialDistance, float distance) {
					return false;
				}

				@Override
				public boolean pinch(Vector2 initialPointer1,
						Vector2 initialPointer2, Vector2 pointer1,
						Vector2 pointer2) {
					return false;
				}
			}) {
		@Override
		public boolean keyUp(int keycode) {
			switch (keycode) {
			case Input.Keys.ESCAPE:
			case Input.Keys.BACK:
			case Input.Keys.MENU:
				showMenu();
				break;
			}

			return true;
		}

		@Override
		public boolean keyTyped(char character) {
			switch (character) {

			case 'd':
				EngineLogger.toggle();
				break;
			case '1':
				EngineLogger.setDebugLevel(EngineLogger.DEBUG0);
				break;
			case '2':
				EngineLogger.setDebugLevel(EngineLogger.DEBUG1);
				break;
			case '3':
				EngineLogger.setDebugLevel(EngineLogger.DEBUG2);
				break;
			case 'f':
				// ui.toggleFullScreen();
				break;
			case 's':
				World.getInstance().saveGameState();
				break;
			case 'r':
				World.getInstance().newGame();
				break;
			case 'l':
				World.getInstance().loadGameState();
				break;
			case '.':
				if (getRecorder().isRecording())
					getRecorder().setRecording(false);
				else
					getRecorder().setRecording(true);
				break;
			case ',':
				if (getRecorder().isPlaying())
					getRecorder().setPlaying(false);
				else {
					getRecorder().load();
					getRecorder().setPlaying(true);
				}
				break;
			case 'p':
				if (World.getInstance().isPaused()) {
					World.getInstance().resume();
				} else {
					World.getInstance().pause();
				}
				break;
			}

			return false;
		}
	};

	public SceneScreen(UI ui) {
		this.ui = ui;

		pie = new PieMenu(this);
		textManagerUI = new TextManagerUI(this);
		inventoryUI = new InventoryUI(this);
		inventoryButton = new InventoryButton(ui.getSkin(), inventoryUI);
		dialogUI = new DialogUI(recorder);

		this.pieMode = ui.isPieMode();

		pie.setVisible(false);
	}

	public UI getUI() {
		return ui;
	}

	public Recorder getRecorder() {
		return recorder;
	}

	private void setUIState(UIStates s) {
		if (state == s)
			return;

		switch (s) {
		case PAUSE_MODE:
		case PLAY_MODE:
		case CUT_MODE:
			if (pieMode && pie.isVisible())
				pie.hide();

			if (inventoryUI.isVisible())
				inventoryUI.hide();

			if (inventoryButton.isVisible())
				inventoryButton.setVisible(false);

			if (dialogUI.isVisible())
				dialogUI.setVisible(false);

			inventoryUI.cancelDragging();
			ui.getPointer().reset();
			break;
		case DIALOG_MODE:
			if (pieMode && pie.isVisible())
				pie.hide();

			if (inventoryUI.isVisible())
				inventoryUI.hide();

			if (inventoryButton.isVisible())
				inventoryButton.setVisible(false);

			if (!dialogUI.isVisible())
				dialogUI.setVisible(true);

			inventoryUI.cancelDragging();
			break;
		case INVENTORY_MODE:
			if (pieMode && pie.isVisible())
				pie.hide();

			if (!inventoryUI.isVisible())
				inventoryUI.show();

			if (!inventoryButton.isVisible())
				inventoryButton.setVisible(true);

			if (!dialogUI.isVisible())
				dialogUI.setVisible(true);
			break;
		case SCENE_MODE:
			if (pieMode && pie.isVisible())
				pie.hide();

			if (inventoryUI.isVisible())
				inventoryUI.hide();

			if (dialogUI.isVisible())
				dialogUI.setVisible(false);
			break;
		}

		state = s;
	}

	private void update(float delta) {
		World w = World.getInstance();
		currentActor = null;

		if (!World.getInstance().isDisposed()) {
			World.getInstance().update(delta);
		}

		AssetState assetState = World.getInstance().getAssetState();

		if (assetState != AssetState.LOADED) {
			ui.setScreen(State.LOADING_SCREEN);
			return;
		}

		// CHECK FOR STATE CHANGES
		switch (state) {
		case CUT_MODE:
			if (!w.inCutMode())
				setUIState(UIStates.SCENE_MODE);
			break;
		case DIALOG_MODE:
			if (w.getCurrentDialog() == null)
				setUIState(UIStates.SCENE_MODE);
			break;
		case INVENTORY_MODE:
			if (!inventoryUI.isVisible())
				setUIState(UIStates.SCENE_MODE);
			break;
		case PAUSE_MODE:
			if (!w.isPaused())
				setUIState(UIStates.SCENE_MODE);
			break;
		case PLAY_MODE:
			if (!recorder.isPlaying())
				setUIState(UIStates.SCENE_MODE);
			break;
		case SCENE_MODE:
			if (w.isPaused())
				setUIState(UIStates.PAUSE_MODE);
			else if (w.inCutMode())
				setUIState(UIStates.CUT_MODE);
			else if (recorder.isPlaying())
				setUIState(UIStates.PLAY_MODE);
			else if (inventoryUI.isVisible())
				setUIState(UIStates.INVENTORY_MODE);
			else if (w.getCurrentDialog() != null)
				setUIState(UIStates.DIALOG_MODE);
			break;
		}

		stage.act(delta);

		if (state == UIStates.PAUSE_MODE)
			return;

		recorder.update(delta);

		if (state == UIStates.INVENTORY_MODE) {
			unproject2Tmp.set(Gdx.input.getX(), Gdx.input.getY());
			inventoryUI.screenToLocalCoordinates(unproject2Tmp);
			currentActor = inventoryUI.getItemAt(unproject2Tmp.x,
					unproject2Tmp.y);
		} else if (state == UIStates.SCENE_MODE) {
			w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

			currentActor = w.getCurrentScene().getActorAt(unprojectTmp.x,
					unprojectTmp.y);
			
			if(!w.getInventory().isVisible() && inventoryButton.isVisible())
				inventoryButton.setVisible(false);
			else if(w.getInventory().isVisible() && !inventoryButton.isVisible())
				inventoryButton.setVisible(true);
		}

		if (!pie.isVisible()) {
			if (currentActor != null) {

				if (showDesc)
					ui.getPointer().setDesc(currentActor.getDesc());

				if (currentActor.getVerb("leave") != null)
					ui.getPointer().setLeaveIcon();
				else
					ui.getPointer().setHotspotIcon();
			} else {
				ui.getPointer().setDefaultIcon();
			}
		}
	}

	@Override
	public void render(float delta) {
		World w = World.getInstance();

		update(delta);

		if (w.getAssetState() != AssetState.LOADED)
			return;

		SpriteBatch batch = ui.getBatch();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// WORLD CAMERA
		w.draw();

		// DRAW DEBUG BBOXES
		if (EngineLogger.debugMode()
				&& EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
			renderer.setProjectionMatrix(w.getSceneCamera().combined);
			w.getCurrentScene().drawBBoxLines(renderer);
			renderer.end();
		}

		// STAGE
		stage.draw();

		// SCREEN CAMERA
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

			StringBuilder sb = new StringBuilder();
			sb.append("Mouse ( ");
			sb.append((int) unprojectTmp.x);
			sb.append(", ");
			sb.append((int) unprojectTmp.y);
			sb.append(") FPS:");
			sb.append(Gdx.graphics.getFramesPerSecond());
			sb.append(" Density:");
			sb.append(Gdx.graphics.getDensity());
			sb.append(" UI Multiplier:");
			sb.append(DPIUtils.getSizeMultiplier(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

			if (w.getCurrentScene().getPlayer() != null) {
				sb.append(" Depth Scale: ");
				sb.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
			}

			String strDebug = sb.toString();

			TextBounds b = EngineLogger.getDebugFont().getBounds(strDebug);
			RectangleRenderer.draw(batch, 0, viewport.getScreenHeight()
					- b.height - 10, b.width, b.height + 10, Color.BLACK);
			EngineLogger.getDebugFont().draw(batch, strDebug, 0,
					viewport.getScreenHeight());
		}

		if (!World.getInstance().inCutMode() && !recorder.isPlaying()) {
			ui.getPointer().draw(batch, viewport);
		}

		Transition t = World.getInstance().getCurrentScene().getTransition();

		if (t != null) {
			t.draw(batch, viewport.getScreenWidth(), viewport.getScreenHeight());
		}

		recorder.draw(batch);

		if (drawHotspots)
			drawHotspots(batch);

		batch.end();
	}

	private void drawHotspots(SpriteBatch batch) {

		for (Actor a : World.getInstance().getCurrentScene().getActors()
				.values()) {
			if (a == World.getInstance().getCurrentScene().getPlayer()
					|| !a.hasInteraction() || !a.isVisible())
				continue;

			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING HOTSPOT FOR: " + a.getId());
			}

			Rectangle r = a.getBBox().getBoundingRectangle();

			unprojectTmp.set(r.getX() + r.getWidth() / 2,
					r.getY() + r.getHeight() / 2, 0);
			World.getInstance().getSceneCamera()
					.scene2screen(viewport, unprojectTmp);

			ui.getPointer().drawHotspot(batch, unprojectTmp.x, unprojectTmp.y,
					showDesc ? a.getDesc() : null);
		}
	}

	@Override
	public void resize(int width, int height) {

		if (!World.getInstance().isDisposed()) {
			viewport.setWorldSize(World.getInstance().getWidth(), World
					.getInstance().getHeight());
			viewport.update(width, height, true);
			World.getInstance().resize(viewport.getWorldWidth(),
					viewport.getWorldHeight());
		} else {
			viewport.setWorldSize(width, height);
			viewport.update(width, height, true);
		}

		pie.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
		inventoryUI.resize(viewport.getScreenWidth(),
				viewport.getScreenHeight());
		textManagerUI.resize(viewport.getScreenWidth(),
				viewport.getScreenHeight());
		inventoryButton.resize(viewport.getScreenWidth(),
				viewport.getScreenHeight());
	}

	public void dispose() {
		textManagerUI.dispose();
		dialogUI.dispose();
		renderer.dispose();
		stage.dispose();
		pie.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
		pie.retrieveAssets(atlas);
		inventoryUI.retrieveAssets(atlas);
		textManagerUI.retrieveAssets(atlas);
	}

	private void sceneClick(boolean lookat) {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

		Scene s = w.getCurrentScene();

		if (currentActor != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(currentActor.toString());
			}

			actorClick(currentActor, lookat);
		} else if (s.getPlayer() != null) {
			if (s.getPlayer().getVerb("goto") != null) {
				runVerb(s.getPlayer(), "goto", null);
			} else {
				Vector2 pos = new Vector2(unprojectTmp.x, unprojectTmp.y);

				if (recorder.isRecording()) {
					recorder.add(pos);
				}

				s.getPlayer().goTo(pos, null);
			}
		}
	}

	public void actorClick(Actor a, boolean lookat) {

		if (a.getVerb("leave") != null) {
			runVerb(a, "leave", null);
		} else if (!pieMode) {
			String verb = "lookat";

			if (!lookat) {
				verb = a.getVerb("talkto") != null ? "talkto" : "pickup";
			}

			runVerb(a, verb, null);
		} else {
			viewport.getInputUnProject(unprojectTmp);
			pie.show(a, unprojectTmp.x, unprojectTmp.y);
			ui.getPointer().reset();
		}
	}

	/**
	 * Run actor verb and handles recording
	 * 
	 * @param a
	 * @param verb
	 * @param target
	 */
	public void runVerb(Actor a, String verb, String target) {
		if (inventoryUI.isVisible())
			inventoryUI.hide();

		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	public void showMenu() {
		ui.setScreen(State.MENU_SCREEN);
	}

	private void resetUI() {

		if (pie.isVisible()) {
			pie.hide();
		}

		ui.getPointer().reset();

		currentActor = null;
	}

	public InventoryUI getInventoryUI() {
		return inventoryUI;
	}

	@Override
	public void show() {
		dialogUI.loadAssets();
		retrieveAssets(ui.getUIAtlas());

		stage = new Stage(viewport);
		stage.addActor(textManagerUI);
		stage.addActor(dialogUI);
		stage.addActor(inventoryUI);
		stage.addActor(inventoryButton);
		stage.addActor(pie);

		multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);

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
		resetUI();
		dispose();
	}

	@Override
	public void pause() {
		World.getInstance().pause();
	}

	@Override
	public void resume() {
		World.getInstance().resume();
	}

	public Viewport getViewport() {
		return viewport;
	}

	public Actor getCurrentActor() {
		return currentActor;
	}

}
