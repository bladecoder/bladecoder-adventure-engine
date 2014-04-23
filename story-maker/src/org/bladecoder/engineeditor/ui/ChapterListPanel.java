package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListModel;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ChapterListPanel extends ElementListPanel {
	
	private JButton initBtn;
	
	public ChapterListPanel() {
		super(true);
		list.setCellRenderer(listCellRenderer);
		
		initBtn = new JButton();
		editToolbar.addToolBarButton(initBtn, "/res/images/ic_check.png", "Set init chapter", "Set init chapter");
		initBtn.setDisabledIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/res/images/ic_check_disabled.png")));
		
		initBtn.setEnabled(true);
		
		initBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDefault();
			}
		});
		
	}	
	
	private void setDefault() {
		WorldDocument w = (WorldDocument) doc;

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		String id = lm.getElementAt(pos).getAttribute("id");
		
		w.setRootAttr("init_chapter", id);
		
		list.repaint();
	}
	
	@Override
	protected void delete() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		
		Element e = lm.getElementAt(pos);
		
		if(e.getElementsByTagName("scene").getLength() > 0) {
			String msg = "The chapter will not be deleted. You must delete all scenes from the chapter first\n\n";
			JOptionPane.showMessageDialog(Ctx.window, msg);
			
			return;
		}
		
		if(lm.getSize() < 2) {
			String msg = "The chapter will not be deleted, at least one chapter must exists\n\n";
			JOptionPane.showMessageDialog(Ctx.window, msg);
			
			return;
		}
		
		super.delete();
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditChapterDialog(Ctx.window, doc, parent, e);
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(false) {

		@Override
		public String getName(Element e) {
			String id = e.getAttribute("id");
			
			String init = ((WorldDocument)doc).getInitChapter();
			
			if(init.equals(id))
				id += " <init>";
			
			return id;
		}

		@Override
		public String getInfo(Element e) {
			return e.getElementsByTagName("scene").getLength() + " scenes";
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			return null;
		}
	};
}
