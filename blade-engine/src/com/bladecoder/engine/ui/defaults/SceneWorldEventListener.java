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
 *******************************************************************************/
package com.bladecoder.engine.ui.defaults;

import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.model.WorldEventListener;
import com.bladecoder.engine.ui.ITextManagerUI;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.util.UIUtils;

public class SceneWorldEventListener implements WorldEventListener {

    private final DefaultSceneScreen dsc;

    public SceneWorldEventListener(DefaultSceneScreen dsc) {
        this.dsc = dsc;
    }

    @Override
    public void text(Text text) {
        if (text != null && text.type == Text.Type.UI) {
            UIUtils.showUIText(dsc.getStage(), dsc.getUI().getSkin(), dsc.getWorld(), text);
        } else {
            ((ITextManagerUI) dsc.getTextManagerUI()).setText(text);
        }
    }

    @Override
    public void dialogOptionsChanged() {
        dsc.updateUI();
    }

    @Override
    public void cutMode(boolean value) {
        dsc.updateUI();
    }

    @Override
    public void inventoryChanged() {
        boolean visible = dsc.getWorld().getInventory().isVisible();
        dsc.getInventoryUI().hide();
        dsc.getInventoryButton().setVisible(visible);
    }

    @Override
    public void pause(boolean value) {
        if (dsc.getUI().getWorld().getAssetState() == AssetState.LOADED)
            dsc.updateUI();
    }

    @Override
    public void gameEnded() {
        dsc.getUI().setCurrentScreen(UI.Screens.CREDIT_SCREEN);
    }
}
