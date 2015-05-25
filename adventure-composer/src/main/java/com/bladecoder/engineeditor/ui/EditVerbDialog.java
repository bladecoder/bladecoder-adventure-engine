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
package com.bladecoder.engineeditor.ui;

import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.EditableOptionsInputPanel;
import com.bladecoder.engineeditor.ui.components.EditableSelectBox;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditVerbDialog extends EditElementDialog {
	private static final String VERBS[] = { "lookat", "pickup", "talkto", "use",
			"leave", "enter", "exit" };

	private static final String SCENE_VERBS[] = { "init", "test" };
	
	private static final String DEFAULT_DESC = "Verbs are used to create the game interaction. Select or write the verb to create";
	private static final String CUSTOM_VERB_DESC = "User defined verbs can be called\n from dialogs or inside other verbs using \nthe 'run_verb' action";

	private static final String VERBS_INFO[] = {
			"Called when the user clicks\n in the 'lookat' icon\n over a object in scene",
			"Called when the user clicks\n in the 'pickup' icon\n over a object in scene",
			"Called when the user clicks\n in the 'talkto' icon\n over a character in scene",
			"Called when the user drags and drops\n an inventory object over\n an object in scene or in inventory",
			"Called when the user clicks\n in an exit zone in scene",
			"Called when the player enters\n in the object bounding box",
			"Called when the player exits\n the object bounding box"};

	private static final String SCENE_VERBS_INFO[] = {
			"Called every time\n that the scene is loaded",
			"Called every time\n that the scene is loaded in test mode.\n'test' verb is called before the 'init' verb"};

	private InputPanel[] inputs;

	String attrs[] = { "id", "state", "target" };

	public EditVerbDialog(Skin skin, BaseDocument doc, Element parentElement,
			Element e) {
		super(skin);

		inputs = new InputPanel[3];
		
		inputs[0] = InputPanelFactory.createInputPanel(skin, "Verb ID",
				"Select the verb to create.", Type.EDITABLE_OPTION, true,
				"",	parentElement.getTagName()
						.equals("scene") ? SCENE_VERBS : VERBS);
		inputs[1] = InputPanelFactory.createInputPanel(skin, "State",
				"Select the state.");
		inputs[2] = InputPanelFactory.createInputPanel(skin,
				"Target BaseActor",
				"Select the target actor id for the 'use' verb");

		if (parentElement.getTagName().equals("scene"))
			setInfo(SCENE_VERBS_INFO[0]);
		else
			setInfo(VERBS_INFO[0]);

		inputs[0].getField()
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						updateDesc();
					}

				});
		
		((EditableSelectBox)inputs[0].getField()).getInput().setTextFieldListener(new TextFieldListener() {
			@Override
			public void keyTyped(TextField actor, char c) {
				updateDesc();
			}
		});		

		init(inputs, attrs, doc, parentElement, "verb", e);

		setVisible(inputs[2], false);

		if (e != null) {
			String id = e.getAttribute("id");

			if (id.equals("use"))
				setVisible(inputs[2], true);
		}
		
		updateDesc();
	}
	
	private void updateDesc() {
		String id = (String) inputs[0].getText();
		int i = ((EditableOptionsInputPanel) inputs[0])
				.getSelectedIndex();

		if(i == -1) {
			if(id.isEmpty())
				setInfo(DEFAULT_DESC);
			else
				setInfo(CUSTOM_VERB_DESC);
		} else {
			if (parent.getTagName().equals("scene")) {
				setInfo(SCENE_VERBS_INFO[i]);
			} else {
				setInfo(VERBS_INFO[i]);	
			}
		}

		if (id.equals("use"))
			setVisible(inputs[2], true);
		else
			setVisible(inputs[2], false);

		pack();		
	}
}
