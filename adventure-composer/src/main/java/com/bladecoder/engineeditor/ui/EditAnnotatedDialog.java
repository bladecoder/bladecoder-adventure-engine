package com.bladecoder.engineeditor.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.utils.ModelUtils;
import org.w3c.dom.Element;

import java.util.List;
import java.util.stream.Collectors;

public class EditAnnotatedDialog<T> extends EditElementDialog {
	public EditAnnotatedDialog(Skin skin, Class<T> modelClass, BaseDocument doc, Element parent, String type, Element e) {
		super(skin);

		initFromParams(modelClass, doc, parent, type, e);
	}

	private void initFromParams(Class<T> modelClass, BaseDocument doc, Element parent, String type, Element e) {
		setInfo(ModelUtils.getInfo(modelClass));
		List<Param> params = ModelUtils.getParams(modelClass);
		List<InputPanel> inputs = ModelUtils.getInputsFromModelClass(params, getSkin());
		List<String> attrs = params.stream().map(Param::getId).collect(Collectors.toList());
		inputs.forEach(this::addInputPanel);

		init(inputs, attrs, doc, parent, type, e);
	}
}
