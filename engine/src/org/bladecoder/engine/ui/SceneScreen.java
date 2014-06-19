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
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SceneScreen implements Screen {

	UI ui;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;
	private ShapeRenderer renderer;

	private boolean pieMode;
	boolean dragging = false;

	private final Recorder recorder;

	private final SceneViewport viewport = new SceneViewport();

	private final Vector3 unprojectTmp = new Vector3();
	
	// Actor under the cursor
	private Actor currentActor = null;

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
		currentActor = null;
		
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

			if (w.getInventory().isVisible()) {
				viewport.getInputUnProject(unprojectTmp);
				currentActor = inventoryUI.getItemAt(unprojectTmp.x, unprojectTmp.y);
			}

			if (currentActor == null) {
				w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

				currentActor = w.getCurrentScene().getActorAt(unprojectTmp.x, unprojectTmp.y);
			}

			if(currentActor != null) {
				ui.getPointer().setDesc(currentActor.getDesc());
				
				if(currentActor.getVerb("leave") != null)
					ui.getPointer().setLeaveIcon();
				else
					ui.getPointer().setHotspotIcon();
			} else {
				ui.getPointer().setDefaultIcon();
			}
		} else {
			ui.getPointer().reset();

			if (pie.isVisible()) {
				pie.hide();
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
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();

		if (EngineLogger.debugMode()) {
			w.getSceneCamera().getInputUnProject(
					viewport, unprojectTmp);

			StringBuilder sb = new StringBuilder();
			sb.append("Mouse ( ");
			sb.append((int) unprojectTmp.x);
			sb.append(", ");
			sb.append((int) unprojectTmp.y);
			sb.append(") FPS:");
			sb.append(Gdx.graphics.getFramesPerSecond());

			if (w.getCurrentScene().getBackgroundMap() != null) {
				sb.append(" Map: ");
				sb.append((int) (w.getCurrentScene().getBackgroundMap()
						.getDepth(unprojectTmp.x, unprojectTmp.y) * 10));
			}

			String strDebug = sb.toString();

			TextBounds b = EngineLogger.getDebugFont().getBounds(strDebug);
			RectangleRenderer.draw(batch, 0,
					viewport.getViewportHeight() - b.height - 10,
					b.width, b.height + 10, Color.BLACK);
			EngineLogger.getDebugFont().draw(batch, strDebug, 0,
					viewport.getViewportHeight());
		}

		if (World.getInstance().getCurrentDialog() != null
				&& !recorder.isPlaying()) { // DIALOG MODE

			if (!World.getInstance().inCutMode()) {
				viewport.getInputUnProject(unprojectTmp);
				dialogUI.draw(batch, (int) unprojectTmp.x, (int) unprojectTmp.y);
			}

			textManagerUI.draw(batch);
			ui.getPointer().draw(batch, false, viewport);
		} else {

			textManagerUI.draw(batch);

			viewport.getInputUnProject(unprojectTmp);
			inventoryUI.draw(batch, (int) unprojectTmp.x, (int) unprojectTmp.y);

			if (pieMode)
				pie.draw(batch);

			if (!World.getInstance().inCutMode() && !recorder.isPlaying())
				ui.getPointer().draw(batch, dragging, viewport);
		}

		Transition t = World.getInstance().getCurrentScene().getTransition();

		if (t != null) {
			t.draw(batch, viewport.getViewportWidth(), viewport.getViewportHeight());
		}

		recorder.draw(batch);

		batch.end();
	}

	@Override
	public void resize(int width, int height) {

		if(!World.getInstance().isDisposed()) {
			viewport.setWorldSize(World.getInstance().getWidth(), World.getInstance().getHeight());
		} else {
			viewport.setWorldSize(width, height);
		}
		
		viewport.update(width, height, true);
		World.getInstance().getSceneCamera().update();
		
		pie.resize(viewport.getViewportWidth(), viewport.getViewportHeight());
		inventoryUI.resize(viewport.getViewportWidth(), viewport.getViewportHeight());
		dialogUI.resize(viewport.getViewportWidth(), viewport.getViewportHeight());
		textManagerUI.resize(viewport.getViewportWidth(), viewport.getViewportHeight());
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

	
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		World w = World.getInstance();

		if (w.isPaused() || recorder.isPlaying())
			return;
		
		viewport.getInputUnProject(unprojectTmp);

		switch (type) {
		case SceneInputProcessor.TOUCH_UP:

			if (w.inCutMode() && !recorder.isRecording()) {
				w.getTextManager().next();
			} else if (w.getCurrentDialog() != null) {
				dialogUI.touchEvent(SceneInputProcessor.TOUCH_UP, unprojectTmp.x, unprojectTmp.y, pointer,
						button);
			} else if (w.getCurrentScene().getOverlay() != null) {
				w.getCurrentScene().getOverlay().click();
			} else if (dragging) {
				inventoryUI.touchEvent(SceneInputProcessor.TOUCH_UP, unprojectTmp.x, unprojectTmp.y,
						pointer, button);
				dragging = false;
			} else if (pie.isVisible()) {
				ui.getPointer().setFreezeHotSpot(false, viewport);
				pie.touchEvent(SceneInputProcessor.TOUCH_UP, unprojectTmp.x, unprojectTmp.y, pointer,
						button);
			} else if (inventoryUI.contains(unprojectTmp.x, unprojectTmp.y)) {
				inventoryUI.touchEvent(SceneInputProcessor.TOUCH_UP, unprojectTmp.x, unprojectTmp.y,
						pointer, button);
			} else {
				sceneClick(button == 1);
			}
			break;

		case SceneInputProcessor.TOUCH_DOWN:
			if (pie.isVisible()) {
				pie.touchEvent(SceneInputProcessor.TOUCH_DOWN, unprojectTmp.x, unprojectTmp.y, pointer,
						button);
			} else if (!w.inCutMode() && inventoryUI.contains(unprojectTmp.x, unprojectTmp.y)) {
				inventoryUI.touchEvent(SceneInputProcessor.TOUCH_DOWN, unprojectTmp.x, unprojectTmp.y,
						pointer, button);
			}

			break;

		case SceneInputProcessor.DRAG:
			if (inventoryUI.contains(unprojectTmp.x, unprojectTmp.y)
					&& inventoryUI.getItemAt(unprojectTmp.x, unprojectTmp.y) != null && !dragging) {

				inventoryUI.touchEvent(SceneInputProcessor.DRAG, unprojectTmp.x, unprojectTmp.y, pointer,
						button);

				dragging = true;

				if (pie.isVisible()) {
					pie.hide();
					ui.getPointer().setFreezeHotSpot(false, viewport);
				}
			}
			break;
		}
	}

	private void sceneClick(boolean lookat) {
		World w = World.getInstance();

		w.getSceneCamera().getInputUnProject(
				viewport, unprojectTmp);

		Scene s = w.getCurrentScene();

		if (currentActor != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(currentActor.toString());
			}

			actorClick(currentActor, lookat);
		} else if (s.getPlayer() != null) {
			if (s.getPlayer().getVerb("goto") != null) {
				if (recorder.isRecording()) {
					recorder.add(s.getPlayer().getId(), "goto", null);
				}

				s.getPlayer().runVerb("goto");
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
			if (recorder.isRecording()) {
				recorder.add(a.getId(), "leave", null);
			}

			a.runVerb("leave");
		} else if (!pieMode) {
			String verb = "lookat";
			
			if(!lookat) {
				verb = a.getVerb("talkto") != null? "talkto":"pickup";
			}			
			
			if (recorder.isRecording()) {
				recorder.add(a.getId(), verb, null);
			}

			a.runVerb(verb);
		} else {
			viewport.getInputUnProject(unprojectTmp);
			pie.show(a, unprojectTmp.x, unprojectTmp.y);
			ui.getPointer().setFreezeHotSpot(true, viewport);
		}
	}
	
	public void showMenu() {
		ui.setScreen(State.MENU_SCREEN);
	}

	private void resetUI() {

		if (pie.isVisible()) {
			pie.hide();
		}

		ui.getPointer().reset();

		dragging = false;
		currentActor = null;
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
		resetUI();
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

	public Viewport getViewport() {
		return viewport;
	}

	public Actor getCurrentActor() {
		return currentActor;
	}

}
