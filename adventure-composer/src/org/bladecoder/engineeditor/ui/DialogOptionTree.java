/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.EditTree;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class DialogOptionTree extends EditTree {

	BaseDocument doc;

	Element dialog;
	Element actor;

	Element clipboard;

	public DialogOptionTree(Skin skin) {
		super(skin);
	}

	public void addOptions(BaseDocument doc, Element a, Element dialog) {
		this.dialog = dialog;
		this.actor = a;
		this.doc = doc;

		tree.clearChildren();
		
		if(dialog != null)
			createTree(dialog);

		toolbar.disableCreate(dialog == null);
		
//		if(tree.getRootNodes().size > 0)
//			tree.getSelection().add(tree.getRootNodes().first());
	}

	@Override
	public void create() {
		Node sel = tree.getSelection().getLastSelected();
		
		Element parent = dialog;
				
		if(sel != null) {
			parent = (Element)sel.getParent().getObject();
		}

		EditDialogOptionDialog o = new EditDialogOptionDialog(skin, doc,
				parent, null);
		o.show(getStage());
		
		o.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Node sel = tree.getSelection().getLastSelected();
							
				Element e = ((EditElementDialog)actor).getElement();
				
				Node n = createNode(e);
				if(sel != null) {
					sel.add(n);
				} else {
					tree.add(n);
				}
			}			
		});
	}

	@Override
	public void edit() {
		Node sel = tree.getSelection().getLastSelected();
		
		Element parent = dialog;
				
		if(sel != null) {
			parent = (Element)sel.getParent().getObject();
		}
		
		EditDialogOptionDialog o = new EditDialogOptionDialog(skin, doc,
				parent, (Element)sel.getObject());
		o.show(getStage());
	}

	@Override
	public void delete() {
		Node sel = tree.getSelection().getLastSelected();
		Element selElement = (Element)sel.getObject();
		Node parent = sel.getParent();
		Element parentElement = (Element)parent.getObject();

		doc.deleteElement(selElement);

		clipboard = selElement;
		toolbar.disablePaste(false);
			
//		Node childBefore = parent.getChildBefore(on);
//		Node childAfter = parent.getChildAfter(on);
		
		parent.remove(sel);
		
		Node nextPath = null;
		
		// TODO Set NEXT SELECTION
		
//		if(childBefore != null) {
//			nextPath = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(childBefore));
//		} else {
//			nextPath = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(childAfter));
//		}
//				
//		tree.expandPath(nextPath);
//		tree.setSelectionPath(nextPath);
	}

	@Override
	public void copy() {
		Node sel = tree.getSelection().getLastSelected();
		Element selElement = (Element)sel.getObject();

		clipboard = (Element) selElement.cloneNode(true);
		toolbar.disablePaste(false);
	}

	@Override
	public void paste() {
		Element newElement = (Element) clipboard.cloneNode(true);
		Node sel = tree.getSelection().getLastSelected();
		
		Element parent = dialog;
		
		Node parentNode = null;		
		if(sel != null) {
			parentNode = sel.getParent();
		}
		
		parent.appendChild(newElement);		
		doc.setModified(newElement);
		
		
		Node newOption = createNode(newElement);
		
		if(parentNode != null)
			parentNode.add(newOption);
		else tree.add(newOption);
		
		// TODO Insert in the selected position
//		int idx = -1;
//		if(path != null)
//			idx = parentOption.getIndex((OptionNode) path.getLastPathComponent());
//		parentOption.insert(newOption, idx + 1);
//		
//		((DefaultTreeModel) tree.getModel()).reload(parentOption);
//		
//		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(newOption)));
	}

	@Override
	public void upNode() {
//		TreePath path = tree.getSelectionPath();
//		OptionNode on = (OptionNode) path.getLastPathComponent();
//		
//		Element e = on.getElement();
//		
//		Node n = e.getPreviousSibling();
//		
//		while(!(n instanceof Element)){
//			n = n.getPreviousSibling();
//		}
//		
//		Element e2 = (Element)n;
//
//		Node parent = e.getParentNode();
//		parent.removeChild(e);
//		parent.insertBefore(e, e2);
//		doc.setModified(e);
//		
////		addOptions(doc, actor, dialog);
//		
//		OptionNode parentOption = (OptionNode) on.getParent();
//		
//		int idx = parentOption.getIndex(on);
//		parentOption.insert(on, idx - 1);
//		
//		((DefaultTreeModel) tree.getModel()).reload(parentOption);
//		
//		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));
	}

	@Override
	public void downNode() {
//		TreePath path = tree.getSelectionPath();
//		OptionNode on = (OptionNode) path.getLastPathComponent();
//		
//		Element e = on.getElement();
//		Node n = e.getNextSibling();
//		
//		while(!(n instanceof Element)){
//			n = n.getNextSibling();
//		}
//		
//		Element e2 = (Element)n;	
//
//		Node parent = e.getParentNode();
//		parent.removeChild(e2);
//		parent.insertBefore(e2, e);
//		doc.setModified(e);
//		
//		OptionNode parentOption = (OptionNode) on.getParent();
//		
//		int idx = parentOption.getIndex(on);
//		parentOption.insert(on, idx + 1);
//		
//		((DefaultTreeModel) tree.getModel()).reload(parentOption);
//		
//		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));
	}

	@Override
	public void leftNode() {
//		TreePath path = tree.getSelectionPath();
//		OptionNode on = (OptionNode) path.getLastPathComponent();
//		Element e = on.getElement();
//		
//		Node parent = e.getParentNode();
//		parent.removeChild(e);
//		
//		Node grandpa = parent.getParentNode();
//		grandpa.replaceChild(e, parent);		
//		grandpa.insertBefore(parent, e);
//		doc.setModified(e);
//		
//		OptionNode parentOption = (OptionNode) on.getParent();
//		
//		OptionNode grandpaOption = (OptionNode) parentOption.getParent();
//		
//		int idx = grandpaOption.getIndex(parentOption);
//		grandpaOption.insert(on, idx + 1);
//		
//		((DefaultTreeModel) tree.getModel()).reload(grandpaOption);
//		
//		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));		
	}

	@Override
	public void rightNode() {
//		TreePath path = tree.getSelectionPath();
//		OptionNode on = (OptionNode) path.getLastPathComponent();
//		Element e = on.getElement();
//		
//		Node n = e.getPreviousSibling();
//		
//		while(!(n instanceof Element)){
//			n = n.getPreviousSibling();
//		}
//		
//		Element e2 = (Element)n;
//		
//		Node parent = e.getParentNode();
//		parent.removeChild(e);
//		e2.appendChild(e);
//		
//		doc.setModified(e);
//		
//		OptionNode parentOption = (OptionNode)((OptionNode) on.getParent()).getChildBefore(on);
//		
//		OptionNode grandpaOption = (OptionNode) on.getParent();
//		
//		parentOption.add(on);
//		
//		((DefaultTreeModel) tree.getModel()).reload(grandpaOption);
//		
//		tree.setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(on)));		
	}

	
	private void createTree(Element dialog) {
		NodeList childs = dialog.getChildNodes();
		int n = childs.getLength();
		for (int i = 0; i < n; i++) {
			if (childs.item(i) instanceof Element)
				tree.add(createNode((Element) childs.item(i)));
		}
	}
	
	private Node createNode(Element e) {
		Label textLbl = new Label(null, skin);
		Label infoLbl = new Label(null, skin);

		String text = e.getAttribute("text");

		textLbl.setText(Ctx.project.getSelectedChapter().getTranslation(text));

		StringBuilder sb = new StringBuilder();

		// if(!actor.isEmpty())
		// sb.append(" actor '").append(actor).append("'");

		NamedNodeMap attr = e.getAttributes();

		String response = e.getAttribute("response_text");

		if (!response.isEmpty())
			sb.append("R: ")
					.append(Ctx.project.getSelectedChapter().getTranslation(response)).append(' ');

		for (int i = 0; i < attr.getLength(); i++) {
			org.w3c.dom.Node n = attr.item(i);
			String name = n.getNodeName();

			if (name.equals("text") || name.equals("response_text"))
				continue;

			String v = n.getNodeValue();
			sb.append(name).append(':')
					.append(Ctx.project.getSelectedChapter().getTranslation(v)).append(' ');
		}
		
		infoLbl.setText(sb.toString());
				
		VerticalGroup vg = new VerticalGroup();
		vg.left();
		vg.addActor(textLbl);
		vg.addActor(infoLbl);
		
		Node node = new Node(vg);
		node.setObject(e);
		
		NodeList childs = e.getChildNodes();
		int n = childs.getLength();

		for (int i = 0; i < n; i++) {
			if (childs.item(i) instanceof Element) {
				node.add(createNode((Element) childs.item(i)));				
			}
		}
		
		
		return node;		
	}
}
