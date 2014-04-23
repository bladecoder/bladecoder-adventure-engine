package org.bladecoder.engineeditor.ui;

import javax.swing.ImageIcon;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;



@SuppressWarnings("serial")
public class DialogListPanel extends ElementListPanel {	
	
    private OptionTreePanel options;

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditDialogDialog(Ctx.window, doc, parent, e);
	}
	
    public DialogListPanel() {
    	super(true);
    	options = new OptionTreePanel();
 
    	options.setAlignmentX(LEFT_ALIGNMENT);
        
        add(options);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();
				
				Element v = null;
				
				if(pos != -1) {
					v = list.getModel().getElementAt(pos);
					options.addOptions(doc, parent, v);
				} else {
					options.addOptions(doc, parent, null);
				}
				
				editToolbar.enableEdit(pos != -1);
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
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(true) {

		@Override
		public String getName(Element e) {
			return e.getAttribute("id");
		}

		@Override
		public String getInfo(Element e) {
			String state = e.getAttribute("state");
			String target = e.getAttribute("target");
			
			StringBuilder sb = new StringBuilder();
			
			if(!state.isEmpty()) sb.append("when ").append(state);
			if(!target.isEmpty()) sb.append(" with target '").append(target).append("'");
			
			return sb.toString();
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			return new ImageIcon(getClass().getResource("/res/images/ic_talkto.png"));
		}
	};

}
