package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditToolbar;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class OptionTreePanel extends javax.swing.JPanel {

	BaseDocument doc;

	Element dialog;
	Element actor;

	Element clipboard;

	private EditToolbar editToolbar;

	private javax.swing.JScrollPane jScrollPane;
	private javax.swing.JTree tree;

	private JButton upBtn;
	private JButton downBtn;
	private JButton leftBtn;
	private JButton rightBtn;

	public OptionTreePanel() {
		editToolbar = new EditToolbar();

		jScrollPane = new javax.swing.JScrollPane();
		tree = new javax.swing.JTree();
		tree.setRootVisible(false);

		setLayout(new java.awt.BorderLayout());

		add(editToolbar, BorderLayout.PAGE_START);
		add(jScrollPane, BorderLayout.CENTER);

		jScrollPane.setViewportView(tree);

		upBtn = new JButton();
		downBtn = new JButton();
		leftBtn = new JButton();
		rightBtn = new JButton();

		editToolbar.addToolBarButton(upBtn, "/res/images/ic_up.png", "Move up", "Move up");
		editToolbar.addToolBarButton(downBtn, "/res/images/ic_down.png", "Move down", "Move down");
		editToolbar.addToolBarButton(leftBtn, "/res/images/ic_left.png", "Child",
				"Move to child option");
		editToolbar.addToolBarButton(rightBtn, "/res/images/ic_right.png", "Parent",
				"Move to parent option");

		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				TreePath selectionPath = tree.getSelectionPath();

				if (selectionPath == null) {
					upBtn.setEnabled(false);
					downBtn.setEnabled(false);

					leftBtn.setEnabled(false);
					rightBtn.setEnabled(false);
				} else {

					OptionNode lastSel = (OptionNode) selectionPath.getLastPathComponent();

					upBtn.setEnabled(lastSel.getParent().getChildAt(0) != lastSel);
					downBtn.setEnabled(lastSel.getParent().getChildAt(
							lastSel.getParent().getChildCount() - 1) != lastSel);

					leftBtn.setEnabled(selectionPath.getPathCount() > 2);
					rightBtn.setEnabled(lastSel.getParent().getChildAt(0) != lastSel);
				}
				
				editToolbar.enableEdit(selectionPath != null);
			}
		});

		tree.setCellRenderer(treeCellRenderer);
		tree.setModel(new DefaultTreeModel(new OptionNode(dialog)));

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

		leftBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				left();
			}
		});

		rightBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				right();
			}
		});
	}

	public void addOptions(BaseDocument doc, Element a, Element dialog) {
		this.dialog = dialog;
		this.actor = a;
		this.doc = doc;

		tree.setModel(new DefaultTreeModel(new OptionNode(dialog)));

		editToolbar.enableCreate(dialog != null);
		if(tree.getRowCount() > 0)
			tree.setSelectionRow(0);
	}

	private void create() {
		TreePath path = tree.getSelectionPath();
		
		Element parent = dialog;
		
		OptionNode parentOption = null;
		
		if(path != null) {
			parentOption = (OptionNode) ((OptionNode) path.getLastPathComponent())
				.getParent();
			parent = parentOption.getElement();
		} else {
			parentOption = (OptionNode) tree.getModel().getRoot();
		}

		CreateEditDialogOptionDialog o = new CreateEditDialogOptionDialog(Ctx.window, doc,
				parent, null);
		o.setVisible(true);

		if (!o.isCancel()) {
//			addOptions(doc, actor, dialog);
			OptionNode newOption = new OptionNode(o.getElement());
			
			int idx = -1;
			if(path != null)
				idx = parentOption.getIndex((OptionNode) path.getLastPathComponent());
			parentOption.insert(newOption, idx + 1);
			
			((DefaultTreeModel) tree.getModel()).reload(parentOption);
			
			tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(newOption)));
		}
	}

	private void edit() {
		TreePath path = tree.getSelectionPath();
		OptionNode sel = (OptionNode) path.getLastPathComponent();
		OptionNode parent = (OptionNode) sel.getParent();

		CreateEditDialogOptionDialog o = new CreateEditDialogOptionDialog(Ctx.window, doc,
				parent.getElement(), sel.getElement());
		o.setVisible(true);

		if (!o.isCancel()) {
			((DefaultTreeModel) tree.getModel()).nodeChanged(sel);
		}
	}

	private void delete() {
		TreePath path = tree.getSelectionPath();

		OptionNode on = (OptionNode) path.getLastPathComponent();
		
		OptionNode parent = (OptionNode) on.getParent();

		doc.deleteElement(on.getElement());

		clipboard = on.getElement();
		editToolbar.enablePaste(true);
		
//		addOptions(doc, actor, dialog);
		
		TreeNode childBefore = parent.getChildBefore(on);
		TreeNode childAfter = parent.getChildAfter(on);
		
		on.removeFromParent();
		
		((DefaultTreeModel) tree.getModel()).reload(parent);
		
		TreePath nextPath = null;
		
		if(childBefore != null) {
			nextPath = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(childBefore));
		} else {
			nextPath = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(childAfter));
		}
				
		tree.expandPath(nextPath);
		tree.setSelectionPath(nextPath);
	}

	private void copy() {
		TreePath path = tree.getSelectionPath();

		OptionNode on = (OptionNode) path.getLastPathComponent();
		Element e = on.getElement();

		clipboard = (Element) e.cloneNode(true);
		editToolbar.enablePaste(true);
	}

	private void paste() {
		Element newElement = (Element) clipboard.cloneNode(true);

		TreePath path = tree.getSelectionPath();
		
		Element parent = dialog;
		
		OptionNode parentOption;
		
		if(path != null) {
			parentOption = (OptionNode) ((OptionNode) path.getLastPathComponent())
				.getParent();
			parent = parentOption.getElement();
		} else {
			parentOption = (OptionNode) tree.getModel().getRoot();
		}
		
		parent.appendChild(newElement);		
		doc.setModified(newElement);
		
		
		OptionNode newOption = new OptionNode(newElement);
		
		int idx = -1;
		if(path != null)
			idx = parentOption.getIndex((OptionNode) path.getLastPathComponent());
		parentOption.insert(newOption, idx + 1);
		
		((DefaultTreeModel) tree.getModel()).reload(parentOption);
		
		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(newOption)));
	}

	private void up() {
		TreePath path = tree.getSelectionPath();
		OptionNode on = (OptionNode) path.getLastPathComponent();
		
		Element e = on.getElement();
		
		Node n = e.getPreviousSibling();
		
		while(!(n instanceof Element)){
			n = n.getPreviousSibling();
		}
		
		Element e2 = (Element)n;

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);
		doc.setModified(e);
		
