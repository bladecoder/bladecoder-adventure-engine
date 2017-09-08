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
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ActionDetector;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.bladecoder.engineeditor.ui.panels.ScopePanel;

public class EditActionDialog extends EditModelDialog<Verb, Action> {
	private InputPanel actionPanel;
	private String scope;
	private int pos;

	@SuppressWarnings("unchecked")
	public EditActionDialog(Skin skin, Verb parent, Action e, String scope, int pos) {
		super(skin);

		this.scope = scope;
		this.pos = e == null ? pos + 1 : pos;

		String[] actions = ActionDetector.getActionNames();
		Arrays.sort(actions);

		actionPanel = InputPanelFactory.createInputPanel(skin, "Action", "Select the action to create.", actions, true);

		((SelectBox<String>) actionPanel.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAction();
			}
		});

		if (e != null) {
			String id = ActionUtils.getName(e.getClass());

			if (id != null) {
				actionPanel.setText(id);
			}

		}

		init(parent, e, new InputPanel[0]);

		setAction();

		if (e != null)
			modelToInputs();

	}

	private void setAction() {
		String id = actionPanel.getText();

		getCenterPanel().clear();
		addInputPanel(actionPanel);

		Action tmp = null;

		tmp = ActionDetector.create(id, null);
		
		String info = ActionUtils.getInfo(tmp.getClass());
		
		if(ActionUtils.isDeprecated(tmp.getClass()))
			info = "[RED]DEPRECATED[]\n" + info;
		
		setInfo(info);

		if (e == null || tmp == null || !(e.getClass().getName().equals(tmp.getClass().getName())))
			e = tmp;

		if (e != null) {
			Param[] params = ActionUtils.getParams(e);

			i = new InputPanel[params.length];

			for (int j = 0; j < params.length; j++) {
				if (params[j].options instanceof Enum[]) {
					i[j] = InputPanelFactory.createInputPanel(getSkin(), params[j].name, params[j].desc, params[j].type,
							params[j].mandatory, params[j].defaultValue, (Enum[]) params[j].options);
				} else {
					i[j] = InputPanelFactory.createInputPanel(getSkin(), params[j].name, params[j].desc, params[j].type,
							params[j].mandatory, params[j].defaultValue, (String[]) params[j].options);
				}

				addInputPanel(i[j]);

				if ((i[j].getField() instanceof TextField && params[j].name.toLowerCase().endsWith("text"))
						|| i[j].getField() instanceof ScrollPane) {
					i[j].getCell(i[j].getField()).fillX();
				}
			}
		} else {
			i = new InputPanel[0];
		}
	}

	@Override
	protected void inputsToModel(boolean create) {
		for (int j = 0; j < i.length; j++) {
			String v = i[j].getText();
			try {
				if (i[j].getTitle().toLowerCase().endsWith("text")) {

					String key = ActionUtils.getStringValue(e, i[j].getTitle());

					if (scope.equals(ScopePanel.WORLD_SCOPE)) {
						if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX)
							key = Ctx.project.getI18N().genKey(null, null, parent.getHashKey(), pos, i[j].getTitle());

						Ctx.project.getI18N().setWorldTranslation(key, v);
					} else if (scope.equals(ScopePanel.SCENE_SCOPE)) {
						if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX)
							key = Ctx.project.getI18N().genKey(Ctx.project.getSelectedScene().getId(), null,
									parent.getHashKey(), pos, i[j].getTitle());

						Ctx.project.getI18N().setTranslation(key, v);
					} else {
						if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX)
							key = Ctx.project.getI18N().genKey(Ctx.project.getSelectedScene().getId(),
									Ctx.project.getSelectedActor().getId(), parent.getHashKey(), pos, i[j].getTitle());

						Ctx.project.getI18N().setTranslation(key, v);
					}

					if (v != null && !v.isEmpty())
						v = key;
					else
						v = null;
				}

				ActionUtils.setParam(e, i[j].getTitle(), v);

			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
		}

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		for (int j = 0; j < i.length; j++) {

			try {
				String v = ActionUtils.getStringValue(e, i[j].getTitle());

				if (scope.equals(ScopePanel.WORLD_SCOPE))
					v = Ctx.project.getI18N().getWorldTranslation(v);
				else
					v = Ctx.project.translate(v);

				i[j].setText(v);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
		}
	}
}
