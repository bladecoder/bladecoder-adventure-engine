package org.bladecoder.engineeditor.ui.components;

import java.text.MessageFormat;

import org.bladecoder.engineeditor.model.BaseDocument;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

public abstract class EditElementDialog extends EditDialog {
	
	protected Element e;
	protected Element parent;
	protected String type;
	protected BaseDocument doc;
	
	protected InputPanel[] i;
	protected String a[];
	
    protected ChangeListener listener;

	public EditElementDialog(Skin skin) {
		super("", skin);
	}
	
	protected void init(InputPanel[] inputs, String attrs[], BaseDocument doc, Element parent, String type, Element e) {
		this.i = inputs;
		this.a = attrs;
		this.e = e;
		this.doc = doc;
		this.parent = parent;
		this.type = type;
		
		for (InputPanel i : inputs) {
			addInputPanel(i);
		}
		
		if (e == null) {
			setTitle("CREATE " + type.toUpperCase());
		} else {
			String s = e.getAttribute("id");
			
			if(s==null||s.isEmpty()) s = e.getTagName();
				
			setTitle(MessageFormat.format("EDIT ELEMENT ''{0}''", s));
			
			for (int pos = 0; pos < attrs.length; pos++) {			
				InputPanel i = inputs[pos];
				i.setText(e.getAttribute(attrs[pos]));
			}
		}
		
		// TODO Set focus to the 1st element		
//		if(inputs.length > 0) {
//			getStage().setKeyboardFocus(inputs[0].getField());
//		}
	}

	@Override
	protected void ok() {

		if (e == null) {
			create();
		}
		
		fill();
		
		if(listener != null)
			listener.changed(new ChangeEvent(), this);
	}
	
	public void setListener(ChangeListener l) {
		listener = l;
	}
	
	protected void create() {
		e = doc.createElement(parent, type);
	}

	protected void fill() {
		for (int j = 0; j < a.length; j++) {
			InputPanel input = i[j];
			
			if (!input.getText().isEmpty() && input.isVisible()) {
				if(a[j].equals("id")) {
					doc.setId(e, input.getText());
				} else {
					e.setAttribute(a[j], input.getText());
				}
			} else {
				e.removeAttribute(a[j]);
			}
			
			
		}
		
		doc.setModified(e);
	}
	
	public Element getElement() {
		return e;
	}

	@Override
	protected boolean validateFields() {

		for (InputPanel p : i) {
			if (p.isVisible() && !p.validateField())
				return false;
		}

		return true;
	}
}
