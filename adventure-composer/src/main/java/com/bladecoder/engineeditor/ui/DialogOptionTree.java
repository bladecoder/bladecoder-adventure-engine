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
package com.bladecoder.engineeditor.ui;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.EditTree;
import com.bladecoder.engineeditor.utils.I18NUtils;

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
		
		if(tree.getRootNodes().size > 0)
			tree.getSelection().add(tree.getRootNodes().first());
	}

	@Override
	public void create() {
		Element parent = dialog;
		
		if(!tree.getSelection().isEmpty()) {
			Node sel = tree.getSelection().first();

			if(sel.getParent() != null)
				parent = (Element)sel.getParent().getObject();
		}

		EditDialogOptionDialog o = new EditDialogOptionDialog(skin, doc,
				parent, null);
		o.show(getStage());
		
		o.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Node sel = null;
				
				if(!tree.getSelection().isEmpty())
					sel = tree.getSelection().first();
							
				Element e = ((EditElementDialog)actor).getElement();
				
				if(sel != null) {
					Element prev = (Element)sel.getObject();
					
					org.w3c.dom.Node parent = e.getParentNode();
					parent.removeChild(e);
				
					parent.replaceChild(e, prev);		
					parent.insertBefore(prev, e);
//					doc.setModified(e);
				}
				
				
				Node n = createNode(e);

				if(sel != null) {
					Node p = sel.getParent();

					// add in the selected position
					if( p != null) {					
						int pos = p.getChildren().indexOf(sel, true);
						p.insert(pos + 1, n);
						
//						p.add(n);
					} else {
						int pos = tree.getRootNodes().indexOf(sel, true);
						tree.insert(pos + 1, n);
						
//						tree.add(n);
					}
				} else {
					tree.add(n);
				}
				
				tree.getSelection().clear();
				tree.getSelection().add(n);
			}			
		});
	}

	@Override
	public void edit() {
		Node sel = tree.getSelection().first();
		
		Element parent = dialog;
				
		if(sel.getParent() != null) {
			parent = (Element)sel.getParent().getObject();
		}
		
		EditDialogOptionDialog o = new EditDialogOptionDialog(skin, doc,
				parent, (Element)sel.getObject());
		o.show(getStage());
		
		o.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Node sel = tree.getSelection().first();
				
				updateNode(sel);
			}		
		});
	}

	@Override
	public void delete() {
		Node sel = tree.getSelection().first();
		Element selElement = (Element)sel.getObject();
		Node parentNode = sel.getParent();
		Array<Node> siblings = getSiblings();
		int pos = siblings.indexOf(sel, true);

		doc.deleteElement(selElement);

		clipboard = selElement;
		I18NUtils.putTranslationsInElement(doc, clipboard);
		toolbar.disablePaste(false);
			
		Node childBefore = pos <= 0? null: siblings.get(pos - 1);
		Node childAfter = pos >= siblings.size ? null: siblings.get(pos +1);
		
		tree.remove(sel);
		
		Node nextPath = null;
		
		// Set NEXT SELECTION	
		if(childBefore != null) {
			nextPath = childBefore;
		} else if(childAfter != null) {
			nextPath = childAfter;
		} else if(parentNode != null) {
			nextPath = parentNode;
		}
				
		tree.getSelection().clear();
		
		if(nextPath != null)
			tree.getSelection().add(nextPath);
		else if(tree.getRootNodes().size > 0)
			tree.getSelection().add(tree.getRootNodes().first());
	}

	@Override
	public void copy() {
		Node sel = tree.getSelection().first();
		Element selElement = (Element)sel.getObject();

		clipboard = (Element) selElement.cloneNode(true);
		I18NUtils.putTranslationsInElement(doc, clipboard);
		toolbar.disablePaste(false);
	}

	@Override
	public void paste() {
		Element newElement = (Element) clipboard.cloneNode(true);
		Node sel = tree.getSelection().first();
		
		Element parent = dialog;
		
		Node parentNode = null;		
		if(sel != null) {
			parentNode = sel.getParent();
		}
		
		parent.appendChild(newElement);		
		doc.setModified(newElement);
		I18NUtils.extractStrings(doc, newElement);
		
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
		tree.getSelection().clear();
		tree.getSelection().add(newOption);
	}

	@Override
	public void upNode() {
		
		Node sel = tree.getSelection().first();
		Element e = (Element)sel.getObject();
		
		org.w3c.dom.Node n = e.getPreviousSibling();
		
		while(!(n instanceof Element)){
			n = n.getPreviousSibling();
		}
		
		Element e2 = (Element)n;

		org.w3c.dom.Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);
		doc.setModified(e);
		
		Node p = sel.getParent();
		
		if(p == null) {
			int pos = tree.getRootNodes().indexOf(sel, true);
			tree.remove(sel);
			tree.insert(pos - 1, sel);
		} else {
			int pos = p.getChildren().indexOf(sel, true);
			p.remove(sel);
			p.insert(pos - 1, sel);			
		}
		
		
		tree.getSelection().clear();
		tree.getSelection().add(sel);
	}

	@Override
	public void downNode() {
		
		Node sel = tree.getSelection().first();
		Element e = (Element)sel.getObject();
		
		org.w3c.dom.Node n = e.getNextSibling();
		
		while(!(n instanceof Element)){
			n = n.getNextSibling();
		}		
		
		
		Element e2 = (Element)n;	

		org.w3c.dom.Node parent = e.getParentNode();
		parent.removeChild(e2);
		parent.insertBefore(e2, e);
		doc.setModified(e);
		
		Node p = sel.getParent();
		
		if(p == null) {
			int pos = tree.getRootNodes().indexOf(sel, true);
			tree.remove(sel);
			tree.insert(pos + 1, sel);
		} else {
			int pos = p.getChildren().indexOf(sel, true);
			p.remove(sel);
			p.insert(pos + 1, sel);			
		}
		
		tree.getSelection().clear();
		tree.getSelection().add(sel);
	}

	@Override
	public void leftNode() {
		Node sel = tree.getSelection().first();
		Element e = (Element)sel.getObject();
		
		org.w3c.dom.Node parent = e.getParentNode();
		parent.removeChild(e);
		
		org.w3c.dom.Node grandpa = parent.getParentNode();
		grandpa.replaceChild(e, parent);		
		grandpa.insertBefore(parent, e);
		doc.setModified(e);
		
		Node p = sel.getParent();		
		Node grandpaOption = p.getParent();
		
		tree.remove(sel);
		if(grandpaOption != null) {
			int idx = grandpaOption.getChildren().indexOf(p, true);
			grandpaOption.insert(idx + 1, sel);
		} else {
			int idx = tree.getRootNodes().indexOf(p, true);
			tree.insert(idx + 1, sel);			
		}
		
		tree.getSelection().clear();
		tree.getSelection().add(sel);		
	}

	@Override
	public void rightNode() {
		Node sel = tree.getSelection().first();
		Element e = (Element)sel.getObject();
		
		org.w3c.dom.Node n = e.getPreviousSibling();
		
		while(!(n instanceof Element)){
			n = n.getPreviousSibling();
		}
		
		Element e2 = (Element)n;
		
		org.w3c.dom.Node parent = e.getParentNode();
		parent.removeChild(e);
		e2.appendChild(e);
		
		doc.setModified(e);
		
		Node grandpaOption = sel.getParent();
		Node parentOption = null;
		
		if(grandpaOption != null) {
			int idx = grandpaOption.getChildren().indexOf(sel, true);
			parentOption = grandpaOption.getChildren().get(idx - 1);
		} else {
			int idx = tree.getRootNodes().indexOf(sel, true);
			parentOption = tree.getRootNodes().get(idx - 1);
		}
		
		tree.remove(sel);
		parentOption.add(sel);
		
		tree.getSelection().clear();
		tree.getSelection().add(sel);	
	}

	
	private void createTree(Element dialog) {
		NodeList childs = dialog.getChildNodes();
		int n = childs.getLength();
		for (int i = 0; i < n; i++) {
			if (childs.item(i) instanceof Element)
				tree.add(createNode((Element) childs.item(i)));
		}
	}
	
	private void updateNode(Node node) {
		Element e = (Element)node.getObject();
		VerticalGroup vg = (VerticalGroup)node.getActor();
		
		Label textLbl =	(Label)vg.getChildren().get(0);
		Label infoLbl = (Label)vg.getChildren().get(1);

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
	}
	
	private Node createNode(Element e) {
		Label textLbl = new Label(null, skin);
		Label infoLbl = new Label(null, skin, "subtitle");

		String text = e.getAttribute(XMLConstants.TEXT_ATTR);

		textLbl.setText(Ctx.project.getSelectedChapter().getTranslation(text));

		StringBuilder sb = new StringBuilder();

		// if(!actor.isEmpty())
		// sb.append(" actor '").append(actor).append("'");

		NamedNodeMap attr = e.getAttributes();

		String response = e.getAttribute(XMLConstants.RESPONSE_TEXT_ATTR);

		if (!response.isEmpty())
			sb.append("R: ")
					.append(Ctx.project.getSelectedChapter().getTranslation(response)).append(' ');

		for (int i = 0; i < attr.getLength(); i++) {
			org.w3c.dom.Node n = attr.item(i);
			String name = n.getNodeName();

			if (name.equals(XMLConstants.TEXT_ATTR) || name.equals(XMLConstants.RESPONSE_TEXT_ATTR))
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
