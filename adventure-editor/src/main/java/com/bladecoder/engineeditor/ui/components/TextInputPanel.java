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

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

public class TextInputPanel extends InputPanel {
	TextArea input;
	
	TextInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		input = new TextArea("", skin);
		input.setPrefRows(10);
		
		ScrollPane scroll = new ScrollPane(input, skin);
		
		init(skin, title, desc, scroll, mandatory, defaultValue);
	}

	public String getText() {
		return input.getText().replaceAll("\n", "\\\\n");
	}

	public void setText(String s) {
		if (s == null) s = "";
		input.setText(s.replaceAll("\\\\n", "\n"));
	}
	
	public void setRows(float rows) {
		input.setPrefRows(rows);
	}
}
