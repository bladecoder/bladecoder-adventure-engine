package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditChapterDialog extends CreateEditElementDialog {
	
	private InputPanel[] inputs = {
		new InputPanel("Chapter ID", "<html>The id of the chapter</html>", true),
	};


	String attrs[] = {"id"};	

	public CreateEditChapterDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);

		setInfo("An adventure game is composed of chapters. Chapters contains scenes.");

		init(inputs, attrs, doc, parent, "chapter", e);
	}
}
