package org.bladecoder.engineeditor.ui;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditDialogOptionDialog extends CreateEditElementDialog {

	private InputPanel[] inputs = {
			new InputPanel("Text", "<html>The sentence of the dialog to say by the player</html>"),
			new InputPanel("Response Text", "<html>The response by the character</html>"),
			new InputPanel("Verb", "<html>The verb to execute when choosing this option</html>"),
			new InputPanel("Next Option",
					"<html>The next option to show when this option is selected</html>"),
			new InputPanel("Visible", "<html>The visibility</html>", Param.Type.BOOLEAN, false) };

	String attrs[] = { "text", "response_text", "verb", "next", "visible" };

	public CreateEditDialogOptionDialog(java.awt.Frame parentWindow, BaseDocument doc,
			Element parent, Element e) {
		super(parentWindow);

		setInfo("A dialog is composed of an option tree. Each option is a dialog sentence that the user can choose to say");

		init(inputs, attrs, doc, parent, "option", e);
	}
}
