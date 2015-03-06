package com.bladecoder.engineeditor.undo;

import org.w3c.dom.Element;

import com.bladecoder.engineeditor.model.BaseDocument;

public class UndoAddElement implements UndoOp {
	private BaseDocument doc;
	private Element e;
	
	
	public UndoAddElement(BaseDocument doc, Element e) {
		this.doc = doc;
		this.e = e;
	}
	
	@Override
	public void undo() {
		doc.deleteElement(e);
	}
}
