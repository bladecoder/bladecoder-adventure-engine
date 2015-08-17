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
package com.bladecoder.engine.actions;

import java.util.HashMap;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class SetActorAttrAction implements Action {
	public static final String INFO = "Change actor attributes.";
	public static final Param[] PARAMS = { new Param("actor", "The target actor", Type.SCENE_ACTOR, false),
			new Param("visible", "sets the actor visibility", Type.BOOLEAN),
			new Param("interaction", "when 'true' the actor responds to the user input", Type.BOOLEAN),
			new Param("layer", "The actor layer", Type.LAYER),
			new Param("zIndex", "The order to draw bigger is near", Type.FLOAT),
			new Param("position", "Sets the actor position", Type.VECTOR2),
			new Param("scale", "Sets the actor scale", Type.FLOAT),
			new Param("standAnimation", "Sets the actor 'stand' animation. Only supported for character actors.", Type.STRING),
			new Param("walkAnimation", "Sets the actor 'walk' animation. Only supported for character actors.", Type.STRING),
			new Param("talkAnimation", "Sets the actor 'talk' animation. Only supported for character actors.", Type.STRING),
			new Param("walkingSpeed", "Sets the actor speed for walking. Only supported for character actors.", Type.STRING),
			};

	String actorId;
	String sceneId;
	String visible;
	String interaction;
	String layer;
	String zIndex;

	String position;
	String scale;
	
	String standAnimation;
	String walkAnimation;
	String talkAnimation;
	String walkingSpeed;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneId = a[0];
		actorId = a[1];

		visible = params.get("visible");
		interaction = params.get("interaction");
		layer = params.get("layer");
		zIndex = params.get("zIndex");

		position = params.get("position");
		scale = params.get("scale");
		
		standAnimation = params.get("standAnimation");
		walkAnimation = params.get("walkAnimation");
		talkAnimation = params.get("talkAnimation");
		walkingSpeed = params.get("walkingSpeed");
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s;

		if (sceneId != null && !sceneId.isEmpty()) {
			s = World.getInstance().getScene(sceneId);
		} else {
			s = World.getInstance().getCurrentScene();
		}

		BaseActor actor = s.getActor(actorId, true);

		if (visible != null)
			actor.setVisible(Boolean.parseBoolean(visible));

		if (interaction != null) {
			if (actor instanceof InteractiveActor)
				((InteractiveActor) actor).setInteraction(Boolean.parseBoolean(interaction));
			else
				EngineLogger.error("Interaction property not supported for actor:" + actor.getId());
		}

		if (layer != null) {
			String oldLayer = actor.getLayer();

			s.getLayer(oldLayer).remove(actor);

			actor.setLayer(layer);

			SceneLayer l = s.getLayer(layer);
			l.add(actor);

			if (!l.isDynamic())
				l.orderByZIndex();
		}

		if (zIndex != null) {
			if (actor instanceof InteractiveActor) {
				((InteractiveActor) actor).setZIndex(Float.parseFloat(zIndex));
				SceneLayer l = s.getLayer(actor.getLayer());

				if (!l.isDynamic())
					l.orderByZIndex();
			} else
				EngineLogger.error("zIndex property not supported for actor:" + actor.getId());
		}

		if (position != null) {
			float scale = EngineAssetManager.getInstance().getScale();

			Vector2 pos = Param.parseVector2(position);
			actor.setPosition(pos.x * scale, pos.y * scale);
		}

		if (scale != null) {
			if (actor instanceof SpriteActor)
				((SpriteActor) actor).setScale(Float.parseFloat(scale));
			else
				EngineLogger.error("scale property not supported for actor:" + actor.getId());
		}
		
		if (standAnimation != null) {
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setStandAnim(standAnimation);
			else
				EngineLogger.error("standAnimation property not supported for actor:" + actor.getId());
		}
		
		if (walkAnimation != null) {
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setWalkAnim(walkAnimation);
			else
				EngineLogger.error("walkAnimation property not supported for actor:" + actor.getId());
		}
		
		if (talkAnimation != null) {
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setTalkAnim(talkAnimation);
			else
				EngineLogger.error("talkAnimation property not supported for actor:" + actor.getId());
		}
		
		if (walkingSpeed != null) {
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setWalkingSpeed(Float.parseFloat(scale));
			else
				EngineLogger.error("walkingSpeed property not supported for actor:" + actor.getId());
		}

		return false;
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
