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
package com.bladecoder.engine.ui;

import com.bladecoder.engine.ui.BladeSkin;
import com.bladecoder.engine.ui.CreditsScreen;
import com.bladecoder.engine.ui.HelpScreen;
import com.bladecoder.engine.ui.InitScreen;
import com.bladecoder.engine.ui.LoadingScreen;
import com.bladecoder.engine.ui.MenuScreen;
import com.bladecoder.engine.ui.Pointer;
import com.bladecoder.engine.ui.SceneScreen;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.Utils3D;

public class UI {

	private static final String SKIN_FILENAME = "ui/ui.json";

	private boolean fullscreen = false;
	private Pointer pointer;

	private BladeScreen screen;

	private boolean pieMode;

	private SpriteBatch batch;
	private Skin skin;

	public static enum Screens {
		INIT_SCREEN, SCENE_SCREEN, LOADING_SCREEN, MENU_SCREEN, HELP_SCREEN, CREDIT_SCREEN, LOAD_GAME, SAVE_GAME
	};
	
	private final BladeScreen screens[];

	public UI() {
		batch = new SpriteBatch();
		
		screens = new BladeScreen[Screens.values().length];

		Gdx.input.setCatchBackKey(true);
		Gdx.input.setCatchMenuKey(true);

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
			setPieMode(true);
		else {
			setPieMode(Config.getProperty(Config.PIE_MODE_DESKTOP_PROP, false));
		}
		
		loadAssets();
		
		screens[Screens.INIT_SCREEN.ordinal()] = getCustomScreenInstance(Config.INIT_SCREEN_CLASS_PROP, InitScreen.class);
		screens[Screens.SCENE_SCREEN.ordinal()] = new SceneScreen();
		screens[Screens.LOADING_SCREEN.ordinal()] = new LoadingScreen();
		screens[Screens.MENU_SCREEN.ordinal()] = getCustomScreenInstance(Config.MENU_SCREEN_CLASS_PROP, MenuScreen.class);
		screens[Screens.HELP_SCREEN.ordinal()] = getCustomScreenInstance(Config.HELP_SCREEN_CLASS_PROP, HelpScreen.class);
		screens[Screens.CREDIT_SCREEN.ordinal()] =  getCustomScreenInstance(Config.CREDIT_SCREEN_CLASS_PROP, CreditsScreen.class);
		screens[Screens.LOAD_GAME.ordinal()] = new LoadSaveScreen();
		screens[Screens.SAVE_GAME.ordinal()] = new LoadSaveScreen();
		
		for(BladeScreen s:screens)
			s.setUI(this);

		setCurrentScreen(Screens.INIT_SCREEN);
	}
	
	private BladeScreen getCustomScreenInstance(String prop, Class<?> defaultClass) {
		String clsName = Config.getProperty(prop, null);
		Class<?> instanceClass = defaultClass;
		
		if( clsName != null  && !clsName.isEmpty()) {
			try {
				instanceClass = ClassReflection.forName(clsName);
				return (BladeScreen)ClassReflection.newInstance(instanceClass);
			} catch (Exception e) {
				EngineLogger.error("Error instancing screen. " + e.getMessage());
				instanceClass = defaultClass;
			}
		} 
		
		try {
			return (BladeScreen)ClassReflection.newInstance(instanceClass);
		} catch (Exception e) {
			EngineLogger.error("Error instancing screen", e);
		}
		
		return null;
	}
	
	public BladeScreen getScreen(Screens state) {
		return screens[state.ordinal()];
	}
	
	public void setScreen(Screens state, BladeScreen s) {
		screens[state.ordinal()] = s;
	}
	
	public SpriteBatch getBatch() {
		return batch;
	}
	
	public Pointer getPointer() {
		return pointer;
	}

	public BladeScreen getCurrentScreen() {
		return screen;
	}

	public void setCurrentScreen(Screens s) {				
		setCurrentScreen(screens[s.ordinal()]);
	}
	
	public void setCurrentScreen(BladeScreen s) {

		if (screen != null) {
			screen.hide();
		}
		
		screen = s;

		screen.show();
			
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	public TextureAtlas getUIAtlas() {
		return skin.getAtlas();
	}
	
	public Skin getSkin() {
		return skin;
	}

	private void setPieMode(boolean m) {
		pieMode = m;
	}
	
	public boolean isPieMode() {
		return pieMode;
	}

	public void render() {
		// for long processing frames, limit delta to 1/30f to avoid skipping animations 
		float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);

		screen.render(delta);
	}

	private void loadAssets() {
		FileHandle skinFile = EngineAssetManager.getInstance().getAsset(SKIN_FILENAME);
		TextureAtlas atlas = new TextureAtlas(EngineAssetManager.getInstance().getResAsset(
				SKIN_FILENAME.substring(0,SKIN_FILENAME.lastIndexOf('.')) + ".atlas"));
		skin = new BladeSkin(skinFile, atlas);
		pointer = new Pointer(skin);
	}

	public void resize(int width, int height) {
		pointer.resize(width, height);

		if (screen != null)
			screen.resize(width, height);
	}

	public void toggleFullScreen() {
		if (!fullscreen) {
			Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode());
			fullscreen = true;
		} else {
			Gdx.graphics.setDisplayMode(World.getInstance().getWidth(), World
					.getInstance().getHeight(), false);
			fullscreen = false;
		}
	}

	public void dispose() {
		screen.hide();
		batch.dispose();
		skin.dispose();
		
		RectangleRenderer.dispose();
		Utils3D.dispose();
		EngineAssetManager.getInstance().dispose();
	}
	
	public void resume() {
		if(Gdx.app.getType() != ApplicationType.Desktop) {
			// RESTORE GL CONTEXT
			RectangleRenderer.dispose();
		}
		
		if(screen != null)
			screen.resume();
	}
	
	public void pause() {
		if(screen != null)
			screen.pause();
	}
}
