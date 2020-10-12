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

import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.model.WorldListener;
import com.bladecoder.engine.ui.AnimButton;
import com.bladecoder.engine.ui.AnimationDrawable;
import com.bladecoder.engine.ui.DebugDrawer;
import com.bladecoder.engine.ui.DialogUI;
import com.bladecoder.engine.ui.HotspotsDrawer;
import com.bladecoder.engine.ui.ITextManagerUI;
import com.bladecoder.engine.ui.InventoryButton;
import com.bladecoder.engine.ui.InventoryUI;
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
import com.bladecoder.engine.util.UIUtils;

public class DefaultSceneScreen implements SceneScreen {
	private final static float LOADING_WAIT_TIME_MS = 400f;

	private UI ui;

	private Stage stage;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private Actor dialogUI;
	private Actor textManagerUI;
	private ShapeRenderer renderer;

	private InventoryButton inventoryButton;
	private Button menuButton;

	private DebugDrawer debugDrawer;
	private HotspotsDrawer hotspotsDrawer;

	public static enum UIModes {
		TWO_BUTTONS, PIE, SINGLE_CLICK
	};

	private UIModes uiMode = UIModes.TWO_BUTTONS;

	private Recorder recorder;
	private TesterBot testerBot;

	private final Viewport viewport;

	private final Vector3 unprojectTmp = new Vector3();
	private final Vector2 unproject2Tmp = new Vector2();

	// Actor under the cursor
	private InteractiveActor currentActor = null;

	// Called by the input handlers to show/hide the hotspots.
	private boolean drawHotspots = false;
	// Configuration to enable/disable the 'drawHotspot' feature.
	private boolean showHotspotsFeature = true;

	private final boolean showDesc;
	private final boolean fastLeave;

	private float speed = 1.0f;

	private ScenePointer pointer;

	private boolean uiEnabled = true;

	private final GestureDetector inputProcessor = new SceneGestureDetector(this);

	private final WorldListener worldListener = new SceneWorldListener(this);

	public DefaultSceneScreen() {
		viewport = Config.getInstance().getProperty(Config.EXTEND_VIEWPORT_PROP, false) ? new SceneExtendViewport()
				: new SceneFitViewport();
		showDesc = Config.getInstance().getProperty(Config.SHOW_DESC_PROP, true);
		fastLeave = Config.getInstance().getProperty(Config.FAST_LEAVE, false);
		showHotspotsFeature = Config.getInstance().getProperty(Config.SHOW_HOTSPOTS, true);
	}

	@Override
	public UI getUI() {
		return ui;
	}

	@Override
	public World getWorld() {
		return ui.getWorld();
	}

	public boolean isUiEnabled() {
		return uiEnabled;
	}

	public PieMenu getPie() {
		return pie;
	}

	public InventoryButton getInventoryButton() {
		return inventoryButton;
	}

	public boolean getDrawHotspots() {
		return drawHotspots;
	}

	public void setDrawHotspots(boolean drawHotspots) {
		this.drawHotspots = drawHotspots && showHotspotsFeature;
	}

	public void setShowHotspotsFeature(boolean v) {
		showHotspotsFeature = v;
	}

	public UIModes getUIMode() {
		return uiMode;
	}

	void updateUI() {
		World w = getWorld();

		if (w.getAssetState() == null || w.getAssetState() == AssetState.LOAD_ASSETS
				|| w.getAssetState() == AssetState.LOAD_ASSETS_AND_INIT_SCENE)
			return;

		if (uiMode == UIModes.PIE && pie.isVisible())
			pie.hide();

		currentActor = null;
		pointer.reset();

		if (w.isPaused() || w.inCutMode() || testerBot.isEnabled() || recorder.isPlaying()) {
			// DISABLE UI
			inventoryUI.hide();
			inventoryButton.setVisible(false);
			dialogUI.setVisible(false);

			inventoryUI.cancelDragging();
			uiEnabled = false;

			EngineLogger.debug("Updating UI: DISABLED.");
		} else {
			if (getWorld().hasDialogOptions()) {
				inventoryUI.hide();
				inventoryButton.setVisible(false);
				dialogUI.setVisible(true);
				inventoryUI.cancelDragging();
				EngineLogger.debug("Updating UI: DIALOG OPTIONS.");
			} else {
				inventoryUI.hide();
				dialogUI.setVisible(false);
				inventoryButton.setVisible(w.getInventory().isVisible());
				EngineLogger.debug("Updating UI: ENABLED.");
			}

			uiEnabled = true;
		}
	}

