package org.bladecoder.engineeditor.ui;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditChapterDialog extends EditElementDialog {

	private InputPanel[] inputs = new InputPanel[1];

	String attrs[] = { "id" };

	String previousId = null;

	public EditChapterDialog(Skin skin, BaseDocument doc, Element parent,
			Element e) {
		super(skin);

		inputs[0] = new InputPanel(skin, "Chapter ID", "The id of the chapter",
				true);

		setInfo("An adventure game is composed of chapters. Chapters contains scenes.");

		init(inputs, attrs, doc, parent, "chapter", e);

		if (e != null)
			previousId = e.getAttribute("id");
	}

	@Override
	protected void create() {
		try {
			e = ((WorldDocument) doc).createChapter(inputs[0].getText());
		} catch (FileNotFoundException | TransformerException
				| ParserConfigurationException e) {
			String msg = "Something went wrong while creating the chapter.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(), msg, 2);

			e.printStackTrace();
		}
	}

	@Override
	protected void fill() {

		super.fill();

		if (previousId != null && !previousId.equals(e.getAttribute("id"))) {

			try {
				((WorldDocument) doc).renameChapter(previousId, e.getAttribute("id"));
			} catch (TransformerException | ParserConfigurationException
					| SAXException | IOException e1) {
				String msg = "Something went wrong while renaming the chapter.\n\n"
						+ e1.getClass().getSimpleName()
						+ " - "
						+ e1.getMessage();
				Ctx.msg.show(getStage(), msg, 3);

				e1.printStackTrace();
			}
		}
	}

}
