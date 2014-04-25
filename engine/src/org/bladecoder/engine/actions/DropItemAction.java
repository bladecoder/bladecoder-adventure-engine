package org.bladecoder.engine.actions;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.math.Vector2;

public class DropItemAction implements Action {
	public static final String INFO = "Drops the inventory actor in the scene.";
	public static final Param[] PARAMS = {
		new Param("pos", "Position in the scene where de actor is dropped", Type.VECTOR2, false)
		};	
	
	String itemId;
	Vector2 pos;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		itemId = params.get("actor");
		pos = Param.parseVector2(params.get("pos"));
	}

	@Override
	public void run() {
		float scale =  EngineAssetManager.getInstance().getScale();
		
		Actor actor = World.getInstance().getCurrentScene().getActor(itemId);
		
		if(actor==null) {
			EngineLogger.error(MessageFormat.format("DropItemAction -  Item not found: {0}", itemId));
			return;
		}
		
		World.getInstance().getInventory().removeItem(itemId);
		
		World.getInstance().getCurrentScene().addActor(actor);
		((SpriteActor)actor).setPosition(pos.x * scale, pos.y * scale);
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
