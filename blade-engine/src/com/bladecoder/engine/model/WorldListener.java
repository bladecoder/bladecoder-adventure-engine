package com.bladecoder.engine.model;

public interface WorldListener {
	public void cutMode(boolean value);
	public void text(Text t);
	public void dialogOptions();
	public void inventoryEnabled(boolean value);
	public void pause(boolean value);	
}
