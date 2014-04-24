package org.bladecoder.engine.actions;

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

public class PosAnimationAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Throws a position type animation";
	public static final Param[] PARAMS = {
		new Param("pos", "The target position", Type.VECTOR2, true),
		new Param("speed", "Duration of the animation in seconds", Type.FLOAT),
		new Param("count", "The times to repeat", Type.INTEGER),
		new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly", Type.BOOLEAN, true),
		new Param("repeat", "The repeat mode", Type.STRING, true, "repeat", new String[]{"repeat", "yoyo", "no_repeat"}),
		};		
	
	private String actorId;
	private float speed;
	private Vector2 pos;
	private int repeat = EngineTween.NO_REPEAT;
	private int count = 1;
	private boolean wait = true;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		// get final position. We need to scale the coordinates to the current resolution
		pos = Param.parseVector2(params.get("pos"));
		
		speed = Float.parseFloat(params.get("speed"));
		
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
				repeat = EngineTween.FROM_FA;
			}
		}
	}

	@Override
	public void run() {
		EngineLogger.debug("SET_POSANIMATION_ACTION");
		
		float scale = EngineAssetManager.getInstance().getScale();

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId);
		
		if(wait) {
			actor.startPosAnimation(repeat, count, speed, pos.x * scale, pos.y * scale, this);
		} else {
			actor.startPosAnimation(repeat, count, speed, pos.x * scale, pos.y * scale, null);
			onEvent();
		}
	}

	@Override
	public void write(Json json) {		
		json.writeValue("actorId", actorId);
		json.writeValue("pos", pos);
		json.writeValue("speed", speed);
		json.writeValue("repeat", repeat);
		json.writeValue("count", count);
		json.writeValue("wait", wait);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {	
		actorId = json.readValue("actorId", String.class, jsonData);
		pos = json.readValue("pos", Vector2.class, jsonData);
		speed = json.readValue("speed", Float.class, jsonData);
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
