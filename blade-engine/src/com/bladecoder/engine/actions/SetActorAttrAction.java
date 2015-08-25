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

import javax.annotation.Nullable;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
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

public class SetActorAttrAction implements Action {
	@JsonProperty(value = "actor", required = true)
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor visibility")
	@ModelPropertyType(Type.BOOLEAN)
	private Boolean visible;

	@JsonProperty
	@JsonPropertyDescription("When 'true' the actor responds to the user input")
	@ModelPropertyType(Type.BOOLEAN)
	private Boolean interaction;

	@JsonProperty
	@JsonPropertyDescription("The actor layer")
	@ModelPropertyType(Type.LAYER)
	private String layer;

	@JsonProperty
	@JsonPropertyDescription("The order to draw bigger is near")
	@ModelPropertyType(Type.FLOAT)
	private Float zIndex;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor position")
	@ModelPropertyType(Type.VECTOR2)
	private Vector2 position;

	@JsonProperty
	@JsonPropertyDescription("Enable/Disable the Fake Depth for the actor")
	@ModelPropertyType(Type.BOOLEAN)
	private Boolean fakeDepth;
	
	@JsonProperty
	@JsonPropertyDescription("Sets the actor scale")
	@ModelPropertyType(Type.FLOAT)
	private Float scale;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor 'stand' animation. Only supported for character actors.")
	@ModelPropertyType(Type.STRING)
	private String standAnimation;
	
	@JsonProperty
	@JsonPropertyDescription("Sets the actor 'walk' animation. Only supported for character actors.")
	@ModelPropertyType(Type.STRING)
	private String walkAnimation;
	@JsonProperty
	@JsonPropertyDescription("Sets the actor 'talk' animation. Only supported for character actors.")
	@ModelPropertyType(Type.STRING)
	private String talkAnimation;
	@JsonProperty
	@JsonPropertyDescription("Sets the actor speed for walking. Only supported for character actors.")
	@ModelPropertyType(Type.FLOAT)
	private Float walkingSpeed;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneActorRef = a == null ? new SceneActorRef() : new SceneActorRef(a[0], a[1]);

		visible = booleanOrNull(params.get("visible"));
		interaction = booleanOrNull(params.get("interaction"));
		layer = params.get("layer");
		zIndex = floatOrNull(params.get("zIndex"));

		position = vector2OrNull(params.get("position"));
		scale = floatOrNull(params.get("scale"));
		fakeDepth = booleanOrNull(params.get("fakeDepth"));
		
		standAnimation = params.get("standAnimation");
		walkAnimation = params.get("walkAnimation");
		talkAnimation = params.get("talkAnimation");
		walkingSpeed = floatOrNull(params.get("walkingSpeed"));
	}

	@Nullable
	private static Vector2 vector2OrNull(String str) {
		return str != null ? Param.parseVector2(str) : null;
	}

	@Nullable
	private static Boolean booleanOrNull(String str) {
		return str != null ? Boolean.parseBoolean(str) : null;
	}

	@Nullable
	private static Float floatOrNull(String str) {
		return str != null ? Float.parseFloat(str) : null;
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = sceneActorRef.getScene();

		BaseActor actor = s.getActor(sceneActorRef.getActorId(), true);

		if (visible != null)
			actor.setVisible(visible);

		if (interaction != null) {
			if (actor instanceof InteractiveActor)
				((InteractiveActor) actor).setInteraction(interaction);
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
				((InteractiveActor) actor).setZIndex(zIndex);
				SceneLayer l = s.getLayer(actor.getLayer());

				if (!l.isDynamic())
					l.orderByZIndex();
			} else
				EngineLogger.error("zIndex property not supported for actor:" + actor.getId());
		}

		if (position != null) {
			float scale = EngineAssetManager.getInstance().getScale();

			actor.setPosition(position.x * scale, position.y * scale);
		}

		if (scale != null) {
			if (actor instanceof SpriteActor)
				((SpriteActor) actor).setScale(scale);
			else
				EngineLogger.error("scale property not supported for actor:" + actor.getId());
		}
		
		if (fakeDepth != null) {
			if (actor instanceof SpriteActor) {
				if(fakeDepth)
					((SpriteActor) actor).setDepthType(DepthType.VECTOR);
				else
					((SpriteActor) actor).setDepthType(DepthType.NONE);
			} else
				EngineLogger.error("fakeDepth property not supported for actor:" + actor.getId());			
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
				((CharacterActor) actor).setWalkingSpeed(walkingSpeed);
			else
				EngineLogger.error("walkingSpeed property not supported for actor:" + actor.getId());
		}

		return false;
	}

}
