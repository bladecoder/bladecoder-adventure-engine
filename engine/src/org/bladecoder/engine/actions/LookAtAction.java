package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.model.Text;
import org.bladecoder.engine.model.TextManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class LookAtAction implements Action {
	public static final String INFO = "Shows the text and sets the player to lookat in the selected actor direction";
	public static final Param[] PARAMS = {
		new Param("speech", "The 'soundId' to play if selected", Type.STRING),
		new Param("text", "The 'text' to show", Type.STRING),
		new Param("direction", "The direction to lookat. If empty, the player lookat to the actor", 
				Type.STRING, false, "", new String[] {"", 
				FrameAnimation.FRONT, FrameAnimation.BACK,FrameAnimation.LEFT,
				FrameAnimation.RIGHT,	FrameAnimation.FRONTLEFT,
				FrameAnimation.FRONTRIGHT,FrameAnimation.BACKLEFT,
				FrameAnimation.BACKRIGHT,})
		};			

	private String soundId;
	private String text;

	private String actorId;
	
	private String direction;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		soundId = params.get("speech");
		text = params.get("text");
		direction = params.get("direction");
	}

	@Override
	public void run() {
		EngineLogger.debug("LOOKAT ACTION");
		Actor actor = (Actor) World.getInstance().getCurrentScene().getActor(actorId);

		SpriteActor player = World.getInstance().getCurrentScene().getPlayer();
		
		if(direction!=null) player.lookat(direction);
		else if(actor!=null && player != null) {
			Rectangle bbox = actor.getBBox().getBoundingRectangle();
			player.lookat(new Vector2(bbox.x, bbox.y));
		}

		if (soundId != null)
			actor.playSound(soundId);

		if(text !=null)
			World.getInstance().getTextManager().addSubtitle(text, TextManager.POS_SUBTITLE,
					TextManager.POS_SUBTITLE, false, Text.Type.RECTANGLE, Color.BLACK, null);
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
