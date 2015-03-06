package com.bladecoder.engineeditor.undo;

import org.w3c.dom.Element;

import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.utils.I18NUtils;

public class UndoSetAttr implements UndoOp {
	private BaseDocument doc;
	private Element e;
	private String attr;
	private String value;
	
	
	public UndoSetAttr(BaseDocument doc, Element e, String attr, String value) {
		this.doc = doc;
		this.e = e;
		this.attr = attr;
		this.value = value;
	}
	
	@Override
	public void undo() {
		I18NUtils.setI18NAttr(doc, e, attr, value);
		doc.setModified(attr, e);
	}
}
