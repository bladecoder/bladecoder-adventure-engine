package org.bladecoder.engine.model;

import java.util.ArrayList;

public class DialogOption {
	ArrayList<DialogOption> options = new ArrayList<DialogOption>();
	
	transient private DialogOption parent;
		
	private String text;
	private String responseText;
	private String verbId;
	private String next;
	private boolean visible = true;
	
	
	public void addOption(DialogOption o) {
		options.add(o);
	}


	public boolean isVisible() {
		return visible;
	}


	public void setVisible(boolean visible) {
		this.visible = visible;
	}


	public String getVerbId() {
		return verbId;
	}


	public void setVerbId(String verbId) {
		this.verbId = verbId;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public DialogOption getParent() {
		return parent;
	}


	public void setParent(DialogOption parent) {
		this.parent = parent;
	}


	public ArrayList<DialogOption> getOptions() {
		return options;
	}


	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}
	
	public String getResponseText() {
		return responseText;
	}


	public String getNext() {
		return next;
	}


	public void setNext(String next) {
		this.next = next;
	}

}
