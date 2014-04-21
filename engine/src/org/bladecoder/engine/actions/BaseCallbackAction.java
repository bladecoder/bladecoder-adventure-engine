package org.bladecoder.engine.actions;

import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class BaseCallbackAction implements ActionCallback, ActionEndTrigger, Serializable {	
	private ActionCallback cb;
	private String cbSer;

	@Override
	public void onEvent() {
		if(cb != null || cbSer != null) {
			if(cb==null) {
				cb =  ActionCallbackSerialization.find(cbSer);
				cbSer = null;
			}
			
			ActionCallback cb2 = cb;
			cb = null;
			cb2.onEvent();
		}
	}

	@Override
	public void setCallback(ActionCallback cb) {
		this.cb = cb;
	}	

	@Override
	public void write(Json json) {		
		json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null : String.class);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {	
		cbSer = json.readValue("cb", String.class, jsonData);
	}
}
