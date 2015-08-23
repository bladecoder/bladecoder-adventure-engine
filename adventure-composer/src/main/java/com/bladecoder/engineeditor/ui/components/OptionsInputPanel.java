package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public interface OptionsInputPanel {
	int getSelectedIndex();
	void addChangeListener(ChangeListener listener);
}
