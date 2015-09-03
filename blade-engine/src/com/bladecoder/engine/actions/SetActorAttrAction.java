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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.SpriteActor.DepthType;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SetActorAttr")
@ModelDescription("Change actor attributes.")
public class SetActorAttrAction extends AbstractAction {
	public enum ActorAttribute {
		VISIBLE, INTERACTION, LAYER, ZINDEX, FAKE_DEPTH, STAND_ANIMATION, TALK_ANIMATION, WALK_ANIMATION, WALKING_SPEED
	}

	@JsonProperty(value = "actor", required = true)
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty(required = true, defaultValue = "VISIBLE")
	@JsonPropertyDescription("The actor attribute")
	@ModelPropertyType(Type.STRING)
	private ActorAttribute attr;

	@JsonProperty(required=true)
	@JsonPropertyDescription("The attribute value")
	@ModelPropertyType(Type.STRING)
	private String value;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneActorRef = a == null ? new SceneActorRef() : new SceneActorRef(a[0], a[1]);

		attr = ActorAttribute.valueOf(params.get("attr").trim().toUpperCase());
		value = params.get("value");
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = sceneActorRef.getScene();

		BaseActor actor = s.getActor(sceneActorRef.getActorId(), true);

		switch(attr) {
		case FAKE_DEPTH:
			if (actor instanceof SpriteActor) {
				boolean fakeDepth = Boolean.parseBoolean(value);

				if(fakeDepth)
					((SpriteActor) actor).setDepthType(DepthType.VECTOR);
				else
					((SpriteActor) actor).setDepthType(DepthType.NONE);
			} else
				EngineLogger.error("fakeDepth property not supported for actor:" + actor.getId());
			break;
		case INTERACTION:
			if (actor instanceof InteractiveActor)
				((InteractiveActor) actor).setInteraction(Boolean.parseBoolean(value));
			else
				EngineLogger.error("Interaction property not supported for actor:" + actor.getId());
			break;
		case LAYER:
		{
			String oldLayer = actor.getLayer();

			s.getLayer(oldLayer).remove(actor);

			actor.setLayer(value);

			SceneLayer l = s.getLayer(value);
			l.add(actor);

			if (!l.isDynamic())
				l.orderByZIndex();
		}
			break;
		case STAND_ANIMATION:
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setStandAnim(value);
			else
				EngineLogger.error("standAnimation property not supported for actor:" + actor.getId());
			break;
		case TALK_ANIMATION:
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setTalkAnim(value);
			else
				EngineLogger.error("talkAnimation property not supported for actor:" + actor.getId());
			break;
		case VISIBLE:
			actor.setVisible(Boolean.parseBoolean(value));
			break;
		case WALKING_SPEED:
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setWalkingSpeed(Float.parseFloat(value));
			else
				EngineLogger.error("walkingSpeed property not supported for actor:" + actor.getId());
			break;
		case WALK_ANIMATION:
			if (actor instanceof CharacterActor)
				((CharacterActor) actor).setWalkAnim(value);
			else
				EngineLogger.error("walkAnimation property not supported for actor:" + actor.getId());
			break;
		case ZINDEX:
			if (actor instanceof InteractiveActor) {
				((InteractiveActor) actor).setZIndex(Integer.parseInt(value));
				SceneLayer l = s.getLayer(actor.getLayer());

				if (!l.isDynamic())
					l.orderByZIndex();
			} else
				EngineLogger.error("zIndex property not supported for actor:" + actor.getId());
			break;
		default:
			break;

		}

		return false;
	}

}
