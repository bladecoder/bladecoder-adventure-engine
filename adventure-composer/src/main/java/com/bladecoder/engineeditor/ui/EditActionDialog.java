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

import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class EditActionDialog extends EditModelDialog<Verb, Action> {
	private static final String CUSTOM_ACTION_STR = "CUSTOM ACTION";

	private static final String CUSTOM_INFO = "Custom action definition";

	private InputPanel actionPanel;
	private InputPanel classPanel;

	private InputPanel parameters[] = new InputPanel[0];

	@SuppressWarnings("unchecked")
	public EditActionDialog(Skin skin, Verb parent, Action e) {
		super(skin);

		String[] actions = ActionFactory.getActionList();
		Arrays.sort(actions);
		String[] actions2 = new String[actions.length + 1];
		System.arraycopy(actions, 0, actions2, 0, actions.length);
		actions2[actions2.length - 1] = CUSTOM_ACTION_STR;

		actionPanel = InputPanelFactory.createInputPanel(skin, "Action", "Select the action to create.", actions2,
				true);

		classPanel = InputPanelFactory.createInputPanel(skin, "Class", "Select the class for the custom action.", true);

		((SelectBox<String>) actionPanel.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAction();
			}
		});

		((TextField) classPanel.getField()).addListener(new FocusListener() {
			@Override
			public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
				if (!event.isFocused())
					setAction();
			}
		});

		if (e != null) {
			String id = ActionFactory.getName(e);

			classPanel.setText(e.getClass().getCanonicalName());

			if (id != null) {
				actionPanel.setText(id);
			} else {
				actionPanel.setText(CUSTOM_ACTION_STR);
			}

		}

		init(parent, e, parameters);

		setAction();
		
		if(e != null)
			modelToInputs();
	}

	private void setAction() {
		String id = actionPanel.getText();

		getCenterPanel().clear();
		addInputPanel(actionPanel);

		Action tmp = null;
		
		if (id.equals(CUSTOM_ACTION_STR)) {
			addInputPanel(classPanel);
			if (classPanel != null || !classPanel.getText().trim().isEmpty())
				tmp = ActionFactory.createByClass(classPanel.getText(), null);
			setInfo(CUSTOM_INFO);
		} else {
			tmp = ActionFactory.create(id, null);
			setInfo(ActionUtils.getInfo(tmp));
		}
		
		if(e == null || tmp == null || !(e.getClass().getName().equals(tmp.getClass().getName())))
			e = tmp;

		if (e != null) {
			Param[] params = ActionUtils.getParams(e);

			parameters = new InputPanel[params.length];

			for (int i = 0; i < params.length; i++) {
				if (params[i].options instanceof Enum[]) {
					parameters[i] = InputPanelFactory.createInputPanel(getSkin(), params[i].name, params[i].desc,
							params[i].type, params[i].mandatory, params[i].defaultValue, (Enum[]) params[i].options);
				} else {
					parameters[i] = InputPanelFactory.createInputPanel(getSkin(), params[i].name, params[i].desc,
							params[i].type, params[i].mandatory, params[i].defaultValue, (String[]) params[i].options);
				}

				addInputPanel(parameters[i]);

				if ((parameters[i].getField() instanceof TextField && params[i].name.toLowerCase().endsWith("text"))
						|| parameters[i].getField() instanceof ScrollPane) {
					parameters[i].getCell(parameters[i].getField()).fillX();
				}
			}
		}

		// ((ScrollPane)(getContentTable().getCells().get(1).getActor())).setWidget(getCenterPanel());
	}

	@Override
	protected void inputsToModel(boolean create) {
		for (int j = 0; j < i.length; j++) {
			String v = i[j].getText();

			try {
				ActionUtils.setParam(e, i[j].getTitle(), v);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
		}
	}

	@Override
	protected void modelToInputs() {
		for (int j = 0; j < i.length; j++) {

			try {
				String v = ActionUtils.getParam(e, i[j].getTitle());
				i[j].setText(v);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
		}
	}
}
