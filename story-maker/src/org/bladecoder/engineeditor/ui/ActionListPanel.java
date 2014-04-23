package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListModel;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings("serial")
public class ActionListPanel extends ElementListPanel {

	private JButton upBtn;
	private JButton downBtn;

	public ActionListPanel() {
		super(false);

		upBtn = new JButton();
		downBtn = new JButton();

		editToolbar.addToolBarButton(upBtn, "/res/images/ic_up.png", "Move up", "Move up");
		editToolbar.addToolBarButton(downBtn, "/res/images/ic_down.png", "Move down", "Move down");

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				editToolbar.enableEdit(pos != -1);
				upBtn.setEnabled(pos != -1 && pos != 0);
				downBtn.setEnabled(pos != -1 && pos != list.getModel().getSize() - 1);
			}
		});

		list.setCellRenderer(listCellRenderer);

		upBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				up();
			}
		});
		downBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				down();
			}
		});
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditActionDialog(Ctx.window, doc, parent, e);
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		Element e = lm.getElementAt(pos);
		Element e2 = lm.getElementAt(pos - 1);

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);

		lm.remove(pos);
		lm.insertElementAt(e, pos - 1);
		list.setSelectedIndex(pos - 1);

		doc.setModified(e);
	}

	private void down() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == list.getModel().getSize() - 1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		Element e = lm.getElementAt(pos);
		Element e2 = pos + 2 < lm.getSize() ? lm.getElementAt(pos + 2) : null;

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);

		
		lm.remove(pos);
		lm.insertElementAt(e, pos + 1);
		list.setSelectedIndex(pos + 1);

		doc.setModified(e);
	}

	@Override
	protected void edit() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		Element e = list.getModel().getElementAt(pos);

		CreateEditElementDialog dialog = getCreateEditElementDialogInstance(e);
		dialog.setVisible(true);

		if (!dialog.isCancel()) {
			ElementListModel lm = (ElementListModel) list.getModel();
			Element s = dialog.getElement();
			parent.replaceChild(s,e);
			lm.insertElementAt(s, pos);
			lm.removeElement(e);

			list.repaint();
			doc.setModified(e);
		}
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(false) {

		@Override
		public String getName(Element e) {
			String id = e.getTagName();

			String actor = e.getAttribute("actor");

			if (!actor.isEmpty())
				id = MessageFormat.format("{1}.{0}", id, actor);
			
			return id;
		}

		@Override
		public String getInfo(Element e) {
			StringBuilder sb = new StringBuilder("<html>");

			NamedNodeMap attr = e.getAttributes();

			for (int i = 0; i < attr.getLength(); i++) {
				Node n = attr.item(i);
				String name = n.getNodeName();

				if (name.equals("actor"))
					continue;

				String v = n.getNodeValue();

				sb.append(name).append(" <b>").append(doc.getTranslation(v))
						.append("</b>  ");
			}
			
			sb.append("</html>");
			
			return sb.toString();
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			return null;
		}
	};	
}
