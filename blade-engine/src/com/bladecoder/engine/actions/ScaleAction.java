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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Scale")
@ModelDescription("Change actor scale")
public class ScaleAction extends AbstractAction {
	@JsonProperty(value = "actor", required = true)
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The actor scale")
	@ModelPropertyType(Type.FLOAT)
	private float scale;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneActorRef = a == null ? new SceneActorRef() : new SceneActorRef(a[0], a[1]);
		scale = Float.parseFloat(params.get("scale"));
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = sceneActorRef.getScene();

		BaseActor actor = s.getActor(sceneActorRef.getActorId(), true);

		if (actor instanceof SpriteActor)
			((SpriteActor) actor).setScale(scale);
		else
			EngineLogger.error("scale property not supported for actor:" + actor.getId());

		return false;
	}

}
