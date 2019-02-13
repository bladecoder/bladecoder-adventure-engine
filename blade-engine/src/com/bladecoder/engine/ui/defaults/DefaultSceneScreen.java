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

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.model.WorldListener;
import com.bladecoder.engine.ui.DialogUI;
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
import com.bladecoder.engine.util.RectangleRenderer;

public class DefaultSceneScreen implements SceneScreen {
	public boolean isUiEnabled() {
		return uiEnabled;
	}

	public PieMenu getPie() {
		return pie;
	}

	public InventoryButton getInventoryButton() {
		return inventoryButton;
	}

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

	// Actor under the cursor
	private InteractiveActor currentActor = null;

	private boolean drawHotspots = false;
	private final boolean showDesc;
	private final boolean fastLeave;

	private float speed = 1.0f;

	private ScenePointer pointer;

	private boolean uiEnabled = true;

	private final GlyphLayout textLayout = new GlyphLayout();

	private final GestureDetector inputProcessor = new SceneGestureDetector(this);

	private final WorldListener worldListener = new SceneWorldListener(this);

	public DefaultSceneScreen() {
		viewport = Config.getProperty(Config.EXTEND_VIEWPORT_PROP, true) ? new SceneExtendViewport()
				: new SceneFitViewport();
		showDesc = Config.getProperty(Config.SHOW_DESC_PROP, true);
		fastLeave = Config.getProperty(Config.FAST_LEAVE, false);
	}

	@Override
	public UI getUI() {
		return ui;
	}

	public boolean getDrawHotspots() {
		return drawHotspots;
	}

	public void setDrawHotspots(boolean drawHotspots) {
		this.drawHotspots = drawHotspots;
	}

	public UIModes getUIMode() {
		return uiMode;
	}

	void showUIText(Text t) {
		// Type UI texts will show at the same time that TextManagerUI texts.

		String style = t.style == null ? "ui-text" : t.style;
		Label msg = new Label(t.str, getUI().getSkin(), style);

		msg.setWrap(true);
		msg.setAlignment(Align.center, Align.center);
		msg.setColor(t.color);
		msg.setSize(msg.getWidth() + DPIUtils.getMarginSize() * 2, msg.getHeight() + DPIUtils.getMarginSize() * 2);

		stage.addActor(msg);
		unprojectTmp.set(t.x, t.y, 0);
		ui.getWorld().getSceneCamera().scene2screen(getStage().getViewport(), unprojectTmp);

		float posx, posy;

		if (t.x == TextManager.POS_CENTER) {
			posx = (getStage().getViewport().getScreenWidth() - msg.getWidth()) / 2;
		} else if (t.y == TextManager.POS_SUBTITLE) {
			posx = DPIUtils.getMarginSize();
		} else {
			posx = unprojectTmp.x;
		}

		if (t.y == TextManager.POS_CENTER) {
			posy = (getStage().getViewport().getScreenHeight() - msg.getHeight()) / 2;
		} else if (t.y == TextManager.POS_SUBTITLE) {
			posy = getStage().getViewport().getScreenHeight() - msg.getHeight() - DPIUtils.getMarginSize() * 3;
		} else {
			posy = unprojectTmp.y;
		}

		msg.setPosition(posx, posy);
		msg.getColor().a = 0;
		msg.addAction(sequence(Actions.fadeIn(0.4f, Interpolation.fade),
				Actions.delay(t.time, sequence(fadeOut(0.4f, Interpolation.fade), Actions.removeActor()))));
	}