//		addOptions(doc, actor, dialog);
		
		OptionNode parentOption = (OptionNode) on.getParent();
		
		int idx = parentOption.getIndex(on);
		parentOption.insert(on, idx - 1);
		
		((DefaultTreeModel) tree.getModel()).reload(parentOption);
		
		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));
	}

	private void down() {
		TreePath path = tree.getSelectionPath();
		OptionNode on = (OptionNode) path.getLastPathComponent();
		
		Element e = on.getElement();
		Node n = e.getNextSibling();
		
		while(!(n instanceof Element)){
			n = n.getNextSibling();
		}
		
		Element e2 = (Element)n;	

		Node parent = e.getParentNode();
		parent.removeChild(e2);
		parent.insertBefore(e2, e);
		doc.setModified(e);
		
		OptionNode parentOption = (OptionNode) on.getParent();
		
		int idx = parentOption.getIndex(on);
		parentOption.insert(on, idx + 1);
		
		((DefaultTreeModel) tree.getModel()).reload(parentOption);
		
		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));
	}

	private void left() {
		TreePath path = tree.getSelectionPath();
		OptionNode on = (OptionNode) path.getLastPathComponent();
		Element e = on.getElement();
		
		Node parent = e.getParentNode();
		parent.removeChild(e);
		
		Node grandpa = parent.getParentNode();
		grandpa.replaceChild(e, parent);		
		grandpa.insertBefore(parent, e);
		doc.setModified(e);
		
		OptionNode parentOption = (OptionNode) on.getParent();
		
		OptionNode grandpaOption = (OptionNode) parentOption.getParent();
		
		int idx = grandpaOption.getIndex(parentOption);
		grandpaOption.insert(on, idx + 1);
		
		((DefaultTreeModel) tree.getModel()).reload(grandpaOption);
		
		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));		
	}

	private void right() {
		TreePath path = tree.getSelectionPath();
		OptionNode on = (OptionNode) path.getLastPathComponent();
		Element e = on.getElement();
		
		Node n = e.getPreviousSibling();
		
		while(!(n instanceof Element)){
			n = n.getPreviousSibling();
		}
		
		Element e2 = (Element)n;
		
		Node parent = e.getParentNode();
		parent.removeChild(e);
		e2.appendChild(e);
		
		doc.setModified(e);
		
		OptionNode parentOption = (OptionNode)((OptionNode) on.getParent()).getChildBefore(on);
		
		OptionNode grandpaOption = (OptionNode) on.getParent();
		
		parentOption.add(on);
		
		((DefaultTreeModel) tree.getModel()).reload(grandpaOption);
		
		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));		
	}

	// -------------------------------------------------------------------------
	// TreeCellRenderer
	// -------------------------------------------------------------------------
	private final TreeCellRenderer treeCellRenderer = new TreeCellRenderer() {
		private final JPanel panel = new JPanel(new BorderLayout());
		private final JPanel txtPanel = new JPanel(new BorderLayout());
		private final JLabel nameLabel = new JLabel();
		private final JLabel infoLabel = new JLabel();
		// private final ImagePreviewPanel imgPanel = new ImagePreviewPanel();
		private final JLabel imgPanel = new JLabel();

		{
			txtPanel.setOpaque(false);
			txtPanel.add(nameLabel, BorderLayout.CENTER);
			txtPanel.add(infoLabel, BorderLayout.SOUTH);

			panel.setBorder(new EmptyBorder(5, 3, 5, 5));
			panel.add(txtPanel, BorderLayout.CENTER);
			panel.setBackground(Color.DARK_GRAY);
			panel.add(imgPanel, BorderLayout.WEST);

			infoLabel.setForeground(Color.LIGHT_GRAY);

			Font font = nameLabel.getFont();
			nameLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));

			font = infoLabel.getFont();
			infoLabel.setFont(new Font(font.getName(), font.getStyle(), 10));

			imgPanel.setBorder(new EmptyBorder(0, 0, 0, 4));
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {

			if (!(value instanceof OptionNode))
				return panel;

			Element e = ((OptionNode) value).getElement();

			if (e == null)
				return panel;

			String text = e.getAttribute("text");

			nameLabel.setText(Ctx.project.getSelectedScene().getTranslation(text));

			StringBuilder sb = new StringBuilder();

			// if(!actor.isEmpty())
			// sb.append(" actor '").append(actor).append("'");

			NamedNodeMap attr = e.getAttributes();

			String response = e.getAttribute("response_text");

			if (!response.isEmpty())
				sb.append("<html><b>R: ")
						.append(Ctx.project.getSelectedScene().getTranslation(response))
						.append("</b></html> ");

			for (int i = 0; i < attr.getLength(); i++) {
				Node n = attr.item(i);
				String name = n.getNodeName();

				if (name.equals("text") || name.equals("response_text"))
					continue;

				String v = n.getNodeValue();
				sb.append(name).append(':')
						.append(Ctx.project.getSelectedScene().getTranslation(v)).append(' ');
			}

			infoLabel.setText(sb.toString());

			URL u = null;

			if (!leaf) {
				if (expanded) {
					u = getClass().getResource("/res/images/ic_open.png");
					imgPanel.setIcon(new javax.swing.ImageIcon(u));
				} else {
					u = getClass().getResource("/res/images/ic_closed.png");
					imgPanel.setIcon(new javax.swing.ImageIcon(u));
				}
			} else
				imgPanel.setIcon(null);

			panel.setOpaque(selected);
			return panel;
		}
	};

	// TREE MODEL
	public class OptionNode extends DefaultMutableTreeNode {
		Element e;

		public OptionNode(Element e) {
			this.e = e;

			if (e == null)
				return;

			NodeList childs = e.getChildNodes();

			int n = childs.getLength();

			for (int i = 0; i < n; i++) {
				if (childs.item(i) instanceof Element)
					add(new OptionNode((Element) childs.item(i)));
			}
		}

		public String toString() {
			if (e == null)
				return "";

			return e.getAttribute("text");
		}

		public Element getElement() {
			return e;
		}
	}
}
