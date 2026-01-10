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

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.defaults.DefaultSceneScreen;
import com.bladecoder.engine.ui.retro.VerbUI;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

import java.nio.IntBuffer;
import java.util.zip.Deflater;

public class UI {

    public enum Screens {
        INIT_SCREEN, SCENE_SCREEN, LOADING_SCREEN, MENU_SCREEN, HELP_SCREEN, CREDIT_SCREEN, LOAD_GAME_SCREEN,
        SAVE_GAME_SCREEN
    }

    public enum InputMode {
        GAMEPAD, MOUSE, TOUCHPANEL
    }

    private static final String SKIN_FILENAME = "ui/ui.json";

    private final Recorder recorder;
    private final TesterBot testerBot;

    private boolean fullscreen = false;

    private BladeScreen screen;

    private SpriteBatch batch;
    private Skin skin;
    private final World w;
    private InputMode inputMode;

    private final BladeScreen[] screens;

    public UI(World w) {
        this.w = w;
        recorder = new Recorder(this);
        testerBot = new TesterBot(w);

        batch = new SpriteBatch();

        screens = new BladeScreen[Screens.values().length];

        Gdx.input.setCatchKey(Input.Keys.MENU, true);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        loadAssets();

        if (Controllers.getControllers().size > 0) {
            inputMode = InputMode.GAMEPAD;
        } else if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen)) {
            inputMode = InputMode.TOUCHPANEL;
        } else {
            inputMode = InputMode.MOUSE;
        }

        screens[Screens.INIT_SCREEN.ordinal()] = getCustomScreenInstance(Screens.INIT_SCREEN.toString(),
                InitScreen.class);
        screens[Screens.SCENE_SCREEN.ordinal()] = getCustomScreenInstance(Screens.SCENE_SCREEN.toString(),
                DefaultSceneScreen.class);
        screens[Screens.LOADING_SCREEN.ordinal()] = getCustomScreenInstance(Screens.LOADING_SCREEN.toString(),
                LoadingScreen.class);
        screens[Screens.MENU_SCREEN.ordinal()] = getCustomScreenInstance(Screens.MENU_SCREEN.toString(),
                MenuScreen.class);
        screens[Screens.HELP_SCREEN.ordinal()] = getCustomScreenInstance(Screens.HELP_SCREEN.toString(),
                HelpScreen.class);
        screens[Screens.CREDIT_SCREEN.ordinal()] = getCustomScreenInstance(Screens.CREDIT_SCREEN.toString(),
                CreditsScreen.class);
        screens[Screens.LOAD_GAME_SCREEN.ordinal()] = getCustomScreenInstance(Screens.LOAD_GAME_SCREEN.toString(),
                LoadSaveScreen.class);
        screens[Screens.SAVE_GAME_SCREEN.ordinal()] = getCustomScreenInstance(Screens.SAVE_GAME_SCREEN.toString(),
                LoadSaveScreen.class);

        for (BladeScreen s : screens)
            s.setUI(this);

        setCurrentScreen(Screens.INIT_SCREEN);
    }

    public World getWorld() {
        return w;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    public TesterBot getTesterBot() {
        return testerBot;
    }

    private BladeScreen getCustomScreenInstance(String prop, Class<?> defaultClass) {
        String clsName = Config.getInstance().getProperty(prop, null);
        Class<?> instanceClass = defaultClass;

        if (clsName != null && !clsName.isEmpty()) {
            try {
                instanceClass = ClassReflection.forName(clsName);
                return (BladeScreen) ClassReflection.newInstance(instanceClass);
            } catch (Exception e) {
                EngineLogger.error("Error instancing screen. " + e.getMessage());
                // FIXME: Probably we just want to fail in this case, instead of creating a
                // different screen than the one expected?
                instanceClass = defaultClass;
            }
        }

        try {
            return (BladeScreen) ClassReflection.newInstance(instanceClass);
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

    public InputMode getInputMode() {
        return inputMode;
    }

    public void setInputMode(InputMode inputMode) {
        this.inputMode = inputMode;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public BladeScreen getCurrentScreen() {
        return screen;
    }

    public void setCurrentScreen(Screens s) {
        EngineLogger.debug("Setting SCREEN: " + s.name());
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

    public void render() {
        // for long processing frames, limit delta to 1/30f to avoid skipping animations
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);

        screen.render(delta);
    }

    private void loadAssets() {
        FileHandle skinFile = EngineAssetManager.getInstance().getAsset(SKIN_FILENAME);
        TextureAtlas atlas = new TextureAtlas(EngineAssetManager.getInstance()
                .getResAsset(SKIN_FILENAME.substring(0, SKIN_FILENAME.lastIndexOf('.')) + ".atlas"));
        skin = new BladeSkin(atlas);

        ((BladeSkin) skin).addStyleTag(VerbUI.VerbUIStyle.class);
        ((BladeSkin) skin).addStyleTag(TextManagerUI.TextManagerUIStyle.class);
        ((BladeSkin) skin).addStyleTag(DialogUI.DialogUIStyle.class);
        ((BladeSkin) skin).addStyleTag(InventoryUI.InventoryUIStyle.class);
        ((BladeSkin) skin).addStyleTag(CreditsScreen.CreditScreenStyle.class);
        ((BladeSkin) skin).addStyleTag(LoadSaveScreen.LoadSaveScreenStyle.class);
        ((BladeSkin) skin).addStyleTag(MenuScreen.MenuScreenStyle.class);

        skin.load(skinFile);

        if (!Config.getInstance().getProperty(Config.CHARACTER_ICON_ATLAS, "").equals("")) {
            EngineAssetManager.getInstance()
                    .loadAtlas(Config.getInstance().getProperty(Config.CHARACTER_ICON_ATLAS, null));
            EngineAssetManager.getInstance().finishLoading();
        }
    }

    public void resize(int width, int height) {
        if (screen != null)
            screen.resize(width, height);
    }

    public void toggleFullScreen() {
        if (!fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            fullscreen = true;
        } else {
            Gdx.graphics.setWindowedMode(w.getWidth(), w.getHeight());
            fullscreen = false;
        }
    }

    public void takeScreenshot(String filename, int width, boolean includeUI) {
        // get viewport
        IntBuffer results = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_VIEWPORT, results);

        int height = (int) (width * w.getSceneCamera().viewportHeight / w.getSceneCamera().viewportWidth);

        FrameBuffer fbo = new FrameBuffer(Format.RGB888, width, height, false);

        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        w.draw();
        BladeScreen sceneScreen = screens[Screens.SCENE_SCREEN.ordinal()];
        if (includeUI && sceneScreen instanceof DefaultSceneScreen) {
            ((DefaultSceneScreen) sceneScreen).getStage().draw();
        }

        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);

        // restore viewport
        fbo.end(results.get(0), results.get(1), results.get(2), results.get(3));

        PixmapIO.writePNG(EngineAssetManager.getInstance().getUserFile(filename), pixmap,
                Deflater.DEFAULT_COMPRESSION, true);

        fbo.dispose();
    }

    public void dispose() {
        screen.hide();
        batch.dispose();
        skin.dispose();

        RectangleRenderer.dispose();

        if (!Config.getInstance().getProperty(Config.CHARACTER_ICON_ATLAS, "").equals(""))
            EngineAssetManager.getInstance()
                    .disposeAtlas(Config.getInstance().getProperty(Config.CHARACTER_ICON_ATLAS, null));

        // DISPOSE ALL SCREENS
        for (BladeScreen s : screens)
            s.dispose();

        EngineAssetManager.getInstance().dispose();
    }

    public void resume() {
        if (Gdx.app.getType() != ApplicationType.Desktop) {
            // RESTORE GL CONTEXT
            RectangleRenderer.dispose();
        }

        if (screen != null)
            screen.resume();
    }

    public void pause() {
        if (screen != null)
            screen.pause();
    }
}
