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

import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.EditableSelectBox;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;
import com.bladecoder.engineeditor.ui.components.ScopePanel;

public class EditVerbDialog extends EditModelDialog<VerbManager, Verb> {
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
	
	private InputPanel id;
	private InputPanel state;
	private InputPanel target;
	
	private String scope;

	public EditVerbDialog(Skin skin, String scope, VerbManager parentElement,
			Verb e) {
		super(skin);
		
		this.scope = scope;
		
		id = InputPanelFactory.createInputPanel(skin, "Verb ID",
				"Select the verb to create.", Type.EDITABLE_OPTION, true,
				"",	ScopePanel.SCENE_SCOPE.equals(scope) ? SCENE_VERBS : VERBS);
		state = InputPanelFactory.createInputPanel(skin, "State",
				"Select the state.");
		target = InputPanelFactory.createInputPanel(skin,
				"Target BaseActor",
				"Select the target actor id for the 'use' verb");

		if (ScopePanel.SCENE_SCOPE.equals(scope))
			setInfo(SCENE_VERBS_INFO[0]);
		else
			setInfo(VERBS_INFO[0]);

		id.getField()
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						updateDesc();
					}

				});
		
		((EditableSelectBox<?>)id.getField()).getInput().setTextFieldListener(new TextFieldListener() {
			@Override
			public void keyTyped(TextField actor, char c) {
				updateDesc();
			}
		});		

		init(parentElement, e, new InputPanel[] { id, state, target });

		setVisible(target, false);

		if (e != null) {
			String id = e.getId();

			if (id.equals("use"))
				setVisible(target, true);
		}
		
		updateDesc();
	}
	
	private void updateDesc() {
		String idStr = (String) id.getText();
		int i = ((OptionsInputPanel) id)
				.getSelectedIndex();

		if(i == -1) {
			if(idStr.isEmpty())
				setInfo(DEFAULT_DESC);
			else
				setInfo(CUSTOM_VERB_DESC);
		} else {
			if (ScopePanel.SCENE_SCOPE.equals(scope)) {
				setInfo(SCENE_VERBS_INFO[i]);
			} else {
				setInfo(VERBS_INFO[i]);	
			}
		}

		if (idStr != null && idStr.equals("use"))
			setVisible(target, true);
		else
			setVisible(target, false);

		pack();		
	}	

	@Override
	protected void inputsToModel(boolean create) {
		
		if(create) {
			e = new Verb();
		} else {
			HashMap<String, Verb> verbs = parent.getVerbs();
			verbs.remove(e.getHashKey());
		}
		
		e.setId(id.getText());
		e.setState(state.getText());
		e.setTarget(target.getText());
		
		parent.addVerb(e);


		// TODO UNDO OP
//		UndoOp undoOp = new UndoAddElement(doc, e);
//		Ctx.project.getUndoStack().add(undoOp);

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		id.setText(e.getId());
		state.setText(e.getState());
		target.setText(e.getTarget());
	}	
}
