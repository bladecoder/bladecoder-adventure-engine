package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditChapterDialog extends EditElementDialog {
	
	private InputPanel[] inputs = new InputPanel[1];

	String attrs[] = {"id"};	

	public EditChapterDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs[0] = new InputPanel(skin, "Chapter ID", "<html>The id of the chapter</html>", true);

		setInfo("An adventure game is composed of chapters. Chapters contains scenes.");

		init(inputs, attrs, doc, parent, "chapter", e);
	}
}
