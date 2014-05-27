package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.Ctx;
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
		toolbar.hideCopyPaste();

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

	@Override
	protected void delete() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		if (list.getItems().size < 2) {
			String msg = "The chapter will not be deleted, at least one chapter must exists";
			Ctx.msg.show(getStage(), msg, 3);

			return;
		}
		
		Element e = list.getItems().removeIndex(pos);

		try {
			((WorldDocument)doc).removeChapter(e.getAttribute("id"));
		} catch (Exception ex) {
			String msg = "Something went wrong while deleting the chapter.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(), msg, 3);

			ex.printStackTrace();
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

	};
}
