package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class StringOptionsInputPanel extends InputPanel implements OptionsInputPanel {
	private final FilteredSelectBox<String> input;
	
	StringOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, String[] options) {
		input = new FilteredSelectBox<String>(skin);
		
		int l = options.length;
		if(!mandatory) l++;
		String[] values = new String[l];
		
		if(!mandatory) {
			values[0] = "";
		}

		System.arraycopy(options, 0, values, mandatory ? 0 : 1, options.length);

		input.setItems(values);
		
		init(skin, title, desc, input, mandatory, defaultValue);
	}
	
	@Override
	public String getText() {
		if(input.getSelected()==null)
			return null;
		
		return input.getSelected().isEmpty()?null:input.getSelected();
	}

	@Override
	public void setText(String s) {
		if(s == null)
			return;
		
		int idx = input.getItems().indexOf(s, false);
		if(idx != -1)
			input.setSelectedIndex(idx);
	}

	@Override
	public int getSelectedIndex() {
    	return input.getSelectedIndex();
    }
}
