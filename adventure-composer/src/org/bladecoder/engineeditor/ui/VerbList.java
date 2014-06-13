package org.bladecoder.engineeditor.ui;

import java.text.MessageFormat;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.GdxRuntimeException;


public class VerbList extends ElementList {

	private ActionList actionList;

	public VerbList(Skin skin) {
		super(skin, true);
		actionList = new ActionList(skin);
		
//		addActor(actionList);
		row();
		add(actionList).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addActions();
			}
		});

		list.setCellRenderer(listCellRenderer);
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditVerbDialog(skin, doc, parent, e);
	}

	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		super.addElements(doc, parent, tag);
		addActions();
	}

	private void addActions() {
		int pos = list.getSelectedIndex();

		Element v = null;

		if (pos != -1) {
			v = list.getItems().get(pos);
		}

		actionList.addElements(doc, v, "action");
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			return e.getAttribute("id");
		}

		@Override
		protected String getCellSubTitle(Element e) {
			String state = e.getAttribute("state");
			String target = e.getAttribute("target");

			StringBuilder sb = new StringBuilder();

			if (!state.isEmpty())
				sb.append("when ").append(state);
			if (!target.isEmpty())
				sb.append(" with target '").append(target).append("'");

			return sb.toString();
		}

		@Override
		public TextureRegion getCellImage(Element e) {
			String iconName = MessageFormat.format("ic_{0}", e.getAttribute("id"));
			TextureRegion image = null;
			
			try {
				image = Ctx.assetManager.getIcon(iconName);
			} catch(GdxRuntimeException e1) {
				image = Ctx.assetManager.getIcon("ic_custom");
			}

			return image;
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
		
		@Override
		protected boolean hasImage() {
			return true;
		}
	};
}
