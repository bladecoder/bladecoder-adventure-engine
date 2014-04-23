package org.bladecoder.engineeditor.ui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public abstract class ElementListPanel extends javax.swing.JPanel {

	protected BaseDocument doc;
	protected Element parent;

	protected Element clipboard;

	protected EditToolbar editToolbar;

	protected javax.swing.JScrollPane jScrollPane;
	protected javax.swing.JList<Element> list;

	public ElementListPanel(boolean sorted) {
		editToolbar = new EditToolbar();

		jScrollPane = new javax.swing.JScrollPane();
		list = new javax.swing.JList<Element>();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		editToolbar.setAlignmentX(LEFT_ALIGNMENT);
		jScrollPane.setAlignmentX(LEFT_ALIGNMENT);

		add(editToolbar);
		add(jScrollPane);

		jScrollPane.setViewportView(list);
		jScrollPane.setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR, 1));

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				editToolbar.enableEdit(pos != -1);
			}
		});

		list.setModel(new ElementListModel(sorted));

		editToolbar.addCreateActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				create();
			}
		});
		editToolbar.addEditActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				edit();
			}
		});
		editToolbar.addDeleteActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});

		editToolbar.addCopyActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copy();
			}
		});
		editToolbar.addPasteActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				paste();
			}
		});
	}

	public void addElements(BaseDocument doc, Element parent, String tag) {
		this.doc = doc;
		this.parent = parent;

		ElementListModel lm = (ElementListModel) list.getModel();
		lm.clear();
		list.clearSelection();

		if (parent != null) {

			NodeList nl;
			
			if(tag == null)
				nl = parent.getChildNodes();
			else {
//				nl = parent.getElementsByTagName(tag);
				nl = doc.getChildrenByTag(parent, tag);
			}

			for (int i = 0; i < nl.getLength(); i++) {
				if(nl.item(i) instanceof Element)
					lm.addElement((Element) nl.item(i));
			}			
		}
		
		if (lm.getSize() > 0)
			list.setSelectedIndex(0);

		editToolbar.enableCreate(parent != null);
	}

	protected void create() {
		CreateEditElementDialog dialog = getCreateEditElementDialogInstance(null);
		dialog.setVisible(true);

		if (!dialog.isCancel()) {
			ElementListModel lm = (ElementListModel) list.getModel();
			Element s = dialog.getElement();
			lm.addElement(s);
			list.setSelectedValue(s, true);
		}
	}

	protected void edit() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		Element e = list.getModel().getElementAt(pos);

		CreateEditElementDialog dialog = getCreateEditElementDialogInstance(e);
		dialog.setVisible(true);

		if (!dialog.isCancel()) {
			list.repaint();
			doc.setModified(e);
		}
	}

	protected abstract CreateEditElementDialog getCreateEditElementDialogInstance(Element e);

	protected void delete() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		Element e = lm.getElementAt(pos);
		lm.removeElement(e);

		doc.deleteElement(e);

		clipboard = e;
		editToolbar.enablePaste(true);

		if (pos > 0)
			list.setSelectedIndex(pos - 1);
		else if (pos == 0 && lm.getSize() > 0)
			list.setSelectedIndex(0);
		else
			list.clearSelection();
	}

	protected void copy() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		Element e = lm.getElementAt(pos);

		clipboard = (Element) e.cloneNode(true);
		editToolbar.enablePaste(true);
	}

	protected void paste() {
		Element newElement = doc.cloneNode(clipboard);

		parent.appendChild(newElement);

		if (newElement.getAttribute("id") != null && !newElement.getAttribute("id").isEmpty()) {
			doc.setId(newElement, newElement.getAttribute("id"));
		}

		ElementListModel lm = (ElementListModel) list.getModel();
		lm.addElement(newElement);
		list.setSelectedValue(newElement, true);
		doc.setModified(newElement);
	}
}
