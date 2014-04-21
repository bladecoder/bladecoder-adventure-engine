package org.bladecoder.engine.actions;

import java.util.HashMap;

public interface Action {
	public void run();
	public void setParams(HashMap<String, String> params);
	
	public String getInfo();
	public Param[] getParams();
}
