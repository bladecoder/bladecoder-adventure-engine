package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListModel;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("serial")
public class FAListPanel extends ElementListPanel {
	
	private JButton initBtn;
	
	public FAListPanel() {
		super(true);
		
		initBtn = new JButton();
		editToolbar.addToolBarButton(initBtn, "/res/images/ic_check.png", "Set init scene", "Set init scene");
		initBtn.setEnabled(false);
		
		list.setCellRenderer(listCellRenderer);	

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				String id = null;

				if (pos != -1)
					id = list.getModel().getElementAt(pos).getAttribute("id");

				Ctx.project.setSelectedFA(id);
				
				editToolbar.enableEdit(pos!= -1);
				initBtn.setEnabled(pos!= -1);
			}
		});
		
		initBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDefault();
			}
		});
	}
	
	private void setDefault() {
		ChapterDocument scn = (ChapterDocument) doc;

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		String id = lm.getElementAt(pos).getAttribute("id");
//		String prev = w.getRootAttr("init_scene");
		
		scn.setRootAttr((Element)lm.getElementAt(pos).getParentNode(), "init_frame_animation", id);
		
		list.repaint();
	}	

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditFADialog(Ctx.window, doc, parent, e);
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(true) {

		@Override
		public String getName(Element e) {
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
		public String getInfo(Element e) {
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
		public ImageIcon getImageIcon(Element e) {
			URL u = null;	

			if (e.getAttribute("animation_type").equalsIgnoreCase("repeat")) {
				u = getClass().getResource(
						"/res/images/ic_repeat.png");
			} else if (e.getAttribute("animation_type").equalsIgnoreCase("yoyo")) {
				u = getClass().getResource(
						"/res/images/ic_yoyo.png");
			} else {
				u = getClass().getResource(
						"/res/images/ic_sprite_actor.png");
			}
			
			return new ImageIcon(u);
		}
	};
}
