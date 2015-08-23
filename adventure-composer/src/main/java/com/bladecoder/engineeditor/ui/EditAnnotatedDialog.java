package com.bladecoder.engineeditor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;
import com.bladecoder.engineeditor.utils.ModelUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EditAnnotatedDialog<T> extends EditElementDialog {
	private Map<String, Class<?>> typeChildren;

	public EditAnnotatedDialog(Skin skin, Class<T> modelClass, BaseDocument doc, Element parent, String type, Element e) {
		super(skin);

		initFromParams(modelClass, doc, parent, type, e);
	}

	private void initFromParams(Class<T> modelClass, BaseDocument doc, Element parent, String type, Element e) {
		final Optional<Map<String, Class<?>>> typeChoiceChildren = ModelUtils.getModelClassChoiceChildren(modelClass);
		final List<Param> params;

		if (!typeChoiceChildren.isPresent()) {
			setInfo(ModelUtils.getInfo(modelClass));
			params = ModelUtils.getParams(modelClass);
		} else {
			typeChildren = typeChoiceChildren.get();

			final Class<?> firstModelClass = typeChildren.values().iterator().next();

			setInfo(ModelUtils.getInfo(firstModelClass));
			List<Param> modelParams = ModelUtils.getParams(firstModelClass);

			final List<Param> newParams = new ArrayList<>();
			final Set<String> keys = typeChoiceChildren.get().keySet();
			final String[] keyOptions = keys.toArray(new String[keys.size()]);

			newParams.add(new Param("type", "Type", "There are multiple " + type + " to choose from", Param.Type.OPTION, true, null, keyOptions, null));
			newParams.addAll(modelParams);

			params = newParams;
		}

		Collection<InputPanel> inputs = ModelUtils.getInputsFromModelClass(params, getSkin());

		if (typeChoiceChildren.isPresent()) {
			InputPanel typePanel = inputs.iterator().next();
			typePanel.getField()
					.addListener(new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							typeChanged(typePanel);
						}
					});
		}
		final List<String> attrs = params.stream().map(Param::getId).collect(Collectors.toList());

		init(inputs, attrs, doc, parent, type, e);
	}

	private void typeChanged(InputPanel typePanel) {
		OptionsInputPanel optionsInputPanel = (OptionsInputPanel) typePanel;
		int i = optionsInputPanel.getSelectedIndex();
		String id = typePanel.getText();
		setInfo(ModelUtils.getInfo(typeChildren.get(id)));
	}
}
