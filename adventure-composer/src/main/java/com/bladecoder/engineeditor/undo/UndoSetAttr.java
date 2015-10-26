package com.bladecoder.engineeditor.undo;

import org.w3c.dom.Element;

import com.bladecoder.engineeditor.model.I18NHandler;

public class UndoSetAttr implements UndoOp {
	private Element e;
	private String attr;
	private String value;
	
	
	public UndoSetAttr( Element e, String attr, String value) {
		this.e = e;
		this.attr = attr;
		this.value = value;
	}
	
	@Override
	public void undo() {
//		I18NHandler.setI18NAttr(doc, e, attr, value);
//		doc.setModified(attr, e);
	}
}
