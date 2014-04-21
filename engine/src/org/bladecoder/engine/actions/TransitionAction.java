package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Transition;
import org.bladecoder.engine.model.World;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class TransitionAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Sets a transition effect (FADEIN/FADEOUT)";
	public static final Param[] PARAMS = {
		new Param("time", "Duration of the transition", Type.FLOAT, true, "1.0"),
		new Param("color", "The color to fade ('white', 'black' or RRGGBBAA).", Type.STRING, true, "black"),
		new Param("type", "The transition type (fadein/fadeout)", Type.STRING, true, "fadein", new String[] {"fadein", "fadeout"}),
		new Param("wait", "If this param is 'false' the transition is showed and the action continues inmediatly", Type.BOOLEAN, true),
		};		
	
	private float time = 1;
	Color c = new Color(0,0,0,1);
	Transition.Type type = Transition.Type.FADE_IN;
	private boolean wait = true;
	
	@Override
	public void run() {
		
		Transition t = new Transition();
		t.create(time, c, type, wait?this:null);
		
		World.getInstance().getCurrentScene().setTransition(t);
		
		if(!wait)
			onEvent();
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		
		if(params.get("time") != null) {
			time = Float.parseFloat(params.get("time"));
		}
		
		if(params.get("type") != null) {
			if(params.get("type").equals("fadeout"))
				type = Transition.Type.FADE_OUT;
		}
		
		if(params.get("color") != null && !params.get("color").trim().isEmpty()) {
			String color = params.get("color").trim();
		
			if(color.equals("black"))
				c =  new Color(0,0,0,1);
			else if(color.equals("white"))
				c = new Color(1,1,1,1);
			else
				c = Color.valueOf(color);
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
	}
	
	@Override
	public void write(Json json) {		
		json.writeValue("time", time);
		json.writeValue("color", c);
		json.writeValue("type", type);
		json.writeValue("wait", wait);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		time = json.readValue("time", Float.class, jsonData);
		c = json.readValue("color", Color.class, jsonData);
		type = json.readValue("type", Transition.Type.class, jsonData);
		wait = json.readValue("wait", Boolean.class, jsonData);
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
