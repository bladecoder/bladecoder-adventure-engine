package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

public class EnumOptionsInputPanel extends InputPanel {
	private enum Empty {
		EMPTY;

		@Override
		public String toString() {
			return "";
		}
	}

	private final SelectBox<Enum<?>> input;

	EnumOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, Enum<?>[] options) {
		input = new SelectBox<>(skin);

		int l = options.length;
		if(!mandatory) l++;
		Enum<?>[] values = new Enum[l];

		if(!mandatory) {
			values[0] = Empty.EMPTY;
		}

		System.arraycopy(options, 0, values, mandatory ? 0 : 1, options.length);

		input.setItems(values);

		init(skin, title, desc, input, mandatory, defaultValue);
	}

	public String getText() {
		final Enum<?> selected = input.getSelected();
		return selected == Empty.EMPTY ? "" : selected.name();
	}

	public void setText(String s) {
		if(s == null)
			return;

		if ("".equals(s) && !isMandatory()) {
			input.setSelectedIndex(0);
		}
		Array<Enum<?>> items = input.getItems();
		for (Enum<?> item : items) {
			if (item != Empty.EMPTY && item.name().equalsIgnoreCase(s)) {
				input.setSelected(item);
			}
		}
	}
	
	public int getSelectedIndex() {
    	return input.getSelectedIndex();
    }
}
