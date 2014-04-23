package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditVerbDialog extends CreateEditElementDialog {
	public static final String VERBS[] = { "lookat", "pickup", "talkto", "use", "leave", "init",
			"test", "custom" };
	
	public static final String VERBS_INFO[] = {
			"<html><center>Called when the user clicks<br/> in the 'lookat' icon<br/> over a object in scene</center></html>",
			"<html><center>Called when the user clicks<br/> in the 'pickup' icon<br/> over a object in scene</center></html>",
			"<html><center>Called when the user clicks<br/> in the 'talkto' icon<br/> over a character in scene</center></html>",
			"<html><center>Called when the user drags and drops<br/> an inventory object over<br/> an object in scene or in inventory</center></html>",
			"<html><center>Called when the user clicks<br/> in an exit zone in scene</center></html>",
			"<html><center>Called every time<br/> that the scene is loaded</center></html>",
			"<html><center>Called every time<br/> that the scene is loaded in test mode.<br/>'test' verb is called before the 'init' verb</center></html>",
			"<html><center>User defined verbs can be called<br/> from dialogs or inside actions using <br/>the 'run_verb' action</center></html>" };

	
	private InputPanel[] inputs = {
			new InputPanel("Verb ID", "<html>Select the verb to create.</html>", VERBS),
			new InputPanel("State", "<html>Select the state.</html>"),
			new InputPanel("Target Actor", "<html>Select the target actor id for the 'use' verb</html>"),
			new InputPanel("Custom Verb Name", "<html>Select the Custom verb id</html>"),
		};


	String attrs[] = { "id", "state", "target"};		
	
	@SuppressWarnings("unchecked")
	public CreateEditVerbDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);

		setInfo(VERBS_INFO[0]);

		((JComboBox<String>) inputs[0].getField()).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String id = (String) inputs[0].getText();
				int i = inputs[0].getSelectedIndex();

				setInfo(VERBS_INFO[i]);

				if (id.equals("use"))
					inputs[2].setVisible(true);
				else
					inputs[2].setVisible(false);

				if (id.equals("custom"))
					inputs[3].setVisible(true);
				else
					inputs[3].setVisible(false);
			}
		});

		inputs[2].setVisible(false);
		inputs[3].setVisible(false);
		
		init(inputs, attrs, doc, parent, "verb", e);
		
		if(e != null) {
			boolean isCustom = true;
			String id = e.getAttribute("id");
			
			for(String v:VERBS) {
				if(v.equals(id) && !id.equals("custom")) {
					isCustom = false;
					break;
				}
			}
			
			if(isCustom) {
				inputs[0].setText("custom");
				inputs[3].setVisible(true);
				inputs[3].setText(id);
			}
		}
	}

	@Override
	protected boolean validateFields() {
		boolean isOk = true;
		
		if(inputs[0].getText().equals("custom") && inputs[3].getText().isEmpty()) {
			inputs[3].setError(true);
			isOk = false;
		} else {
			inputs[3].setError(false);
		}
				
		return isOk;
	}
	
	@Override
	protected void fill() {
		for (int j = 0; j < a.length; j++) {
			InputPanel input = i[j];
			
			if (!input.getText().isEmpty()) {
				e.setAttribute(a[j], input.getText());
			} else {
				e.removeAttribute(a[j]);
			}
		}
		
		if(e.getAttribute("id").equals("custom"))
			e.setAttribute("id", inputs[3].getText());
	}
}
