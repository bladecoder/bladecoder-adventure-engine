package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class FAList extends ElementList {
	
	private ImageButton initBtn;
	
	public FAList(Skin skin) {
		super(skin, true);
		
		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check", "Set init scene", "Set init scene");
		initBtn.setDisabled(true);
		
		setCellRenderer(listCellRenderer);

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				String id = null;

				if (pos != -1)
					id = list.getItems().get(pos).getAttribute("id");

				Ctx.project.setSelectedFA(id);
				
				toolbar.disableEdit(pos== -1);
				initBtn.setDisabled(pos== -1);
			}
		});
		
		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}
		});
	}
	
	private void setDefault() {
		ChapterDocument scn = (ChapterDocument) doc;

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		String id = list.getItems().get(pos).getAttribute("id");
//		String prev = w.getRootAttr("init_scene");
		
		scn.setRootAttr((Element)list.getItems().get(pos).getParentNode(), "init_frame_animation", id);
	}	

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditFADialog(skin, doc, parent, e);
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			String name =  e.getAttribute("id");
			Element actor = (Element)e.getParentNode();
			
			String init = actor.getAttribute("init_frame_animation");
			
			if(init == null || init.isEmpty()) {
				Node n = actor.getFirstChild();
				while(!(n instanceof Element))
					n = n.getNextSibling();
				
				init = ((Element)n).getAttribute("id");
			}
			
			if(init.equals(name))
				name += " <init>";
			
			return name;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			String source = e.getAttribute("source");
			String speed = e.getAttribute("speed");
			String delay = e.getAttribute("delay");
			String count = e.getAttribute("count");

			StringBuilder sb = new StringBuilder();

			if (!source.isEmpty())
				sb.append("source: ").append(source);
			if (!speed.isEmpty())
				sb.append(" speed: ").append(speed);
			if (!delay.isEmpty())
				sb.append(" delay: ").append(delay);
			if (!count.isEmpty())
				sb.append(" count: ").append(count);
			
			
			return sb.toString();
		}

		@Override
		public TextureRegion getCellImage(Element e) {
			String u = null;	

			if (e.getAttribute("animation_type").equalsIgnoreCase("repeat")) {
				u = "ic_repeat";
			} else if (e.getAttribute("animation_type").equalsIgnoreCase("yoyo")) {
				u = "ic_yoyo";
			} else {
				u = "ic_sprite_actor";
			}
			
			return  Ctx.assetManager.getIcon(u);
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
