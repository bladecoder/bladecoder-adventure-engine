package com.bladecoder.engineeditor.undo;

import org.w3c.dom.Element;


public class UndoDeleteElement implements UndoOp {
	private Element e;
	private Element parent;
	
	
	public UndoDeleteElement(Element e) {

		this.e = e;
		this.parent = (Element)e.getParentNode();
	}
	
	@Override
	public void undo() {
//		parent.appendChild(e);
//		doc.setModified(e, this);
	}
}
