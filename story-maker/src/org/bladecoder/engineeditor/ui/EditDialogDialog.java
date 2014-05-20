package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditDialogDialog extends EditElementDialog {
	public static final String INFO = "Actors can have several dialogs defined. Dialogs have a tree of options to choose";
	
	private InputPanel[] inputs; 

	String attrs[] = { "id"};	

	public EditDialogDialog(Skin skin,  BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel[1];
		
		inputs[0] = new InputPanel(skin, "Dialog ID",
				"<html>Select the dialog id to create.</html>", true);

		setInfo(INFO);

		init(inputs, attrs, doc, parent, "dialog", e);
	}
}
