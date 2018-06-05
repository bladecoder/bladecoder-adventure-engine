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
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.model.WorldListener;
import com.bladecoder.engine.ui.DialogUI;
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

	private Button menuButton;

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

	private final GlyphLayout textLayout = new GlyphLayout();

	private Pointer pointer;
	
	private boolean uiEnabled = true;

	private final GestureDetector inputProcessor = new GestureDetector(new GestureDetector.GestureAdapter() {
		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			return true;
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			EngineLogger.debug("Event TAP button: " + button);

			World w = ui.getWorld();

			if (w.isPaused() || recorder.isPlaying() || testerBot.isEnabled())
				return true;

			if (drawHotspots)
				drawHotspots = false;
			else {
				if (w.inCutMode() && !recorder.isRecording()) {
					w.getCurrentScene().getTextManager().next();
				} else if (!w.hasDialogOptions()) {
					sceneClick(button);
				}
			}

			return true;
		}

		@Override
		public boolean longPress(float x, float y) {
			EngineLogger.debug("Event LONG PRESS");

			if (uiEnabled && !ui.getWorld().hasDialogOptions()) {
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
					ui.getWorld().saveGameState();
				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
				break;
			case 'l':
				try {
					ui.getWorld().loadGameState();
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
				if (ui.getWorld().isPaused()) {
					ui.getWorld().resume();
				} else {
					ui.getWorld().pause();
				}
				break;
			case ' ':
				if (uiEnabled && !ui.getWorld().hasDialogOptions()) {
					drawHotspots = true;
				}
				break;
			}

			// FIXME: This is returning false even in the cases where we
			// actually process the character
			return false;
		}
	};
	
	private final WorldListener worldListener = new WorldListener() {
		@Override
		public void text(Text t) {
			textManagerUI.setText(t);
		}

		@Override
		public void dialogOptions() {
			updateUI();
		}

		@Override
		public void cutMode(boolean value) {
			updateUI();
		}

		@Override
		public void inventoryEnabled(boolean value) {
			if (value)
				verbUI.show();
			else
				verbUI.hide();
		}

		@Override
		public void pause(boolean value) {
			updateUI();
		}
	};

	public RetroSceneScreen() {
		screenViewport = new SceneFitViewport();

		worldViewport = new Viewport() {
			// This is the World Viewport. It is like a ScreenViewport but the
			// camera is the same that the screenViewport;
			@Override
			public void apply(boolean centerCamera) {
				HdpiUtils.glViewport(getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
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
	
	private void updateUI() {
		World w = ui.getWorld();

		if (w.isPaused() || w.inCutMode() || testerBot.isEnabled() || recorder.isPlaying()) {
			// DISABLE UI
			dialogUI.setVisible(false);
			verbUI.hide();
			pointer.hide();
			uiEnabled = false;
		} else {
			if (ui.getWorld().hasDialogOptions()) {
				dialogUI.setVisible(true);
				verbUI.hide();
			} else {
				dialogUI.setVisible(false);
				
				if (w.getInventory().isVisible())
					verbUI.show();
				else
					verbUI.hide();
			}
			
			pointer.show();
			uiEnabled = true;
		}
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
		final World world = ui.getWorld();

		currentActor = null;

		if (!world.isDisposed()) {
			world.update(delta * speed);
		}

		AssetState assetState = world.getAssetState();

		if (assetState != AssetState.LOADED) {
			ui.setCurrentScreen(Screens.LOADING_SCREEN);
			return;
		}

		stage.act(delta);
		worldViewportStage.act(delta);

		if (world.isPaused())
			return;

		recorder.update(delta * speed);
		testerBot.update(delta * speed);

		if (uiEnabled && !world.hasDialogOptions()) {

			final float tolerance;

			if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				tolerance = DPIUtils.getTouchMinSize();
			else
				tolerance = 0;

			currentActor = world.getInteractiveActorAtInput(worldViewport, tolerance);

			verbUI.setCurrentActor(currentActor);
		}
	}

	@Override
	public void render(float delta) {
		final World world = ui.getWorld();

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
		World w = ui.getWorld();

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
		final World world = ui.getWorld();
		for (BaseActor a : world.getCurrentScene().getActors().values()) {
			if (!(a instanceof InteractiveActor) || !a.isVisible() || a == world.getCurrentScene().getPlayer())
				continue;

			InteractiveActor ia = (InteractiveActor) a;

			if (!ia.canInteract())
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
				if (desc.charAt(0) == I18N.PREFIX)
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
		final World world = ui.getWorld();

		if (!world.isDisposed()) {
			screenViewport.setWorldSize(world.getWidth(), world.getHeight());
			screenViewport.update(width, height, true);

			worldViewport.setScreenBounds(screenViewport.getScreenX(), screenViewport.getScreenY(),
					screenViewport.getScreenWidth(), screenViewport.getScreenHeight());

			world.resize(screenViewport.getWorldWidth(), screenViewport.getWorldHeight());
		}

		pointer.resize();

		verbUI.setSize(screenViewport.getScreenWidth(), screenViewport.getScreenHeight() * UI_SCREEN_PERCENT);
		
		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();
		
		menuButton.setSize(size, size);
		menuButton.setPosition(
				stage.getViewport().getScreenWidth() - menuButton.getWidth()
						- margin, stage.getViewport()
						.getScreenHeight()
						- menuButton.getHeight()
						- margin);
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
		World w = ui.getWorld();

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

				s.getPlayer().goTo(pos, null, false);
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
		
		menuButton.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});

		worldViewportStage = new Stage(worldViewport);
		worldViewportStage.addActor(textManagerUI);

		final InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);

		if (ui.getWorld().isDisposed()) {
			try {
				ui.getWorld().load();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);

				dispose();
				Gdx.app.exit();
			}
		}

		ui.getWorld().setListener(worldListener);
		ui.getWorld().resume();
		
		textManagerUI.setText(ui.getWorld().getCurrentScene().getTextManager().getCurrentText());
		
		updateUI();
	}

	@Override
	public void hide() {
		ui.getWorld().pause();
		currentActor = null;
		dispose();
	}

	@Override
	public void pause() {
		ui.getWorld().pause();
	}

	@Override
	public void resume() {
		ui.getWorld().resume();
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

		textManagerUI = new TextManagerUI(ui);
		menuButton = new Button(ui.getSkin(), "menu");
		dialogUI = new DialogUI(ui);

		verbUI = new VerbUI(this);

		pointer = new Pointer(ui.getSkin());
	}
}
