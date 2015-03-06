package com.bladecoder.engineeditor.undo;

import org.w3c.dom.Element;

import com.bladecoder.engineeditor.model.BaseDocument;

public class UndoDeleteElement implements UndoOp {
	private BaseDocument doc;
	private Element e;
	private Element parent;
	
	
	public UndoDeleteElement(BaseDocument doc, Element e) {
		this.doc = doc;
		this.e = e;
		this.parent = (Element)e.getParentNode();
	}
	
	@Override
	public void undo() {
		parent.appendChild(e);
		doc.setModified(e, this);
	}
}
