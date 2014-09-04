/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
				FrameAnimation.BACKRIGHT})
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
		Actor actor = (Actor) World.getInstance().getCurrentScene().getActor(actorId, true);

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

	@Override
	public boolean waitForFinish(ActionCallback cb) {
		return false;
	}
}
