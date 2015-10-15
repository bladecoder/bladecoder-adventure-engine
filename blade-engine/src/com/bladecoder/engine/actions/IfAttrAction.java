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

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Execute the actions inside the If/EndIf if the attribute has the specified value.")
public class IfAttrAction extends AbstractIfAction {
	public static final String ENDTYPE_VALUE = "else";

	public enum ActorAttribute {
		STATE, VISIBLE
	}

	@ActionProperty
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty(required = true, defaultValue = "STATE")
	@ActionPropertyDescription("The actor attribute")
	private ActorAttribute attr;

	@ActionProperty
	@ActionPropertyDescription("The attribute value")
	private String value;

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = actor.getScene();

		final String actorId = actor.getActorId();
		if (actorId == null) {
			// if called inside a scene verb and no actor is specified, return
			EngineLogger.error(getClass() + ": No actor specified");
			return false;
		}

		BaseActor a = s.getActor(actorId, true);

		if (attr.equals(ActorAttribute.STATE) && a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor)a;
			if (!((ia.getState() == null && value == null) || (ia.getState() != null && ia.getState().equals(value)))) {
				gotoElse((VerbRunner) cb);
			}
		}
		if (attr.equals(ActorAttribute.VISIBLE)) {
			boolean val = Boolean.parseBoolean(value);
			if (val != a.isVisible()) {
				gotoElse((VerbRunner) cb);
			}
		}

		return false;
	}
}
