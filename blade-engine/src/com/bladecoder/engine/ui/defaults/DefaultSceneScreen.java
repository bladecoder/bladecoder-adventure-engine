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

import java.io.IOException;
import java.util.Locale;

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
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
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
import com.bladecoder.engine.ui.InventoryButton;
import com.bladecoder.engine.ui.InventoryUI;
import com.bladecoder.engine.ui.InventoryUI.InventoryPos;
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
	private final static float LOADING_WAIT_TIME_MS = 500f;

	private UI ui;

	private Stage stage;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private Actor dialogUI;
	private TextManagerUI textManagerUI;
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

	private final GestureDetector inputProcessor = new GestureDetector(new GestureDetector.GestureAdapter() {
		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			return true;
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			EngineLogger.debug("Event TAP button: " + button + " count: " + count);

			World w = ui.getWorld();

			if (w.isPaused() || recorder.isPlaying() || testerBot.isEnabled())
				return true;

			if (pie.isVisible()) {
				pie.hide();
			}

			if (drawHotspots)
				drawHotspots = false;
			else {
				getInputUnProject(unprojectTmp);

				if ((w.inCutMode() && !recorder.isRecording())
						|| (!w.inCutMode() && !TextManager.AUTO_HIDE_TEXTS && textManagerUI.isVisible())) {
					w.getCurrentScene().getTextManager().next();
				} else if (inventoryUI.isVisible()) {
					inventoryUI.hide();
				} else if (!w.hasDialogOptions()) {
					if (button == 2) {
						// Show inventory with the middle button
						if (!inventoryUI.isVisible()) {
							inventoryUI.show();
						}
					} else {
						sceneClick(button, count);
					}
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
			case Input.Keys.D:
				if (UIUtils.ctrl())
					EngineLogger.toggle();
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
				if (EngineLogger.debugMode()) {
					try {
						ui.getWorld().saveGameState();
					} catch (IOException e) {
						EngineLogger.error(e.getMessage());
					}
				}
				break;
			case 'l':
				if (EngineLogger.debugMode()) {
					try {
						ui.getWorld().loadGameState();
					} catch (IOException e) {
						EngineLogger.error(e.getMessage());
					}
				}
				break;
			case 't':
				if (EngineLogger.debugMode()) {
					testerBot.setEnabled(!testerBot.isEnabled());
					updateUI();
				}
				break;
			case '.':
				if (EngineLogger.debugMode()) {
					if (recorder.isRecording())
						recorder.setRecording(false);
					else
						recorder.setRecording(true);

					updateUI();
				}
				break;
			case ',':
				if (EngineLogger.debugMode()) {
					if (recorder.isPlaying())
						recorder.setPlaying(false);
					else {
						recorder.load();
						recorder.setPlaying(true);
					}

					updateUI();
				}
				break;
			case 'p':
				if (ui.getWorld().isPaused()) {
					resume();
				} else {
					pause();
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

		@Override
		public boolean scrolled(int amount) {
			if (uiEnabled && !ui.getWorld().hasDialogOptions()
					&& ui.getWorld().getInventory().isVisible()) {

				boolean fromDown = (inventoryUI.getInventoryPos() == InventoryPos.CENTER
						|| inventoryUI.getInventoryPos() == InventoryPos.DOWN);

				if ((amount > 0 && fromDown || amount < 0 && !fromDown) && inventoryUI.isVisible())
					inventoryUI.hide();
				else if ((amount > 0 && !fromDown || amount < 0 && fromDown) && !inventoryUI.isVisible()) {
					if (uiMode == UIModes.PIE && pie.isVisible())
						pie.hide();

					inventoryUI.show();
				}
			}

			return true;
		}

	};

	private final WorldListener worldListener = new WorldListener() {
		@Override
		public void text(Text t) {
			if(t != null && t.type == Text.Type.UI) {
				showUIText(t);
			} else {			
				textManagerUI.setText(t);
			}
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
			inventoryButton.setVisible(value);
		}

		@Override
		public void pause(boolean value) {
			updateUI();
		}
	};

	public DefaultSceneScreen() {
		viewport = Config.getProperty(Config.EXTEND_VIEWPORT_PROP, true) ? new SceneExtendViewport()
				: new SceneFitViewport();
		showDesc = Config.getProperty(Config.SHOW_DESC_PROP, true);
		fastLeave = Config.getProperty(Config.FAST_LEAVE, false);
	}

	public UI getUI() {
		return ui;
	}
	
	private void showUIText(Text t) {
		// Type UI texts will show at the same time that TextManagerUI texts.
		
		String style = t.style == null ?"ui-text":t.style;
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
		msg.addAction(sequence(Actions.fadeIn(0.4f, Interpolation.fade), Actions.delay(t.time,
				sequence(fadeOut(0.4f, Interpolation.fade), Actions.removeActor()))));
	}

	private void updateUI() {
		World w = ui.getWorld();

		if (uiMode == UIModes.PIE && pie.isVisible())
			pie.hide();

		currentActor = null;

		if (w.isPaused() || w.inCutMode() || testerBot.isEnabled() || recorder.isPlaying()) {
			// DISABLE UI
			inventoryUI.hide();
			inventoryButton.setVisible(false);
			dialogUI.setVisible(false);

			inventoryUI.cancelDragging();
			pointer.reset();
			uiEnabled = false;
		} else {
			if (ui.getWorld().hasDialogOptions()) {
				inventoryUI.hide();
				inventoryButton.setVisible(false);
				dialogUI.setVisible(true);
				inventoryUI.cancelDragging();
				pointer.reset();
			} else {
				inventoryUI.hide();
				dialogUI.setVisible(false);
				inventoryButton.setVisible(w.getInventory().isVisible());
			}

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

		if (!world.isDisposed()) {
			world.update(delta * speed);

			// if the game ends returns
			if (world.isDisposed())
				return;
		}

		AssetState assetState = world.getAssetState();

		if (world.getAssetState() != AssetState.LOADED) {

			resetUI();

			if (assetState == AssetState.LOAD_ASSETS || assetState == AssetState.LOAD_ASSETS_AND_INIT_SCENE) {
				// update() to set LOADING state
				world.update(0);
			}

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
				world.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

				// update() to retrieve assets and exec init verb
				world.update(0);
			}

			return;
		}

		stage.act(delta);

		if (world.isPaused())
			return;

		recorder.update(delta * speed);
		testerBot.update(delta * speed);

		InteractiveActor actorUnderCursor = null;

		if (uiEnabled && !world.hasDialogOptions()) {
			if (inventoryUI.isVisible()) {
				unproject2Tmp.set(Gdx.input.getX(), Gdx.input.getY());
				inventoryUI.screenToLocalCoordinates(unproject2Tmp);
				actorUnderCursor = inventoryUI.getItemAt(unproject2Tmp.x, unproject2Tmp.y);
			} else {

				final float tolerance;

				if (inventoryUI.isDragging())
					tolerance = DPIUtils.getTouchMinSize();
				else if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
					tolerance = DPIUtils.getTouchMinSize() / 2;
				else
					tolerance = 0;

				actorUnderCursor = world.getInteractiveActorAtInput(viewport, tolerance);
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
						
						if(actionVerb != null && actionVerb.getIcon() != null
								&& (r = getUI().getSkin().getAtlas().findRegion(actionVerb.getIcon())) != null) {
							pointer.setIcon(r);
						} else {						
							pointer.setHotspotIcon();
						}
					}
				} else {
					pointer.setDefaultIcon();
				}
			} else if(pie.isVisible()) {
				currentActor = actorUnderCursor;
			}
		}
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

			// sbTmp.append("( ");
			// sbTmp.append((int) unprojectTmp.x);
			// sbTmp.append(", ");
			// sbTmp.append((int) unprojectTmp.y);
			// sbTmp.append(") FPS:");
			// sbTmp.append(Gdx.graphics.getFramesPerSecond());
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

			// if (w.getCurrentScene().getPlayer() != null) {
			// sbTmp.append(" Depth Scl: ");
			// sbTmp.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
			// }

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

		resetUI();

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

	public void dispose() {
		renderer.dispose();
		stage.dispose();
	}

	private void retrieveAssets(TextureAtlas atlas) {
		renderer = new ShapeRenderer();
		inventoryUI.retrieveAssets(atlas);
	}

	private void sceneClick(int button, int count) {
		World w = ui.getWorld();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

		Scene s = w.getCurrentScene();
		CharacterActor player = s.getPlayer();

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
	public void runVerb(InteractiveActor a, String verb, String target) {
		// COMMENTED BECAUSE THE INVENTORY ONLY HIDES WHEN CUTMODE
		// if (inventoryUI.isVisible())
		// inventoryUI.hide();

		if (recorder.isRecording()) {
			recorder.add(a.getId(), verb, target);
		}

		a.runVerb(verb, target);
	}

	private void showMenu() {
		pause();
		ui.setCurrentScreen(Screens.MENU_SCREEN);
	}

	private void resetUI() {
		if (pie.isVisible()) {
			pie.hide();
		}

		pointer.reset();
		inventoryUI.cancelDragging();

		currentActor = null;
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

	public void setTextManagerUI(TextManagerUI a) {
		textManagerUI.remove();
		textManagerUI = a;
		stage.addActor(textManagerUI);
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

		textManagerUI.setText(ui.getWorld().getCurrentScene().getTextManager().getCurrentText());

		updateUI();
	}

	@Override
	public void hide() {
		ui.getWorld().pause();
		resetUI();
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

	public Viewport getViewport() {
		return viewport;
	}

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
