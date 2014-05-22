package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.glcanvas.Assets;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;



public class DialogList extends ElementList {	
	
    private DialogOptionTree options;

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditDialogDialog(skin, doc, parent, e);
	}
	
    public DialogList(Skin skin) {
    	super(skin, true);
    	
    	options = new DialogOptionTree(skin);
    	row();
    	add(options).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();
				
				Element v = null;
				
				if(pos != -1) {
					v = list.getItems().get(pos);
					options.addOptions(doc, parent, v);
				} else {
					options.addOptions(doc, parent, null);
				}
				
				toolbar.disableEdit(pos == -1);
			}
		});
		
		list.setCellRenderer(listCellRenderer);
    }
    
    
	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		options.addOptions(doc, null, null);
		super.addElements(doc, parent, tag);
    }	


	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			return e.getAttribute("id");
		}

//		@Override
//		protected String getCellSubTitle(Element e) {
//			String state = e.getAttribute("state");
//			String target = e.getAttribute("target");
//			
//			StringBuilder sb = new StringBuilder();
//			
//			if(!state.isEmpty()) sb.append("when ").append(state);
//			if(!target.isEmpty()) sb.append(" with target '").append(target).append("'");
//			
//			return sb.toString();
//		}

		@Override
		public TextureRegion getCellImage(Element e) {
			return new TextureRegion(Assets.inst().get("res/images/ic_talkto.png", Texture.class));
		}
		
		@Override
		protected boolean hasSubtitle() {
			return false;
		}
		
		@Override
		protected boolean hasImage() {
			return true;
		}
	};

}
