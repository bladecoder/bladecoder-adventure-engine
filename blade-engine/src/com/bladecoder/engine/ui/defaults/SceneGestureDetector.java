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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.bladecoder.engine.ui.InventoryUI.InventoryPos;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.ui.defaults.DefaultSceneScreen.UIModes;
import com.bladecoder.engine.util.EngineLogger;

import java.io.IOException;

public class SceneGestureDetector extends GestureDetector {

    private final DefaultSceneScreen dsc;
    private final SceneControllerHandler sceneControllerHandler;

    public SceneGestureDetector(DefaultSceneScreen dsc, SceneControllerHandler sceneControllerHandler) {
        super(new SceneGestureListener(dsc));
        this.dsc = dsc;
        this.sceneControllerHandler = sceneControllerHandler;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.ESCAPE:
            case Input.Keys.BACK:
            case Input.Keys.MENU:
                dsc.showMenu();
                break;
            case Input.Keys.D:
                if (UIUtils.ctrl())
                    EngineLogger.toggle();
                break;
            case Input.Keys.SPACE:
                if (dsc.getDrawHotspots())
                    dsc.setDrawHotspots(false);
                break;
        }

        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        switch (character) {
            case Input.Keys.F1:
                EngineLogger.setDebugLevel(EngineLogger.DEBUG0);
                break;
            case Input.Keys.F2:
                EngineLogger.setDebugLevel(EngineLogger.DEBUG1);
                break;
            case '1':
                break;
            case 'f':
                // ui.toggleFullScreen();
                break;
            case 'i':
                dsc.tap(SceneScreen.ActionButton.INVENTORY, 1);
                break;
            case '\n':
            case '\r':
            case 'q':
                dsc.getUI().setInputMode(InputMode.GAMEPAD);

                if (!sceneControllerHandler.clickOnUI()) {
                    dsc.tap(SceneScreen.ActionButton.LOOKAT, 1);
                }
                break;
            case 'e':
                dsc.getUI().setInputMode(InputMode.GAMEPAD);

                if (!sceneControllerHandler.clickOnUI()) {
                    dsc.tap(SceneScreen.ActionButton.ACTION, 1);
                }
                break;
            case 's':
                if (EngineLogger.debugMode()) {
                    try {
                        dsc.getUI().getWorld().saveGameState();
                    } catch (IOException e) {
                        EngineLogger.error(e.getMessage());
                    }
                }
                break;
            case 'l':
                if (EngineLogger.debugMode()) {
                    try {
                        dsc.getUI().getWorld().loadGameState();
                    } catch (IOException e) {
                        EngineLogger.error(e.getMessage());
                    }
                }
                break;
            case 't':
                if (EngineLogger.debugMode()) {
                    dsc.getUI().getTesterBot().setEnabled(!dsc.getUI().getTesterBot().isEnabled());
                    dsc.updateUI();
                }
                break;
            case '.':
                if (EngineLogger.debugMode()) {
                    dsc.getUI().getRecorder().setRecording(!dsc.getUI().getRecorder().isRecording());

                    dsc.updateUI();
                }
                break;
            case ',':
                if (EngineLogger.debugMode()) {
                    if (dsc.getUI().getRecorder().isPlaying())
                        dsc.getUI().getRecorder().setPlaying(false);
                    else {
                        dsc.getUI().getRecorder().load();
                        dsc.getUI().getRecorder().setPlaying(true);
                    }

                    dsc.updateUI();
                }
                break;
            case 'p':
                if (dsc.getUI().getWorld().isPaused()) {
                    dsc.resume();
                } else {
                    dsc.pause();
                }
                break;
            case 'z':
                sceneControllerHandler.focusNext(ScreenControllerHandler.PointerToNextType.LEFT);
                break;
            case 'x':
                sceneControllerHandler.focusNext(ScreenControllerHandler.PointerToNextType.RIGHT);
                break;
            case ' ':
                if (dsc.isUiEnabled() && !dsc.getUI().getWorld().hasDialogOptions()) {
                    dsc.setDrawHotspots(true);
                }
                break;
        }

        // FIXME: This is returning false even in the cases where we
        // actually process the character
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (dsc.isUiEnabled() && !dsc.getUI().getWorld().hasDialogOptions()
                && dsc.getUI().getWorld().getInventory().isVisible()) {

            dsc.getUI().setInputMode(InputMode.MOUSE);

            boolean fromDown = (dsc.getInventoryUI().getInventoryPos() == InventoryPos.CENTER
                    || dsc.getInventoryUI().getInventoryPos() == InventoryPos.DOWN);

            if ((amountY > 0 && fromDown || amountY < 0 && !fromDown) && dsc.getInventoryUI().isVisible())
                dsc.getInventoryUI().hide();
            else if ((amountY > 0 && !fromDown || amountY < 0 && fromDown) && !dsc.getInventoryUI().isVisible()) {
                if (dsc.getUIMode() == UIModes.PIE && dsc.getPie().isVisible())
                    dsc.getPie().hide();

                dsc.getInventoryUI().show();
            }
        }

        return true;
    }
}
