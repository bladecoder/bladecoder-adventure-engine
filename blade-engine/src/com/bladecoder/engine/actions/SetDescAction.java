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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription(value = "Sets the actor description")
public class SetDescAction implements Action {
	@ActionProperty(type = Type.SCENE_INTERACTIVE_ACTOR, required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("The actor 'desc'")
	private String text;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		final Scene s = actor.getScene(w);

		String actorId = actor.getActorId();
		if (actorId == null) {
			EngineLogger.error("SetDesc - Actor not set.");
			return false;
		}

		InteractiveActor a = (InteractiveActor) s.getActor(actorId, true);

		if (a != null)
			a.setDesc(text);
		else
			EngineLogger.error("SetDesc - Actor not found: " + actorId);

		return false;
	}

}
