package org.bladecoder.engineeditor.ui;

import java.io.FileNotFoundException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ChapterList extends ElementList {

	private ImageButton initBtn;

	public ChapterList(Skin skin) {
		super(skin, true);

		list.setCellRenderer(listCellRenderer);

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check",
				"Set init chapter", "Set init chapter");

		initBtn.setDisabled(false);

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}
		});

	}

	private void setDefault() {
		WorldDocument w = (WorldDocument) doc;

		Element e = list.getSelected();

		if (e == null)
			return;

		String id = e.getAttribute("id");

		w.setRootAttr(doc.getElement(), "init_chapter", id);
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(
			Element e) {
		return new EditChapterDialog(skin, doc, parent, e);
	}
	
	// TODO Override create and edit!!

	@Override
	protected void delete() {

		Element e = list.getSelected();

		if (e == null)
			return;

		if (list.getItems().size < 2) {
			String msg = "The chapter will not be deleted, at least one chapter must exists\n\n";
			Ctx.msg.show(getStage(), msg, 2000);

			return;
		}

		try {
			Ctx.project.getWorld().removeChapter(doc.getRootAttr(e, "id"));
		} catch (Exception ex) {
			String msg = "Something went wrong while deleting the scene.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(), msg, 2000);

			ex.printStackTrace();
		}

		super.delete();
	}

	@Override
	protected void paste() {
		WorldDocument w = (WorldDocument) doc;

		ChapterDocument scn;
		try {
			scn = w.createChapter(clipboard.getAttribute("id"));
			String id = scn.getId();

			Element newElement = scn.cloneNode(clipboard);

			scn.getDocument().replaceChild(newElement, scn.getElement());
			// scn.getDocument().appendChild(newElement);
			scn.setModified(true);

			newElement.setAttribute("id", id);

			list.getItems().add(newElement);
			list.setSelectedIndex(list.getItems().indexOf(newElement, true));
			doc.setModified(newElement);
		} catch (FileNotFoundException | TransformerException
				| ParserConfigurationException e) {
			String msg = "Something went wrong while pasting the scene.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(), msg, 2000);

			e.printStackTrace();
		}

	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			String id = e.getAttribute("id");

			String init = ((WorldDocument) doc).getInitChapter();

			if (init.equals(id))
				id += " <init>";

			return id;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			return e.getElementsByTagName("scene").getLength() + " scenes";
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}

	};
}
