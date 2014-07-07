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
package org.bladecoder.engineeditor.ui;

import java.util.Arrays;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionFactory;
import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;

public class EditActionDialog extends EditElementDialog {
	private static final String CUSTOM_ACTION_STR = "CUSTOM ACTION";

	private static final String CUSTOM_INFO="Custom action definition";
	
	private InputPanel actionPanel;
	private InputPanel actorPanel;
	private InputPanel classPanel;

	private InputPanel parameters[];	

	@SuppressWarnings("unchecked")
	public EditActionDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);

		String[] actions = ActionFactory.getActionList();
		Arrays.sort(actions);
		String[] actions2 = new String[actions.length + 1];
		System.arraycopy(actions, 0, actions2, 0, actions.length);		
		actions2[actions2.length - 1] = CUSTOM_ACTION_STR;

		actionPanel = new InputPanel(skin, "Action",
				"Select the action to create.", actions2);
		
		actorPanel = new InputPanel(skin, "Target Actor",
				"Select the target actor id. Default is current actor.");
		classPanel = new InputPanel(skin, "Class",
				"Select the class for the custom action.", true);

		setAction();

		((SelectBox<String>) actionPanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						setAction();
					}
				});
		
		((TextField) classPanel.getField()).addListener(new FocusListener() {
			@Override
			public void keyboardFocusChanged (FocusEvent event, Actor actor, boolean focused) {
				if(!event.isFocused())
					setAction();
			}
		});

		if(e != null) {
			actorPanel.setText(e.getAttribute("actor"));
			classPanel.setText(e.getAttribute("class"));
			
			if(!e.getAttribute("action_name").isEmpty()) {
				actionPanel.setText(e.getAttribute("action_name"));
			} 
			
			if(!e.getAttribute("class").isEmpty()) {
				actionPanel.setText(CUSTOM_ACTION_STR);
			}
			
			setAction();
		}
		
		init(parameters, getAttrs(), doc, parent, "action", e);
	}
	
	private String[] getAttrs() {
		String inputs[] = new String[parameters.length];
		
		for (int j = 0; j < parameters.length ; j++) {
			InputPanel i=  parameters[j];
			inputs[j] = i.getTitle();
		}
		
		return inputs;
	}

	private void setAction() {
		String id = actionPanel.getText();

		getCenterPanel().clear();
		addInputPanel(actionPanel);
		addInputPanel(actorPanel);

		Action ac = null;
		
		if (id.equals(CUSTOM_ACTION_STR)) {
			addInputPanel(classPanel);
			if(!classPanel.getText().trim().isEmpty())
				ac = ActionFactory.createByClass(classPanel.getText(), null);
		} else {
			ac = ActionFactory.create(id, null);			
		}
		
		if (ac != null) {
			setInfo(ac.getInfo());

			Param[] params = ac.getParams();

			parameters = new InputPanel[params.length];

			for (int i = 0; i < params.length; i++) {
				parameters[i] = new InputPanel(getSkin(),params[i].name, params[i].desc,
						params[i].type, params[i].mandatory, params[i].defaultValue, params[i].options);
				addInputPanel(parameters[i]);
			}
			
			i = parameters;
			a = getAttrs();
		} else {
			setInfo(CUSTOM_INFO);
			i = new InputPanel[0];
			a = new String[0];
		}

	}

	@Override
	protected void fill() {
		String actor = actorPanel.getText().trim();
		
		// Remove previous params
		while(e.getAttributes().getLength() > 0) {
			e.removeAttribute(e.getAttributes().item(0).getNodeName());
		}

		if (!actor.isEmpty())
			e.setAttribute("actor", actor);
		
		String id = actionPanel.getText();
		
		if (id.equals(CUSTOM_ACTION_STR)) {
			e.setAttribute("class", classPanel.getText());
		} else {
			e.setAttribute("action_name", id);			
		}
		
		super.fill();
	}
}
