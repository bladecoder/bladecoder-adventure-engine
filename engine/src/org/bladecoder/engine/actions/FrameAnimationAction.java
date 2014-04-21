package org.bladecoder.engine.actions;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class FrameAnimationAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Sets an actor frame animation";
	public static final Param[] PARAMS = {
		new Param("frame_animation", "The FA to set", Type.STRING, true),	
		new Param("reverse", "Sets the FA in reverse mode", Type.BOOLEAN),
		new Param("count", "The times to repeat", Type.INTEGER),
		new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly", Type.BOOLEAN, true),
		new Param("repeat", "The repeat mode", Type.STRING, true, "repeat", new String[]{"repeat", "yoyo", "no_repeat"}),
		new Param("x", "Puts actor 'x' position after sets the FA", Type.FLOAT),
		new Param("y", "Puts actor 'y' position after sets the FA", Type.FLOAT),
		new Param("dx", "Adds 'dx' to the actor position", Type.FLOAT),
		new Param("dy", "Adds 'dy' to the actor position", Type.FLOAT),			
		};		
	
	private static final int NO_POS = 0;
	private static final int SET_POS_ABSOLUTE = 1;
	private static final int SET_POS_RELATIVE = 2;
	
	private String fa;
	private String actorId;
	private float posx, posy;
	private int setPos = NO_POS;
	private boolean reverse = false;
	private int repeat = EngineTween.REPEAT_DEFAULT;
	private int count = 1;
	private boolean wait = true;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		fa = params.get("frame_animation");

		if (params.get("x") != null) {
			posx = Float.parseFloat(params.get("x"));
			posy = Float.parseFloat(params.get("y"));
			setPos = SET_POS_ABSOLUTE;
		}
		
		if (params.get("dx") != null) {
			posx = Float.parseFloat(params.get("dx"));
			posy = Float.parseFloat(params.get("dy"));
			setPos = SET_POS_RELATIVE;
		}
		
		if(params.get("reverse") != null) {
			reverse = Boolean.parseBoolean(params.get("reverse"));
		}
		
		if(params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
		
		if(params.get("repeat") != null) {
			String repeatStr = params.get("repeat");
			if (repeatStr.equalsIgnoreCase("repeat")) {
				repeat = EngineTween.REPEAT;
			} else if (repeatStr.equalsIgnoreCase("yoyo")) {
				repeat = EngineTween.YOYO;
			} else if (repeatStr.equalsIgnoreCase("no_repeat")) {
				repeat = EngineTween.NO_REPEAT;
			} else {
				repeat = EngineTween.REPEAT_DEFAULT;
			}
		}
	}

	@Override
	public void run() {
		EngineLogger.debug(MessageFormat.format("SET_FRAMEANIMATION_ACTION: {0}", fa));
		
		float scale =  EngineAssetManager.getInstance().getScale();

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId);
		
		if(wait) {
			actor.startFrameAnimation(fa, repeat, count, reverse, this);
		} else {
			actor.startFrameAnimation(fa, repeat, count, reverse, null);
			onEvent();
		}		

		if (setPos == SET_POS_ABSOLUTE)
			actor.setPosition(posx * scale, posy * scale);
		else if (setPos == SET_POS_RELATIVE) {
			Vector2 p = actor.getPosition();
			
			actor.setPosition(p.x + posx * scale, p.y + posy * scale);
		}
	}

	@Override
	public void write(Json json) {		
		json.writeValue("fa", fa);
		json.writeValue("actorId", actorId);
		json.writeValue("posx", posx);
		json.writeValue("posy", posy);
		json.writeValue("setPos", setPos);
		json.writeValue("reverse", reverse);
		json.writeValue("repeat", repeat);
		json.writeValue("count", count);
		json.writeValue("wait", wait);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {	
		fa = json.readValue("fa", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		posx = json.readValue("posx", Float.class, jsonData);
		posy = json.readValue("posy", Float.class, jsonData);
		setPos = json.readValue("setPos", Integer.class, jsonData);
		reverse = json.readValue("reverse", Boolean.class, jsonData);
		repeat = json.readValue("repeat", Integer.class, jsonData);
		count = json.readValue("count", Integer.class, jsonData);
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
