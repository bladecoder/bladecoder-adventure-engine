package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class EditVerbDialog extends EditElementDialog {
	public static final String VERBS[] = { "lookat", "pickup", "talkto", "use", "leave", "init",
			"test", "custom" };
	
	public static final String VERBS_INFO[] = {
			"Called when the user clicks\n in the 'lookat' icon\n over a object in scene",
			"Called when the user clicks\n in the 'pickup' icon\n over a object in scene",
			"Called when the user clicks\n in the 'talkto' icon\n over a character in scene",
			"Called when the user drags and drops\n an inventory object over\n an object in scene or in inventory",
			"Called when the user clicks\n in an exit zone in scene",
			"Called every time\n that the scene is loaded",
			"Called every time\n that the scene is loaded in test mode.\n'test' verb is called before the 'init' verb",
			"User defined verbs can be called\n from dialogs or inside actions using \nthe 'run_verb' action" };

	
	private InputPanel[] inputs;


	String attrs[] = { "id", "state", "target"};		
	
	@SuppressWarnings("unchecked")
	public EditVerbDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel [4];
		inputs[0] = new InputPanel(skin, "Verb ID", "Select the verb to create.", VERBS);
		inputs[1] = new InputPanel(skin, "State", "Select the state.");
		inputs[2] = new InputPanel(skin, "Target Actor", "Select the target actor id for the 'use' verb");
		inputs[3] = new InputPanel(skin, "Custom Verb Name", "Select the Custom verb id");

		setInfo(VERBS_INFO[0]);

		((SelectBox<String>) inputs[0].getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String id = (String) inputs[0].getText();
				int i = inputs[0].getSelectedIndex();

				setInfo(VERBS_INFO[i]);

				if (id.equals("use"))
					setVisible(inputs[2],true);
				else
					setVisible(inputs[2],false);

				if (id.equals("custom"))
					setVisible(inputs[3],true);
				else
					setVisible(inputs[3],false);
			}

		});
		
		init(inputs, attrs, doc, parent, "verb", e);
		
		setVisible(inputs[2],false);
		setVisible(inputs[3],false);
		
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
				setVisible(inputs[3],true);
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