	/**
	 * Sets the game speed. Can be used to fastfordward
	 *
	 * @param s The multiplier speed. ej. 2.0
	 */
	@Override
	public void setSpeed(float s) {
		speed = s;
	}

	@Override
	public float getSpeed() {
		return speed;
	}

	private void update(float delta) {
		final World world = getWorld();
		float deltaScaled = delta * speed;

		if (!world.isDisposed()) {
			world.update(deltaScaled);

			// if the game ends returns
			if (world.isDisposed())
				return;
		}

		if (world.getAssetState() != AssetState.LOADED) {

			// TRY TO LOAD THE ASSETS FOR LOADING_WAIT_TIME_MS TO AVOID
			// BLACK/LOADING SCREEN
			long t0 = System.currentTimeMillis();
			long t = t0;
			while (EngineAssetManager.getInstance().isLoading() && t - t0 < LOADING_WAIT_TIME_MS) {
				t = System.currentTimeMillis();
			}

			if (!EngineAssetManager.getInstance().isFinished()) {
				// Sets loading screen if resources are not loaded yet
				ui.setCurrentScreen(Screens.LOADING_SCREEN);
			} else {
				updateUI();

				world.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

				// update() to retrieve assets and exec init verb
				world.update(0);
			}

			return;
		}

		stage.act(delta);
		pointer.update(delta);

		if (drawHotspots) {
			Drawable hotspotDrawable = getUI().getSkin().getDrawable(Verb.LEAVE_VERB);
			Drawable leaveDrawable = getUI().getSkin().getDrawable("hotspot");

			if (hotspotDrawable != null && hotspotDrawable instanceof AnimationDrawable)
				((AnimationDrawable) hotspotDrawable).act(delta);

			if (leaveDrawable != null && leaveDrawable instanceof AnimationDrawable)
				((AnimationDrawable) leaveDrawable).act(delta);
		}

		if (world.isPaused())
			return;

		recorder.update(deltaScaled);
		testerBot.update(deltaScaled);

		InteractiveActor actorUnderCursor = null;

		if (!uiEnabled || world.hasDialogOptions()) {
			return;
		}

		if (inventoryUI.isVisible()) {
			unproject2Tmp.set(Gdx.input.getX(), Gdx.input.getY());
			inventoryUI.screenToLocalCoordinates(unproject2Tmp);
			actorUnderCursor = inventoryUI.getItemAt(unproject2Tmp.x, unproject2Tmp.y);
		} else {
			actorUnderCursor = getActorUnderCursor();
		}

		// UPDATE POINTER
		if (!pie.isVisible() && actorUnderCursor != currentActor) {
			currentActor = actorUnderCursor;

			updatePointer();
		} else if (pie.isVisible()) {
			currentActor = actorUnderCursor;
		}
	}

	private void updatePointer() {
		if (currentActor == null) {
			pointer.setDefaultIcon();
			return;
		}

		if (showDesc)
			pointer.setDesc(currentActor.getDesc());

		Verb leaveVerb = currentActor.getVerb(Verb.LEAVE_VERB);

		Drawable r = null;

		if (leaveVerb != null) {
			if ((r = getDrawable(leaveVerb.getIcon())) != null) {
				pointer.setIcon(r);

			} else {
				pointer.setLeaveIcon(UIUtils.calcLeaveArrowRotation(viewport, currentActor));
			}
		} else {
			Verb actionVerb = currentActor.getVerb(Verb.ACTION_VERB);

			if (actionVerb != null && (r = getDrawable(actionVerb.getIcon())) != null) {
				pointer.setIcon(r);
			} else {
				pointer.setHotspotIcon();
			}
		}
	}

