package org.bladecoder.engineeditor.ui;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditDialogOptionDialog extends EditElementDialog {

	private InputPanel[] inputs;

	String attrs[] = { "text", "response_text", "verb", "next", "visible" };

	public EditDialogOptionDialog(Skin skin, BaseDocument doc,
			Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel[5];
		
		inputs[0] = new InputPanel(skin, "Text", "<html>The sentence of the dialog to say by the player</html>");
		inputs[1] = new InputPanel(skin, "Response Text", "<html>The response by the character</html>");
		inputs[2] = new InputPanel(skin, "Verb", "<html>The verb to execute when choosing this option</html>");
		inputs[3] = new InputPanel(skin, "Next Option",
						"<html>The next option to show when this option is selected</html>");
		inputs[4] = new InputPanel(skin, "Visible", "<html>The visibility</html>", Param.Type.BOOLEAN, false);

		setInfo("A dialog is composed of an option tree. Each option is a dialog sentence that the user can choose to say");

		init(inputs, attrs, doc, parent, "option", e);
	}
}
