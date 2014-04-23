package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditDialogDialog extends CreateEditElementDialog {
	public static final String INFO = "Actors can have several dialogs defined. Dialogs have a tree of options to choose";
	
	private InputPanel[] inputs = {
			new InputPanel("Dialog ID",
					"<html>Select the dialog id to create.</html>", true)
	};


	String attrs[] = { "id"};	

	public CreateEditDialogDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);
		
		inputs[0].setMandatory(true);

		setInfo(INFO);

		init(inputs, attrs, doc, parent, "dialog", e);
	}
}
