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
import com.bladecoder.engine.ui.BladeScreen;
import com.bladecoder.engine.ui.DialogUI;
import com.bladecoder.engine.ui.MenuButton;
import com.bladecoder.engine.ui.Recorder;
import com.bladecoder.engine.ui.SceneExtendViewport;
import com.bladecoder.engine.ui.SceneFitViewport;
import com.bladecoder.engine.ui.TesterBot;
import com.bladecoder.engine.ui.TextManagerUI;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class SceneScreen implements BladeScreen {
	private UI ui;

	private Stage stage;

	private VerbUI verbUI;
//	private InventoryUI inventoryUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private MenuButton menuButton;

	private Recorder recorder;
	private TesterBot testerBot;

	private final Viewport viewport;

	private final Vector3 unprojectTmp = new Vector3();
	private final Vector2 unproject2Tmp = new Vector2();

	private final StringBuilder sbTmp = new StringBuilder();

	// BaseActor under the cursor
	private InteractiveActor currentActor = null;

	private boolean drawHotspots = false;

	private float speed = 1.0f;

	private static enum UIStates {
		SCENE_MODE, CUT_MODE, PLAY_MODE, PAUSE_MODE, INVENTORY_MODE, DIALOG_MODE, TESTER_BOT_MODE
	};

	private UIStates state = UIStates.SCENE_MODE;

	private final GlyphLayout textLayout = new GlyphLayout();

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
				getInputUnProject(unprojectTmp);

				if (w.inCutMode() && !recorder.isRecording()) {
					w.getTextManager().next();
				} else if (state == UIStates.INVENTORY_MODE) {
					// inventoryUI.hide();
				} else if (state == UIStates.SCENE_MODE) {
					sceneClick();
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
				showMenu();
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
				World.getInstance().saveGameState();
				break;
			case 'r':
				World.getInstance().newGame();
				break;
			case 'l':
				World.getInstance().loadGameState();
				break;
			case 't':
				testerBot.setEnabled(!testerBot.isEnabled());
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

	public SceneScreen() {
		viewport = Config.getProperty(Config.EXTEND_VIEWPORT_PROP, true) ? new SceneExtendViewport()
				: new SceneFitViewport();
	}

	public UI getUI() {
		return ui;
	}

	public Recorder getRecorder() {
		return recorder;
	}

	public TesterBot getTesterBot() {
		return testerBot;
	}

	private void setUIState(UIStates s) {
		if (state == s)
			return;

		switch (s) {
		case PAUSE_MODE:
		case PLAY_MODE:
		case TESTER_BOT_MODE:
		case CUT_MODE:
			// inventoryUI.hide();
			// inventoryButton.setVisible(false);
			// dialogUI.setVisible(false);
			//
			// inventoryUI.cancelDragging();
			ui.getPointer().reset();
			break;
		case DIALOG_MODE:
			// inventoryUI.hide();
			// inventoryButton.setVisible(false);
			// dialogUI.show();
			//
			// inventoryUI.cancelDragging();
			break;
		case INVENTORY_MODE:
			// inventoryUI.show();
			// inventoryButton.setVisible(true);
			// dialogUI.setVisible(false);
			break;
		case SCENE_MODE:
			// inventoryUI.hide();
			// dialogUI.hide();
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
		case INVENTORY_MODE:
			// if (!inventoryUI.isVisible())
			// setUIState(UIStates.SCENE_MODE);
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
			// else if (inventoryUI.isVisible())
			// setUIState(UIStates.INVENTORY_MODE);
			else if (world.getCurrentDialog() != null)
				setUIState(UIStates.DIALOG_MODE);
			break;
		}

		stage.act(delta);

		if (state == UIStates.PAUSE_MODE)
			return;

		recorder.update(delta * speed);
		testerBot.update(delta * speed);

		switch (state) {
		case INVENTORY_MODE:
			// unproject2Tmp.set(Gdx.input.getX(), Gdx.input.getY());
			// inventoryUI.screenToLocalCoordinates(unproject2Tmp);
			// currentActor = inventoryUI.getItemAt(unproject2Tmp.x,
			// unproject2Tmp.y);
			break;
		case SCENE_MODE:
			world.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

			final Scene currentScene = world.getCurrentScene();

			final float tolerance;

			if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				tolerance = DPIUtils.getTouchMinSize();
			else
				tolerance = 0;

			currentActor = currentScene.getInteractiveActorAt(unprojectTmp.x, unprojectTmp.y, tolerance);

			break;
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
		if(world.getInventory().isVisible())
			Gdx.gl.glViewport(viewport.getScreenX(), viewport.getScreenY() + (int)verbUI.getHeight(), viewport.getScreenWidth(), viewport.getScreenHeight());
		else
			Gdx.gl.glViewport(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
		
		world.draw();

		// DRAW DEBUG BBOXES
		if (EngineLogger.debugMode() && EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
			renderer.setProjectionMatrix(world.getSceneCamera().combined);
			world.getCurrentScene().drawBBoxLines(renderer);
			renderer.end();
		}

		// STAGE CAMERA
		Gdx.gl.glViewport(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
		stage.draw();

		// SCREEN CAMERA
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			drawDebugText(batch);
		}

		if (!world.inCutMode() && !recorder.isPlaying() && !testerBot.isEnabled()) {
			ui.getPointer().draw(batch, viewport);
		}

		Transition t = world.getTransition();

		t.draw(batch, viewport.getScreenWidth(), viewport.getScreenHeight());

		recorder.draw(batch);
		testerBot.draw(batch);

		if (drawHotspots)
			drawHotspots(batch);

		batch.end();
	}

	private void drawDebugText(SpriteBatch batch) {
		World w = World.getInstance();

		getWorldInputUnProject(viewport, unprojectTmp);

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

		textLayout.setText(ui.getSkin().getFont("debug"), strDebug, color, viewport.getScreenWidth(), Align.left, true);
		RectangleRenderer.draw(batch, 0, viewport.getScreenHeight() - textLayout.height - 10, textLayout.width,
				textLayout.height + 10, Color.BLACK);
		ui.getSkin().getFont("debug").draw(batch, textLayout, 0, viewport.getScreenHeight() - 5);

		// Draw actor states when debug
		if (EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {

			for (BaseActor a : w.getCurrentScene().getActors().values()) {
				Rectangle r = a.getBBox().getBoundingRectangle();
				sbTmp.setLength(0);
				sbTmp.append(a.getId());
				if (a instanceof InteractiveActor && ((InteractiveActor) a).getState() != null)
					sbTmp.append(".").append(((InteractiveActor) a).getState());

				unprojectTmp.set(r.getX(), r.getY(), 0);
				w.getSceneCamera().scene2screen(viewport, unprojectTmp);
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
			world.getSceneCamera().scene2screen(viewport, unprojectTmp);

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
			viewport.setWorldSize(world.getWidth(), world.getHeight());
			viewport.update(width, height, true);
			world.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
		} else {
			viewport.setWorldSize(width, height);
			viewport.update(width, height, true);
		}

		
//		inventoryUI.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
//		textManagerUI.resize(viewport.getScreenWidth(), viewport.getScreenHeight());

		menuButton.resize();
		
		float factor = 1 - 144.0f/200.0f;
		
		verbUI.setSize(viewport.getScreenWidth(), viewport.getScreenHeight() * factor);
	}

	public void dispose() {
		renderer.dispose();
		stage.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
//		inventoryUI.retrieveAssets(atlas);
	}

	private void sceneClick() {
		World w = World.getInstance();

		getWorldInputUnProject(viewport, unprojectTmp);

		Scene s = w.getCurrentScene();

		if (currentActor != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(currentActor.toString());
			}

			actorClick(currentActor);
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
	
	private void getWorldInputUnProject(Viewport viewport, Vector3 out) {
		World w = World.getInstance();

		out.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		
		int h;

		if(!w.getInventory().isVisible())
			h = viewport.getScreenY();
		else
			h = viewport.getScreenY() + (int)verbUI.getHeight();

				
		w.getSceneCamera().unproject(out, viewport.getScreenX(), h, 
				viewport.getScreenWidth(), viewport.getScreenHeight());

		if (out.x >= w.getSceneCamera().getScrollingWidth())
			out.x =  w.getSceneCamera().getScrollingWidth() - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >=  w.getSceneCamera().getScrollingHeight())
			out.y = w.getSceneCamera().getScrollingHeight() - 1;
		else if (out.y < 0)
			out.y = 0;
	}

	public void actorClick(InteractiveActor a) {		
		runVerb(a, verbUI.getCurrentVerb(), verbUI.getTarget());
	}

	private void getInputUnProject(Vector3 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY(), 0);

		viewport.unproject(out);
	}

	/**
	 * Run actor verb and handles recording
	 *
	 * @param a
	 * @param verb
	 * @param target
	 */
	public void runVerb(InteractiveActor a, String verb, String target) {
		// if (inventoryUI.isVisible())
		// inventoryUI.hide();

		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	public void showMenu() {
		ui.setCurrentScreen(Screens.MENU_SCREEN);
	}

	private void resetUI() {

		ui.getPointer().reset();

		currentActor = null;
	}

//	public InventoryUI getInventoryUI() {
//		return inventoryUI;
//	}

	@Override
	public void show() {
		retrieveAssets(ui.getUIAtlas());

		stage = new Stage(viewport);
		// stage.addActor(textManagerUI);
		// stage.addActor(dialogUI);
		stage.addActor(menuButton);
		// stage.addActor(inventoryUI);
		stage.addActor(verbUI);

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

	public InteractiveActor getCurrentActor() {
		return currentActor;
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
		
		recorder = ui.getRecorder();
		testerBot = ui.getTesterbot();

		// textManagerUI = new TextManagerUI(this);
		// inventoryUI = new InventoryUI(this);
		menuButton = new MenuButton(ui.getSkin(), ui);
		// dialogUI = new DialogUI(this);

		verbUI = new VerbUI(this);
	}
}
