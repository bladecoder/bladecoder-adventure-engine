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
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;

@ActionDescription("Execute actions inside the If/EndIf if the scene attribute has the specified value.")
public class IfSceneAttrAction extends AbstractIfAction {

	public enum SceneAttr {
		STATE, CURRENT_SCENE, PLAYER
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
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = (scene != null && !scene.isEmpty()) ? w.getScene(scene) : w
				.getCurrentScene();

		if (attr == SceneAttr.STATE) {
			if (!ActionUtils.compareNullStr(value, s.getState())) {
				gotoElse((VerbRunner) cb);
			}
		} else if (attr == SceneAttr.CURRENT_SCENE) {
			String scn = w.getCurrentScene().getId();
			
			if (!ActionUtils.compareNullStr(value, scn)) {
				gotoElse((VerbRunner) cb);
			}
		} else if (attr == SceneAttr.PLAYER) {
			CharacterActor player = s.getPlayer();
			
			String id = player!=null?player.getId():null;
			
			if (!ActionUtils.compareNullStr(value, id)) {
				gotoElse((VerbRunner) cb);
			}			
		}

		return false;
	}
}
