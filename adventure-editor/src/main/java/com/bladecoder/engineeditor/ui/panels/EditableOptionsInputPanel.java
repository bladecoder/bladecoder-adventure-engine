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
