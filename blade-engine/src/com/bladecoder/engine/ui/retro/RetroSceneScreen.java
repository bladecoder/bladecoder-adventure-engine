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
package com.bladecoder.engine.ui.retro;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.ui.DialogUI;
import com.bladecoder.engine.ui.MenuButton;
import com.bladecoder.engine.ui.Pointer;
import com.bladecoder.engine.ui.Recorder;
import com.bladecoder.engine.ui.SceneFitViewport;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.TesterBot;
import com.bladecoder.engine.ui.TextManagerUI;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class RetroSceneScreen implements SceneScreen {
	private static final float UI_SCREEN_PERCENT = 1 - 144.0f / 200.0f; // % of
																		// screen
																		// height
																		// of
																		// verbui;

	private UI ui;

	private Stage stage;

	// we need an stage for the TextManagerUI because it runs inside the world
	// viewport
	private Stage worldViewportStage;

	private VerbUI verbUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private MenuButton menuButton;

	private Recorder recorder;
	private TesterBot testerBot;

	private final Viewport screenViewport;
	private final Viewport worldViewport;

	private final Vector3 unprojectTmp = new Vector3();

	private final StringBuilder sbTmp = new StringBuilder();

	// BaseActor under the cursor
	private InteractiveActor currentActor = null;

	private boolean drawHotspots = false;

	private float speed = 1.0f;

	private static enum UIStates {
		SCENE_MODE, CUT_MODE, PLAY_MODE, PAUSE_MODE, DIALOG_MODE, TESTER_BOT_MODE
	};

	private UIStates state = UIStates.SCENE_MODE;

	private final GlyphLayout textLayout = new GlyphLayout();

	private Pointer pointer;

	private final GestureDetector inputProcessor = new GestureDetector(new GestureDetector.GestureAdapter() {
		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			return true;
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			EngineLogger.debug("Event TAP button: " + button);

			World w = World.getInstance();

			if (state == UIStates.PAUSE_MODE || state == UIStates.PLAY_MODE || state == UIStates.TESTER_BOT_MODE)
				return true;

			if (drawHotspots)
				drawHotspots = false;
			else {
				if (w.inCutMode() && !recorder.isRecording()) {
					w.getTextManager().next();
				} else if (state == UIStates.SCENE_MODE) {
					sceneClick(button);
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
	}) {
		@Override
		public boolean keyUp(int keycode) {
			switch (keycode) {
			case Input.Keys.ESCAPE:
			case Input.Keys.BACK:
			case Input.Keys.MENU:
				ui.setCurrentScreen(Screens.MENU_SCREEN);
				break;
			case Input.Keys.SPACE:
				if (drawHotspots)
					drawHotspots = false;
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
				try {
					World.getInstance().saveGameState();
				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
				break;
			case 'r':
				World.getInstance().newGame();
				break;
			case 'l':
				try {
					World.getInstance().loadGameState();
				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
				break;
			case 't':
				testerBot.setEnabled(!testerBot.isEnabled());
				break;
			case '.':
				if (recorder.isRecording())
					recorder.setRecording(false);
				else
					recorder.setRecording(true);
				break;
			case ',':
				if (recorder.isPlaying())
					recorder.setPlaying(false);
				else {
					recorder.load();
					recorder.setPlaying(true);
				}
				break;
			case 'p':
				if (World.getInstance().isPaused()) {
					World.getInstance().resume();
				} else {
					World.getInstance().pause();
				}
				break;
			case ' ':
				if (state == UIStates.SCENE_MODE) {
					drawHotspots = true;
				}
				break;
			}

			// FIXME: This is returning false even in the cases where we
			// actually process the character
			return false;
		}
	};

	public RetroSceneScreen() {
		screenViewport = new SceneFitViewport();

		worldViewport = new Viewport() {
			// This is the World Viewport. It is like a ScreenViewport but the
			// camera is the same that the screenViewport;
			@Override
			public void apply(boolean centerCamera) {
				Gdx.gl.glViewport(getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
				getCamera().viewportWidth = getScreenWidth();
				getCamera().viewportHeight = getScreenHeight();
				if (centerCamera)
					getCamera().position.set(getScreenWidth() / 2, getScreenHeight() / 2, 0);
				getCamera().update();
			}
		};

		worldViewport.setCamera(screenViewport.getCamera());
	}

	public UI getUI() {
		return ui;
	}

	private void setUIState(UIStates s) {
		if (state == s)
			return;

		switch (s) {
		case PAUSE_MODE:
		case PLAY_MODE:
		case TESTER_BOT_MODE:
		case CUT_MODE:
			dialogUI.setVisible(false);
			verbUI.hide();
			pointer.hide();
			break;
		case DIALOG_MODE:
			dialogUI.show();
			verbUI.hide();
			pointer.show();
			break;
		case SCENE_MODE:
			dialogUI.hide();
			verbUI.show();
			pointer.show();
			break;
		}

		state = s;
	}

	/**
	 * Sets the game speed. Can be used to fastfordward
	 *
	 * @param s
	 *            The multiplier speed. ej. 2.0
	 */
	public void setSpeed(float s) {
		speed = s;
	}

	public float getSpeed() {
		return speed;
	}

	private void update(float delta) {
		final World world = World.getInstance();

		currentActor = null;

		if (!world.isDisposed()) {
			world.update(delta * speed);
		}

		AssetState assetState = world.getAssetState();

		if (assetState != AssetState.LOADED) {
			ui.setCurrentScreen(Screens.LOADING_SCREEN);
			return;
		}

		// CHECK FOR STATE CHANGES
		switch (state) {
		case CUT_MODE:
			if (!world.inCutMode())
				setUIState(UIStates.SCENE_MODE);
			break;
		case DIALOG_MODE:
			if (world.getCurrentDialog() == null)
				setUIState(UIStates.SCENE_MODE);
			else if (world.inCutMode())
				setUIState(UIStates.CUT_MODE);
			break;
		case PAUSE_MODE:
			if (!world.isPaused())
				setUIState(UIStates.SCENE_MODE);
			break;
		case PLAY_MODE:
			if (!recorder.isPlaying())
				setUIState(UIStates.SCENE_MODE);
			break;
		case TESTER_BOT_MODE:
			if (!testerBot.isEnabled())
				setUIState(UIStates.SCENE_MODE);
			break;
		case SCENE_MODE:
			if (world.isPaused())
				setUIState(UIStates.PAUSE_MODE);
			else if (world.inCutMode())
				setUIState(UIStates.CUT_MODE);
			else if (recorder.isPlaying())
				setUIState(UIStates.PLAY_MODE);
			else if (testerBot.isEnabled())
				setUIState(UIStates.TESTER_BOT_MODE);
			else if (world.getCurrentDialog() != null)
				setUIState(UIStates.DIALOG_MODE);
			break;
		}

		stage.act(delta);
		worldViewportStage.act(delta);

		if (state == UIStates.PAUSE_MODE)
			return;

		recorder.update(delta * speed);
		testerBot.update(delta * speed);

		if (state == UIStates.SCENE_MODE) {
			world.getSceneCamera().getInputUnProject(worldViewport, unprojectTmp);

			final Scene currentScene = world.getCurrentScene();

			final float tolerance;

			if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				tolerance = DPIUtils.getTouchMinSize();
			else
				tolerance = 0;

			currentActor = currentScene.getInteractiveActorAt(unprojectTmp.x, unprojectTmp.y, tolerance);

			verbUI.setCurrentActor(currentActor);

			if (world.getInventory().isVisible())
				verbUI.show();
			else
				verbUI.hide();

		}
	}

	@Override
	public void render(float delta) {
		final World world = World.getInstance();

		update(delta);

		// Gdx.gl.glClearColor(0, 0, 0, 1);
		// Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (world.getAssetState() != AssetState.LOADED)
			return;

		SpriteBatch batch = ui.getBatch();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// WORLD CAMERA
		if (world.getInventory().isVisible()) {
			worldViewport.setScreenY(screenViewport.getScreenY() + (int) verbUI.getHeight());
			worldViewport.setScreenHeight(screenViewport.getScreenHeight() - (int) verbUI.getHeight());
			world.resize(world.getWidth(), world.getHeight() * (1 - UI_SCREEN_PERCENT));
		} else {
			worldViewport.setScreenY(screenViewport.getScreenY());
			worldViewport.setScreenHeight(screenViewport.getScreenHeight());
			world.resize(world.getWidth(), world.getHeight());
		}

		worldViewport.apply(true);

		world.draw();

		// DRAW DEBUG BBOXES
		if (EngineLogger.debugMode() && EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
			renderer.setProjectionMatrix(world.getSceneCamera().combined);
			world.getCurrentScene().drawBBoxLines(renderer);
			renderer.end();
		}

		// SCREEN CAMERA
		batch.setProjectionMatrix(worldViewport.getCamera().combined);
		batch.begin();

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			drawDebugText(batch);
		}

		Transition t = world.getTransition();

		t.draw(batch, worldViewport.getScreenWidth(), worldViewport.getScreenHeight());

		recorder.draw(batch);
		testerBot.draw(batch);

		if (drawHotspots)
			drawHotspots(batch);

		batch.end();

		worldViewportStage.draw();

		// STAGE CAMERA
		screenViewport.apply(true);
		stage.draw();
	}

	private void drawDebugText(SpriteBatch batch) {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(worldViewport, unprojectTmp);

		Color color;

		sbTmp.setLength(0);

		if (EngineLogger.lastError != null) {
			sbTmp.append(EngineLogger.lastError);

			color = Color.RED;
		} else {

			sbTmp.append("( ");
			sbTmp.append((int) unprojectTmp.x);
			sbTmp.append(", ");
			sbTmp.append((int) unprojectTmp.y);
			sbTmp.append(") FPS:");
			sbTmp.append(Gdx.graphics.getFramesPerSecond());
			// sbTmp.append(" Density:");
			// sbTmp.append(Gdx.graphics.getDensity());
			// sbTmp.append(" UI Multiplier:");
			// sbTmp.append(DPIUtils.getSizeMultiplier());
			sbTmp.append(" UI STATE: ");
			sbTmp.append(state.toString());

			if (w.getCurrentScene().getPlayer() != null) {
				sbTmp.append(" Depth Scl: ");
				sbTmp.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
			}

			color = Color.WHITE;
		}

		String strDebug = sbTmp.toString();

		textLayout.setText(ui.getSkin().getFont("debug"), strDebug, color, worldViewport.getScreenWidth(), Align.left,
				true);

		RectangleRenderer.draw(batch, 0, worldViewport.getScreenHeight() - textLayout.height - 10, textLayout.width,
				textLayout.height + 10, Color.BLACK);
		ui.getSkin().getFont("debug").draw(batch, textLayout, 0, worldViewport.getScreenHeight() - 5);

		// Draw actor states when debug
		if (EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {

			for (BaseActor a : w.getCurrentScene().getActors().values()) {
				Rectangle r = a.getBBox().getBoundingRectangle();
				sbTmp.setLength(0);
				sbTmp.append(a.getId());
				if (a instanceof InteractiveActor && ((InteractiveActor) a).getState() != null)
					sbTmp.append(".").append(((InteractiveActor) a).getState());

				unprojectTmp.set(r.getX(), r.getY(), 0);
				w.getSceneCamera().scene2screen(worldViewport, unprojectTmp);

				if (w.getInventory().isVisible()) {
					// unprojectTmp.y += verbUI.getHeight();
				}

				ui.getSkin().getFont("debug").draw(batch, sbTmp.toString(), unprojectTmp.x, unprojectTmp.y);
			}

		}
	}

	private void drawHotspots(SpriteBatch batch) {
		final World world = World.getInstance();
		for (BaseActor a : world.getCurrentScene().getActors().values()) {
			if (!(a instanceof InteractiveActor) || !a.isVisible() || a == world.getCurrentScene().getPlayer())
				continue;

			InteractiveActor ia = (InteractiveActor) a;

			if (!ia.hasInteraction())
				continue;

			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING HOTSPOT FOR: " + a.getId());
			}

			Rectangle r = a.getBBox().getBoundingRectangle();

			unprojectTmp.set(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 0);
			world.getSceneCamera().scene2screen(worldViewport, unprojectTmp);

			if (world.getInventory().isVisible()) {
				// unprojectTmp.y += verbUI.getHeight();
			}

			if (ia.getDesc() == null) {

				float size = DPIUtils.ICON_SIZE * DPIUtils.getSizeMultiplier();

				Drawable drawable = ((TextureRegionDrawable) getUI().getSkin().getDrawable("circle")).tint(Color.RED);

				drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size, size);
			} else {
				BitmapFont font = getUI().getSkin().getFont("desc");
				String desc = ia.getDesc();
				if (desc.charAt(0) == '@')
					desc = I18N.getString(desc.substring(1));

				textLayout.setText(font, desc);

				float textX = unprojectTmp.x - textLayout.width / 2;
				float textY = unprojectTmp.y + textLayout.height;

				RectangleRenderer.draw(batch, textX - 8, textY - textLayout.height - 8, textLayout.width + 16,
						textLayout.height + 16, Color.BLACK);
				font.draw(batch, textLayout, textX, textY);
			}
		}
	}

	@Override
	public void resize(int width, int height) {
		final World world = World.getInstance();

		if (!world.isDisposed()) {
			screenViewport.setWorldSize(world.getWidth(), world.getHeight());
			screenViewport.update(width, height, true);

			worldViewport.setScreenBounds(screenViewport.getScreenX(), screenViewport.getScreenY(),
					screenViewport.getScreenWidth(), screenViewport.getScreenHeight());

			world.resize(screenViewport.getWorldWidth(), screenViewport.getWorldHeight());
		}

		textManagerUI.resize();
		menuButton.resize();
		pointer.resize();

		verbUI.setSize(screenViewport.getScreenWidth(), screenViewport.getScreenHeight() * UI_SCREEN_PERCENT);
	}

	public void dispose() {
		renderer.dispose();
		stage.dispose();
		worldViewportStage.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
	}

	private void sceneClick(int button) {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(worldViewport, unprojectTmp);

		Scene s = w.getCurrentScene();

		if (currentActor != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(currentActor.toString());
			}

			actorClick(currentActor, button);
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

	public void actorClick(InteractiveActor a, int button) {
		runVerb(a, verbUI.getCurrentVerb(), verbUI.getTarget());
	}

	/**
	 * Run actor verb and handles recording
	 *
	 * @param a
	 * @param verb
	 * @param target
	 */
	public void runVerb(InteractiveActor a, String verb, String target) {

		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	@Override
	public void show() {
		retrieveAssets(ui.getUIAtlas());

		stage = new Stage(screenViewport);
		// stage.addActor(textManagerUI);
		stage.addActor(dialogUI);
		stage.addActor(menuButton);
		stage.addActor(verbUI);
		stage.addActor(pointer);

		worldViewportStage = new Stage(worldViewport);
		worldViewportStage.addActor(textManagerUI);

		final InputMultiplexer multiplexer = new InputMultiplexer();
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
		currentActor = null;
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
		return screenViewport;
	}

	public InteractiveActor getCurrentActor() {
		return currentActor;
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;

		recorder = ui.getRecorder();
		testerBot = ui.getTesterBot();

		textManagerUI = new TextManagerUI(ui.getSkin());
		menuButton = new MenuButton(ui);
		dialogUI = new DialogUI(ui);

		verbUI = new VerbUI(this);

		pointer = new Pointer(ui.getSkin());
	}
}
