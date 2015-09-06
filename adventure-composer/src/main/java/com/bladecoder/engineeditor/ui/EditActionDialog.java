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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.bladecoder.engine.actions.AbstractAction;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.utils.ModelUtils;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.utils.I18NUtils;

public class EditActionDialog extends EditElementDialog {
	private static final String CUSTOM_ACTION_STR = "CUSTOM ACTION";

	private static final String CUSTOM_INFO = "Custom action definition";

	private InputPanel actionPanel;
	private InputPanel classPanel;

	private List<Param> params = Collections.emptyList();

	@SuppressWarnings("unchecked")
	public EditActionDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);

		String[] actions = ActionFactory.getActionList();
		Arrays.sort(actions);
		String[] actions2 = new String[actions.length + 1];
		System.arraycopy(actions, 0, actions2, 0, actions.length);
		actions2[actions2.length - 1] = CUSTOM_ACTION_STR;

		actionPanel = InputPanelFactory
				.createInputPanel(skin, "Action", "Select the action to create.", actions2, true);

		classPanel = InputPanelFactory.createInputPanel(skin, "Class", "Select the class for the custom action.", true);

		actionPanel.getField().addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAction();
			}
		});

		classPanel.getField().addListener(new FocusListener() {
			@Override
			public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
				if (!event.isFocused())
					setAction();
			}
		});

		if (e != null) {
			classPanel.setText(e.getAttribute("class"));

			if (!e.getAttribute("action_name").isEmpty()) {
				actionPanel.setText(e.getAttribute("action_name"));
			}

			if (!e.getAttribute("class").isEmpty()) {
				actionPanel.setText(CUSTOM_ACTION_STR);
			}

		}

		init(new InputPanel[0], new String[0], doc, parent, "action", e);

		setAction();

		if (e != null) {
			for (int pos = 0; pos < a.length; pos++) {
				InputPanel input = i[pos];
				if (I18NUtils.mustTraslateAttr(a[pos])) {
					input.setText(doc.getTranslation(e.getAttribute(a[pos])));
				} else {
					input.setText(e.getAttribute(a[pos]));
				}
			}
		}
	}

	private String[] getAttrs() {
		final List<String> list = params.stream().map(Param::getId).collect(Collectors.toList());
		return list.toArray(new String[list.size()]);
	}

	private void setAction() {
		String id = actionPanel.getText();

		getCenterPanel().clear();
		addInputPanel(actionPanel);

		AbstractAction ac = null;

		if (id.equals(CUSTOM_ACTION_STR)) {
			addInputPanel(classPanel);
			if (!classPanel.getText().trim().isEmpty())
				ac = ActionFactory.createByClass(classPanel.getText(), null);
		} else {
			ac = ActionFactory.create(id, null);
		}

		if (ac != null) {
			final Class<?> clazz = ac.getClass();

			setInfo(ModelUtils.getInfo(clazz));
			params = ModelUtils.getParams(clazz);
			Collection<InputPanel> inputs = ModelUtils.getInputsFromModelClass(params, getSkin());
			inputs.forEach(this::addInputPanel);

			i = inputs.toArray(new InputPanel[inputs.size()]);
			a = getAttrs();
		} else {
			setInfo(CUSTOM_INFO);
			params = Collections.emptyList();
			i = new InputPanel[0];
			a = new String[0];
		}

		// ((ScrollPane)(getContentTable().getCells().get(1).getActor())).setWidget(getCenterPanel());
	}

	@Override
	protected void fill() {

		// Remove previous params
		while (e.getAttributes().getLength() > 0) {
			e.removeAttribute(e.getAttributes().item(0).getNodeName());
		}

		String id = actionPanel.getText();

		if (id.equals(CUSTOM_ACTION_STR)) {
			e.setAttribute("class", classPanel.getText());
		} else {
			e.setAttribute("action_name", id);
		}

		super.fill();
	}
}
