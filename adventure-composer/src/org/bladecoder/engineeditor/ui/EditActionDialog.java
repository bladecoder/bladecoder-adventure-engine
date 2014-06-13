package org.bladecoder.engineeditor.ui;

import java.util.Arrays;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionFactory;
import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class EditActionDialog extends EditElementDialog {
	private static final String CUSTOM_INFO="Custom action definition";
	
	private InputPanel actionPanel;
	private InputPanel actorPanel;
	private InputPanel classPanel;
	private InputPanel customParams;

	private InputPanel parameters[];	

	@SuppressWarnings("unchecked")
	public EditActionDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);

		String[] actions = ActionFactory.getActionList();
		Arrays.sort(actions);
		String[] actions2 = new String[actions.length + 1];
		System.arraycopy(actions, 0, actions2, 0, actions.length);		
		actions2[actions2.length - 1] = "CUSTOM ACTION";

		actionPanel = new InputPanel(skin, "Action",
				"Select the action to create.", actions2);
		
		actorPanel = new InputPanel(skin, "Target Actor",
				"Select the target actor id. Default is current actor.");
		classPanel = new InputPanel(skin, "Class",
				"Select the class for the custom action.", true);
		customParams = new InputPanel(skin, "Params",
				"Write the params for the custom action.");		

		setAction();

		((SelectBox<String>) actionPanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						setAction();
					}
				});

		if(e != null) {
			actionPanel.setText(e.getTagName());
			actorPanel.setText(e.getAttribute("actor"));
			classPanel.setText(e.getAttribute("class"));
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
		
		if (id.equals("CUSTOM ACTION")) {
			addInputPanel(classPanel);
			addInputPanel(customParams);
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
		NamedNodeMap attributes = e.getAttributes();
		int l = attributes.getLength();
		for(int i = 0; i < l ; i++)
			e.removeAttribute(attributes.item(i).getLocalName());

		if (!actor.isEmpty())
			e.setAttribute("actor", actor);
		
		String id = actionPanel.getText();
		
		if (id.equals("CUSTOM ACTION")) {
			e.setAttribute("class", classPanel.getText());
			// TODO Add parameters
		} else {
			e.setAttribute("action_name", id);
			
			super.fill();
		}
	}
}
