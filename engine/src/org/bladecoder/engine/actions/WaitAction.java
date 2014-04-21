package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class WaitAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Pause the action";
	public static final Param[] PARAMS = {
		new Param("time", "The time for the pause in seconds", Type.FLOAT, true, "1.0")
		};		
	
	private float time;
	

	@Override
	public void run() {
		World.getInstance().addTimer(time, this);
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		time = Float.parseFloat(params.get("time"));
	}
	
	@Override
	public void write(Json json) {		
		json.writeValue("time", time);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		time = json.readValue("time", Float.class, jsonData);
		super.read(json, jsonData);
	}	
	

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}
