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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;

public class LayerInputPanel extends EditableOptionsInputPanel {	
	LayerInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		NodeList actors = Ctx.project.getSelectedChapter().getLayers(Ctx.project.getSelectedScene());
		int l = actors.getLength();
		if(!mandatory) l++;
		String values[] = new String[l];
		
		if(!mandatory) {
			values[0] = "";
		}
		
		for(int i = 0; i < actors.getLength(); i++) {
			if(mandatory)
				values[i] = ((Element)actors.item(i)).getAttribute("id");
			else
				values[i+1] = ((Element)actors.item(i)).getAttribute("id");
		}
		
		input = new EditableSelectBox(skin);
		init(skin, title, desc, input, mandatory, defaultValue);
		input.setItems(values);
       	
		if(defaultValue != null)
    		setText(defaultValue);
	}
}