	private Drawable getDrawable(String name) {
		if (name == null)
			return null;

		try {
			return getUI().getSkin().getDrawable(name);
		} catch (GdxRuntimeException e) {
			return null;
		}
	}

	private InteractiveActor getActorUnderCursor() {
		final float tolerance;

		if (inventoryUI.isDragging())
			tolerance = DPIUtils.getTouchMinSize();
		else if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
			tolerance = DPIUtils.getTouchMinSize() / 2;
		else
			tolerance = 0;

		return getWorld().getInteractiveActorAtInput(viewport, tolerance);
	}

	@Override
	public void render(float delta) {
		final World world = getWorld();

		update(delta);

		if (world.getAssetState() != AssetState.LOADED) {
			return;
		}

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

		if (!world.inCutMode() && !recorder.isPlaying() && !testerBot.isEnabled()) {
			pointer.draw(batch, viewport);
		}

		Transition t = world.getTransition();

		t.draw(batch, viewport.getScreenWidth(), viewport.getScreenHeight());

		recorder.draw(batch);
		testerBot.draw(batch);

		if (drawHotspots)
			hotspotsDrawer.draw(batch, showDesc);

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			debugDrawer.draw(batch);
		}

		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		final World world = getWorld();
		if (!world.isDisposed()) {
			viewport.setWorldSize(world.getWidth(), world.getHeight());
			viewport.update(width, height, true);
			world.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
		} else {
			viewport.setWorldSize(width, height);
			viewport.update(width, height, true);
		}

		updateUI();

		pie.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
		inventoryUI.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
		inventoryButton.resize(width, height);
		pointer.resize(width, height);

		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();

