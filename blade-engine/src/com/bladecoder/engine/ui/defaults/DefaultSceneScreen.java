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
package com.bladecoder.engine.ui.defaults;

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
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.ui.DialogUI;
import com.bladecoder.engine.ui.InventoryButton;
import com.bladecoder.engine.ui.InventoryUI;
import com.bladecoder.engine.ui.InventoryUI.InventoryPos;
import com.bladecoder.engine.ui.MenuButton;
import com.bladecoder.engine.ui.PieMenu;
import com.bladecoder.engine.ui.Recorder;
import com.bladecoder.engine.ui.SceneExtendViewport;
import com.bladecoder.engine.ui.SceneFitViewport;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.TesterBot;
import com.bladecoder.engine.ui.TextManagerUI;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class DefaultSceneScreen implements SceneScreen {
	private UI ui;

	private Stage stage;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private InventoryButton inventoryButton;
	private MenuButton menuButton;

	public static enum UIModes {
		TWO_BUTTONS, PIE, SINGLE_CLICK
	};

	private UIModes uiMode = UIModes.TWO_BUTTONS;

	private Recorder recorder;
	private TesterBot testerBot;

	private final Viewport viewport;

	private final Vector3 unprojectTmp = new Vector3();
	private final Vector2 unproject2Tmp = new Vector2();

	private final StringBuilder sbTmp = new StringBuilder();

	// BaseActor under the cursor
	private InteractiveActor currentActor = null;

	private boolean drawHotspots = false;
	private final boolean showDesc;

	private float speed = 1.0f;

	private ScenePointer pointer;

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

			if (pie.isVisible())
				pie.hide();

			if (drawHotspots)
				drawHotspots = false;
			else {
				getInputUnProject(unprojectTmp);

				if (w.inCutMode() && !recorder.isRecording()) {
					w.getTextManager().next();
				} else if (state == UIStates.INVENTORY_MODE) {
					inventoryUI.hide();
				} else if (state == UIStates.SCENE_MODE) {
					if (button == 2) { // Show inventory with the middle
						// button
						if (!inventoryUI.isVisible())
							inventoryUI.show();
					} else {
						sceneClick(button);
					}
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
			tap(x, y, 1, button);

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
				try {
					World.getInstance().saveGameState();
				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
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

		@Override
		public boolean scrolled(int amount) {
			if (state == UIStates.SCENE_MODE || state == UIStates.INVENTORY_MODE) {

				boolean fromDown = (inventoryUI.getInventoryPos() == InventoryPos.CENTER
						|| inventoryUI.getInventoryPos() == InventoryPos.DOWN);

				if ((amount > 0 && fromDown || amount < 0 && !fromDown) && inventoryUI.isVisible())
					inventoryUI.hide();
				else if ((amount > 0 && !fromDown || amount < 0 && fromDown) && !inventoryUI.isVisible())
					inventoryUI.show();
			}

			return true;
		}
	};

	public DefaultSceneScreen() {
		viewport = Config.getProperty(Config.EXTEND_VIEWPORT_PROP, true) ? new SceneExtendViewport()
				: new SceneFitViewport();
		showDesc = Config.getProperty(Config.SHOW_DESC_PROP, true);
	}

	public UI getUI() {
		return ui;
	}

	private void setUIState(UIStates s) {
		if (state == s)
			return;

		if (uiMode == UIModes.PIE && pie.isVisible())
			pie.hide();

		switch (s) {
		case PAUSE_MODE:
		case PLAY_MODE:
		case TESTER_BOT_MODE:
		case CUT_MODE:
			inventoryUI.hide();
			inventoryButton.setVisible(false);
			dialogUI.setVisible(false);

			inventoryUI.cancelDragging();
			pointer.reset();
			break;
		case DIALOG_MODE:
			inventoryUI.hide();
			inventoryButton.setVisible(false);
			dialogUI.show();

			inventoryUI.cancelDragging();
			break;
		case INVENTORY_MODE:
			inventoryUI.show();
			inventoryButton.setVisible(true);
			dialogUI.setVisible(false);
			break;
		case SCENE_MODE:
			inventoryUI.hide();
			dialogUI.hide();
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

			// if the game ends returns
			if (world.isDisposed())
				return;
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
			if (!inventoryUI.isVisible())
				setUIState(UIStates.SCENE_MODE);
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
			else if (inventoryUI.isVisible())
				setUIState(UIStates.INVENTORY_MODE);
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
			unproject2Tmp.set(Gdx.input.getX(), Gdx.input.getY());
			inventoryUI.screenToLocalCoordinates(unproject2Tmp);
			currentActor = inventoryUI.getItemAt(unproject2Tmp.x, unproject2Tmp.y);
			break;
		case SCENE_MODE:
			world.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

			final Scene currentScene = world.getCurrentScene();

			final float tolerance;
			if (inventoryUI.isDragging())
				tolerance = DPIUtils.getTouchMinSize() * 2f;
			else if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				tolerance = DPIUtils.getTouchMinSize();
			else
				tolerance = 0;

			currentActor = currentScene.getInteractiveActorAt(unprojectTmp.x, unprojectTmp.y, tolerance);

			inventoryButton.setVisible(world.getInventory().isVisible());
			break;
		default:
		}

		if (!pie.isVisible()) {
			if (currentActor != null) {
				if (showDesc)
					pointer.setDesc(currentActor.getDesc());

				if (currentActor.getVerb("leave") != null) {
					pointer.setLeaveIcon(calcLeaveArrowRotation(currentActor));
				} else
					pointer.setHotspotIcon();
			} else {
				pointer.setDefaultIcon();
			}
		}
	}

	/**
	 * Calcs the rotation based in the actor screen position
	 */
	private float calcLeaveArrowRotation(InteractiveActor actor) {
		Verb verb = actor.getVerb("leave");

		if (verb == null || verb.getIcon() == null) {

			actor.getBBox().getBoundingRectangle().getCenter(unproject2Tmp);

			if (unproject2Tmp.x < stage.getViewport().getWorldWidth() / 3f) {
				return 180; // LEFT
			}

			if (unproject2Tmp.x > stage.getViewport().getWorldWidth() / 3f * 2f) {
				return 0; // RIGHT
			}

			if (unproject2Tmp.y < stage.getViewport().getWorldHeight() / 5f) {
				return -90; // DOWN
			}

			return 90; // UP
		} else {
			String dir = verb.getIcon();
			
			if (dir.equals("left")) {
				return 180; // LEFT
			}

			if (dir.equals("right")) {
				return 0; // RIGHT
			}

			if (dir.equals("down")) {
				return -90; // DOWN
			}

			return 90; // UP
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
		world.draw();

		// DRAW DEBUG BBOXES
		if (EngineLogger.debugMode() && EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
			renderer.setProjectionMatrix(world.getSceneCamera().combined);
			world.getCurrentScene().drawBBoxLines(renderer);
			renderer.end();
		}

		// STAGE
		stage.draw();

		// SCREEN CAMERA
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			drawDebugText(batch);
		}

		if (!world.inCutMode() && !recorder.isPlaying() && !testerBot.isEnabled()) {
			pointer.draw(batch, viewport);
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

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

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

//			if (w.getCurrentScene().getPlayer() != null) {
//				sbTmp.append(" Depth Scl: ");
//				sbTmp.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
//			}

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

				if (a instanceof AnchorActor)
					continue;

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

			if (!showDesc || ia.getDesc() == null) {

				float size = DPIUtils.ICON_SIZE * DPIUtils.getSizeMultiplier();

				if (ia.getVerb("leave") != null) {
					TextureRegionDrawable drawable = (TextureRegionDrawable) getUI().getSkin().getDrawable("leave");

					// drawable.draw(batch, unprojectTmp.x - size / 2,
					// unprojectTmp.y - size / 2, size, size);

					drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size / 2, size / 2, size,
							size, 1.0f, 1.0f, calcLeaveArrowRotation(ia));
				} else {
					Drawable drawable = ((TextureRegionDrawable) getUI().getSkin().getDrawable("circle"))
							.tint(Color.RED);

					drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size, size);
				}
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
		final World world = World.getInstance();
		if (!world.isDisposed()) {
			viewport.setWorldSize(world.getWidth(), world.getHeight());
			viewport.update(width, height, true);
			world.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
		} else {
			viewport.setWorldSize(width, height);
			viewport.update(width, height, true);
		}

		pie.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
		inventoryUI.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
		textManagerUI.resize();
		inventoryButton.resize();
		menuButton.resize();
		pointer.resize(width, height);
	}

	public void dispose() {
		renderer.dispose();
		stage.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
		inventoryUI.retrieveAssets(atlas);
	}

	private void sceneClick(int button) {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

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
		final boolean lookat = button == 1;

		if (a.getVerb(Verb.LEAVE_VERB) != null) {
			runVerb(a, Verb.LEAVE_VERB, null);
		} else if (uiMode == UIModes.TWO_BUTTONS) {
			String verb = Verb.LOOKAT_VERB;

			if (!lookat) {
				verb = a.getVerb(Verb.TALKTO_VERB) != null ? Verb.TALKTO_VERB : Verb.ACTION_VERB;
			}

			runVerb(a, verb, null);
		} else if (uiMode == UIModes.PIE) {
			getInputUnProject(unprojectTmp);
			pie.show(a, unprojectTmp.x, unprojectTmp.y);
			pointer.reset();
		} else {
			// SINGLE CLICK UI
			// Preference TALK_TO, PICKUP, LOOK_AT
			String verb = Verb.TALKTO_VERB;

			if (a.getVerb(verb) == null)
				verb = Verb.ACTION_VERB;

			if (a.getVerb(verb) == null)
				verb = Verb.LOOKAT_VERB;

			runVerb(a, verb, null);
		}
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
		if (inventoryUI.isVisible())
			inventoryUI.hide();

		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	private void showMenu() {
		ui.setCurrentScreen(Screens.MENU_SCREEN);
	}

	private void resetUI() {
		if (pie.isVisible()) {
			pie.hide();
		}

		pointer.reset();

		currentActor = null;
	}

	public InventoryUI getInventoryUI() {
		return inventoryUI;
	}

	@Override
	public void show() {
		retrieveAssets(ui.getUIAtlas());

		stage = new Stage(viewport);
		stage.addActor(textManagerUI);
		stage.addActor(dialogUI);
		stage.addActor(inventoryButton);
		stage.addActor(menuButton);
		stage.addActor(inventoryUI);
		stage.addActor(pie);

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
		testerBot = ui.getTesterBot();

		pie = new PieMenu(this);
		textManagerUI = new TextManagerUI(ui.getSkin());
		menuButton = new MenuButton(ui);
		dialogUI = new DialogUI(ui);
		pointer = new ScenePointer(ui.getSkin());
		inventoryUI = new InventoryUI(this, pointer);
		inventoryButton = new InventoryButton(ui.getSkin(), inventoryUI);

		uiMode = UIModes.valueOf(Config.getProperty(Config.UI_MODE, "TWO_BUTTONS").toUpperCase());

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen) && uiMode == UIModes.TWO_BUTTONS) {
			uiMode = UIModes.PIE;
		}

		pie.setVisible(false);
	}
}
