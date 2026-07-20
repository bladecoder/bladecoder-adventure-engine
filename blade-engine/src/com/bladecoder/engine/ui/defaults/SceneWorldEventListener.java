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
