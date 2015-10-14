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
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Change actor attributes.")
public class PositionAction implements Action {
	@JsonProperty(value = "actor", required = true)
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor position")
	@ActionPropertyType(Type.VECTOR2)
	private Vector2 position;
	
	@JsonProperty(value = "anchor")
	@JsonPropertyDescription("Sets the position of this actor")
	@ActionPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef anchorActorRef;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneActorRef = a == null ? new SceneActorRef() : new SceneActorRef(a[0], a[1]);

		position = vector2OrNull(params.get("position"));
		
		
		String[] b = Param.parseString2(params.get("anchor"));
		anchorActorRef = b == null ? null : new SceneActorRef(b[0], b[1]);
	}

	private static Vector2 vector2OrNull(String str) {
		return str != null ? Param.parseVector2(str) : null;
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = sceneActorRef.getScene();

		BaseActor actor = s.getActor(sceneActorRef.getActorId(), true);

		if (position != null) {
			float scale = EngineAssetManager.getInstance().getScale();

			actor.setPosition(position.x * scale, position.y * scale);
		} else if(anchorActorRef != null) {
			BaseActor anchor = s.getActor(anchorActorRef.getActorId(), true);
			
			actor.setPosition(anchor.getX(), anchor.getY());
		}

		return false;
	}

}