	void updateUI() {
		World w = ui.getWorld();

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
			if (ui.getWorld().hasDialogOptions()) {
				inventoryUI.hide();
				inventoryButton.setVisible(false);
				dialogUI.setVisible(true);
				inventoryUI.cancelDragging();
			} else {
				inventoryUI.hide();
				dialogUI.setVisible(false);
				inventoryButton.setVisible(w.getInventory().isVisible());
			}

			uiEnabled = true;

			EngineLogger.debug("Updating UI: ENABLED.");
		}
	}

	/**
	 * Sets the game speed. Can be used to fastfordward
	 *
	 * @param s
	 *            The multiplier speed. ej. 2.0
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
		final World world = ui.getWorld();
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

			if (t - t0 >= LOADING_WAIT_TIME_MS) {
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

		if (world.isPaused())
			return;

		recorder.update(deltaScaled);
		testerBot.update(deltaScaled);

		InteractiveActor actorUnderCursor = null;

		if (uiEnabled && !world.hasDialogOptions()) {
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

				if (currentActor != null) {
					if (showDesc)
						pointer.setDesc(currentActor.getDesc());

					Verb leaveVerb = currentActor.getVerb(Verb.LEAVE_VERB);

					TextureRegion r = null;

					if (leaveVerb != null) {
						if (leaveVerb.getIcon() != null
								&& (r = getUI().getSkin().getAtlas().findRegion(leaveVerb.getIcon())) != null) {
							pointer.setIcon(r);

						} else {
							pointer.setLeaveIcon(calcLeaveArrowRotation(currentActor));
						}
					} else {
						Verb actionVerb = currentActor.getVerb(Verb.ACTION_VERB);

						if (actionVerb != null && actionVerb.getIcon() != null
								&& (r = getUI().getSkin().getAtlas().findRegion(actionVerb.getIcon())) != null) {
							pointer.setIcon(r);
						} else {
							pointer.setHotspotIcon();
						}
					}
				} else {
					pointer.setDefaultIcon();
				}
			} else if (pie.isVisible()) {
				currentActor = actorUnderCursor;
			}
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

		return ui.getWorld().getInteractiveActorAtInput(viewport, tolerance);
	}

	/**
	 * Calcs the rotation based in the actor screen position
	 */
	private float calcLeaveArrowRotation(InteractiveActor actor) {
		Verb verb = actor.getVerb(Verb.LEAVE_VERB);

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
		final World world = ui.getWorld();

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
			drawHotspots(batch);

		// DRAW DEBUG STRING
		if (EngineLogger.debugMode()) {
			drawDebugText(batch);
		}

		batch.end();
	}

	private void drawDebugText(SpriteBatch batch) {
		World w = ui.getWorld();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

		Color color;

		sbTmp.setLength(0);

		if (EngineLogger.lastError != null) {
			sbTmp.append(EngineLogger.lastError);

			color = Color.RED;
		} else {

			// sbTmp.append(" Density:");
			// sbTmp.append(Gdx.graphics.getDensity());
			// sbTmp.append(" UI Multiplier:");
			// sbTmp.append(DPIUtils.getSizeMultiplier());
			sbTmp.append(" ");

			long millis = w.getTimeOfGame();
			long second = (millis / 1000) % 60;
			long minute = (millis / (1000 * 60)) % 60;
			long hour = (millis / (1000 * 60 * 60));

			String time = String.format("%02d:%02d:%02d", hour, minute, second);

			sbTmp.append(time);

			if (EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
				if (w.inCutMode()) {
					sbTmp.append(" CUT_MODE ");
				} else if (w.hasDialogOptions()) {
					sbTmp.append(" DIALOG_MODE ");
				} else if (w.isPaused()) {
					sbTmp.append(" PAUSED ");
				}

				sbTmp.append(" ( ");
				sbTmp.append((int) unprojectTmp.x);
				sbTmp.append(", ");
				sbTmp.append((int) unprojectTmp.y);
				sbTmp.append(") FPS:");
				sbTmp.append(Gdx.graphics.getFramesPerSecond());

				if (w.getCurrentScene().getState() != null) {
					sbTmp.append(" Scn State: ");
					sbTmp.append(w.getCurrentScene().getState());
				}

				if (w.getCurrentScene().getPlayer() != null) {
					sbTmp.append(" Depth Scl: ");
					sbTmp.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
				}
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
			world.getSceneCamera().scene2screen(viewport, unprojectTmp);

			if (!showDesc || ia.getDesc() == null) {

				float size = DPIUtils.ICON_SIZE * DPIUtils.getSizeMultiplier();

				if (ia.getVerb(Verb.LEAVE_VERB) != null) {
					TransformDrawable drawable = (TransformDrawable) getUI().getSkin().getDrawable(Verb.LEAVE_VERB);

					drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size / 2, size / 2, size,
							size, 1.0f, 1.0f, calcLeaveArrowRotation(ia));
				} else {
					Drawable drawable = getUI().getSkin().getDrawable("hotspot");

					if (drawable != null)
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
		final World world = ui.getWorld();
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
		World w = ui.getWorld();

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
		// COMMENTED BECAUSE THE INVENTORY ONLY HIDES WHEN CUTMODE
		// if (inventoryUI.isVisible())
		// inventoryUI.hide();

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
	}

	@Override
	public void hide() {
		ui.getWorld().pause();
		// dispose();
	}

	@Override
	public void pause() {
		ui.getWorld().pause();
	}

	@Override
	public void resume() {
		ui.getWorld().resume();

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
		textManagerUI = new TextManagerUI(ui);
		menuButton = new Button(ui.getSkin(), "menu");
		dialogUI = new DialogUI(ui);
		pointer = new ScenePointer(ui.getSkin());
		inventoryUI = new InventoryUI(this, pointer);
		inventoryButton = new InventoryButton(ui, inventoryUI);

		uiMode = UIModes.valueOf(Config.getProperty(Config.UI_MODE, "TWO_BUTTONS").toUpperCase(Locale.ENGLISH));

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
	}
}
