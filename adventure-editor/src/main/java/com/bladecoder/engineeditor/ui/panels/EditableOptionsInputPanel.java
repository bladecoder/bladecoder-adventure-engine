package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditableOptionsInputPanel<T> extends InputPanel implements OptionsInputPanel {
	protected final EditableSelectBox<T> input;

	EditableOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, T[] options) {
		input = new EditableSelectBox<>(skin);
		init(skin, title, desc, input, mandatory, defaultValue);

		if(options != null)
			input.setItems(options);
	}

	public String getText() {
		if(input.getSelected().isEmpty())
			return null;
		
		return input.getSelected();
	}

	@Override
	public void setText(String s) {
		if(s == null)
			return;

		input.setSelected(s);
	}

	@Override
	public int getSelectedIndex() {
		return input.getSelectedIndex();
	}
}
