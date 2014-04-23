package org.bladecoder.engineeditor.ui;

import javax.swing.ImageIcon;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class SoundListPanel extends ElementListPanel {	
	
	public SoundListPanel() {
		super(true);
		list.setCellRenderer(listCellRenderer);		
	}	

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditSoundDialog(Ctx.window, doc, parent, e);
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(false) {

		@Override
		public String getName(Element e) {
			String id  = e.getAttribute("id");

			return id;
		}

		@Override
		public String getInfo(Element e) {

			String filename = e.getAttribute("filename");
			String loop = e.getAttribute("loop");
			String volume = e.getAttribute("volume");

			StringBuilder sb = new StringBuilder();

			if (!filename.isEmpty())
				sb.append("filename: ").append(filename);
			if (!loop.isEmpty())
				sb.append(" loop: ").append(loop);
			if (!volume.isEmpty())
				sb.append(" volume: ").append(volume);
			
			return sb.toString();
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			return null;
		}
	};
}
