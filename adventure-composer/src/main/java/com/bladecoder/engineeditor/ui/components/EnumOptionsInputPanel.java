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
package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

public class EnumOptionsInputPanel extends InputPanel {
	private final SelectBox<Enum> input;

	EnumOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, Enum[] options) {
		input = new SelectBox<>(skin);

		int l = options.length;
		if(!mandatory) l++;
		Enum[] values = new Enum[l];

		if(!mandatory) {
			values[0] = null;
		}

		System.arraycopy(options, 0, values, mandatory ? 0 : 1, options.length);

		input.setItems(values);

		init(skin, title, desc, input, mandatory, defaultValue);
	}

	public String getText() {
		return input.getSelected().name();
	}

	public void setText(String s) {
		if(s == null)
			return;

		if ("".equals(s) && !isMandatory()) {
			input.setSelectedIndex(0);
		}
		Array<Enum> items = input.getItems();
		for (Enum item : items) {
			if (item != null && item.name().equalsIgnoreCase(s)) {
				input.setSelected(item);
			}
		}
	}
	
	public int getSelectedIndex() {
    	return input.getSelectedIndex();
    }
}
