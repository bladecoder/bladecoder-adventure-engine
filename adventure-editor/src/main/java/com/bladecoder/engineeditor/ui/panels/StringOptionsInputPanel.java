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

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class StringOptionsInputPanel extends InputPanel implements OptionsInputPanel {
	private final SelectBox<String> input;
	
	StringOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, String[] options) {
		input = new SelectBox<>(skin);
		
		int l = options.length;
		if(!mandatory) l++;
		String values[] = new String[l];
		
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
