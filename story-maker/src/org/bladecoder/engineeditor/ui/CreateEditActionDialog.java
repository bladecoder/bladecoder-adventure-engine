package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JComboBox;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionFactory;
import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditActionDialog extends CreateEditElementDialog {
	private InputPanel actionPanel;
	private InputPanel actorPanel;
	private InputPanel classPanel;

	private InputPanel parameters[];	

	@SuppressWarnings("unchecked")
	public CreateEditActionDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);

		String[] actions = ActionFactory.getActionList();
		Arrays.sort(actions);

		actionPanel = new InputPanel("Action",
				"<html>Select the action to create.</html>", actions);
		actorPanel = new InputPanel("Target Actor",
				"<html>Select the target actor id. Default is current actor.</html>");
		classPanel = new InputPanel("Class",
				"<html>Select the class for the custom action.</html>");

		setAction();

		((JComboBox<String>) actionPanel.getField())
				.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
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

		centerPanel.removeAll();
		centerPanel.add(actionPanel);
		centerPanel.add(actorPanel);

		Action ac = null;
		
		if (id.equals("action")) {
			centerPanel.add(classPanel);
			// TODO
		} else {
			ac = ActionFactory.create(id, null);
		}

			
		if (ac != null) {
			setInfo(ac.getInfo());

			Param[] params = ac.getParams();

			parameters = new InputPanel[params.length];

			for (int i = 0; i < params.length; i++) {
				parameters[i] = new InputPanel(params[i].name, params[i].desc,
						params[i].type, params[i].mandatory, params[i].defaultValue, params[i].options);
				centerPanel.add(parameters[i]);
			}
			
			i = parameters;
			a = getAttrs();
		}
	}

	@Override
	protected void create() {
//		e = doc.createElement(parent, type);
	}

	@Override
	protected void fill() {
		String act = actionPanel.getText();
		String actor = actorPanel.getText().trim();

//		((Node)e).setNodeValue(act);
		e = doc.createElement(parent, act);

		if (!actor.isEmpty())
			e.setAttribute("actor", actor);
		else
			e.removeAttribute("actor");

		super.fill();
	}
}
