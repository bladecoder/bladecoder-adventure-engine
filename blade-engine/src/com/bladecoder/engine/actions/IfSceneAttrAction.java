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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Execute the actions inside the If/EndIf if the attribute has the specified value.")
public class IfSceneAttrAction extends AbstractIfAction {

	public enum SceneAttr {
		STATE
	}

	@ActionPropertyDescription("The scene to check its attribute")
	@ActionProperty(type = Type.SCENE)
	private String scene;

	@ActionProperty(required = true, defaultValue = "state")
	@ActionPropertyDescription("The scene attribute")
	private SceneAttr attr;

	@ActionProperty
	@ActionPropertyDescription("The attribute value")
	private String value;

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = (scene != null && !scene.isEmpty()) ? World.getInstance().getScene(scene) : World.getInstance()
				.getCurrentScene();

		if (attr == SceneAttr.STATE) {
			if (!((s.getState() == null && value == null) || (s.getState() != null && s.getState().equals(value)))) {
				gotoElse((VerbRunner) cb);
			}
		}

		return false;
	}
}