		menuButton.setSize(size, size);
		menuButton.setPosition(stage.getViewport().getScreenWidth() - menuButton.getWidth() - margin,
				stage.getViewport().getScreenHeight() - menuButton.getHeight() - margin);
	}

	@Override
	public void dispose() {
		renderer.dispose();
		stage.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
		inventoryUI.retrieveAssets(atlas);
	}

	void sceneClick(int button, int count) {
		World w = getWorld();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

		Scene s = w.getCurrentScene();
		CharacterActor player = s.getPlayer();

		currentActor = getActorUnderCursor();

		if (currentActor != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(currentActor.toString());
			}

			// DOUBLE CLICK: Fastwalk when leaving scene.
			if (count > 1) {
				if (count == 2 && fastLeave && !recorder.isRecording() && player != null
						&& currentActor.getVerb(Verb.LEAVE_VERB) != null) {

					player.fastWalk();
				}

				return;
			}

			actorClick(currentActor, button);
		} else if (player != null) {
			if (count > 1)
				return;

			if (s.getPlayer().getVerb(Verb.GOTO_VERB) != null) {
				runVerb(s.getPlayer(), Verb.GOTO_VERB, null);
			} else {
				Vector2 pos = new Vector2(unprojectTmp.x, unprojectTmp.y);

				if (recorder.isRecording()) {
					recorder.add(pos);
				}

				player.goTo(pos, null, false);
			}
		}
	}

	@Override
	public void actorClick(InteractiveActor a, int button) {
		final boolean lookatButton = button == 1;

		if (a.getVerb(Verb.LEAVE_VERB) != null) {
			runVerb(a, Verb.LEAVE_VERB, null);
		} else if (a.getVerb(Verb.ACTION_VERB) != null) {
			runVerb(a, Verb.ACTION_VERB, null);
		} else if (uiMode == UIModes.SINGLE_CLICK) {
			// SINGLE CLICK UI
			// Preference TALKTO, ACTION, PICKUP, LOOKAT
			String verb = Verb.TALKTO_VERB;

			if (a.getVerb(verb) == null)
				verb = Verb.PICKUP_VERB;

			if (a.getVerb(verb) == null)
				verb = Verb.LOOKAT_VERB;

			runVerb(a, verb, null);
		} else if (uiMode == UIModes.TWO_BUTTONS) {
			String verb = Verb.LOOKAT_VERB;

			if (!lookatButton) {
				verb = a.getVerb(Verb.TALKTO_VERB) != null ? Verb.TALKTO_VERB : Verb.PICKUP_VERB;
			}

			runVerb(a, verb, null);
		} else if (uiMode == UIModes.PIE) {
			getInputUnProject(unprojectTmp);
			pie.show(a, unprojectTmp.x, unprojectTmp.y);
			pointer.reset();
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
	@Override
	public void runVerb(InteractiveActor a, String verb, String target) {
		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	void showMenu() {
		pause();
		ui.setCurrentScreen(Screens.MENU_SCREEN);
	}

	public InventoryUI getInventoryUI() {
		return inventoryUI;
	}

	public Actor getTextManagerUI() {
		return textManagerUI;
	}

	public Actor getDialogUI() {
		return dialogUI;
	}

	public void setTextManagerUI(Actor a) {
		if (a instanceof ITextManagerUI) {
			textManagerUI.remove();
			textManagerUI = a;
			stage.addActor(textManagerUI);
		} else {
			EngineLogger.error("ERROR setTextManagerUI: actor is not a ITextManagerUI");

			dispose();
			Gdx.app.exit();
		}
	}

	public void setDialogUI(Actor a) {
		dialogUI.remove();
		dialogUI = a;
		stage.addActor(dialogUI);
	}

	public Stage getStage() {
		return stage;
	}

	public Button getMenuButton() {
		return menuButton;
	}

	@Override
	public void show() {
		final InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);

		if (getWorld().isDisposed()) {
			try {
				getWorld().load();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);

				dispose();
				Gdx.app.exit();
			}
		}

		getWorld().setListener(worldListener);
		getWorld().resume();
	}

	@Override
	public void hide() {
		getWorld().pause();
	}

	@Override
	public void pause() {
		getWorld().pause();
	}

	@Override
	public void resume() {
		getWorld().resume();

		// resets the error when continue
		if (EngineLogger.lastError != null && EngineLogger.debugMode()) {
			EngineLogger.lastError = null;
			EngineLogger.lastException = null;
		}
	}

	@Override
	public Viewport getViewport() {
		return viewport;
	}

	@Override
	public InteractiveActor getCurrentActor() {
		return currentActor;
	}

	@Override
	public void setUI(final UI ui) {
		this.ui = ui;

		recorder = ui.getRecorder();
		testerBot = ui.getTesterBot();

		pie = new PieMenu(this);
		textManagerUI = new TextManagerUI(ui.getSkin(), getWorld());
		menuButton = new AnimButton(ui.getSkin(), "menu");
		dialogUI = new DialogUI(ui.getSkin(), getWorld(), ui.getRecorder());
		pointer = new ScenePointer(ui.getSkin(), getWorld());
		inventoryUI = new InventoryUI(this, pointer);
		inventoryButton = new InventoryButton(ui.getSkin(), getWorld(), inventoryUI);

		uiMode = UIModes
				.valueOf(Config.getInstance().getProperty(Config.UI_MODE, "TWO_BUTTONS").toUpperCase(Locale.ENGLISH));

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen) && uiMode == UIModes.TWO_BUTTONS) {
			uiMode = UIModes.PIE;
		}

		pie.setVisible(false);

		menuButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});

		retrieveAssets(ui.getUIAtlas());

		stage = new Stage(viewport);
		stage.addActor(textManagerUI);
		stage.addActor(dialogUI);
		stage.addActor(inventoryButton);
		stage.addActor(menuButton);
		stage.addActor(inventoryUI);
		stage.addActor(pie);

		debugDrawer = new DebugDrawer(getWorld(), ui.getSkin(), viewport);
		hotspotsDrawer = new HotspotsDrawer(ui, viewport);
	}
}
