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

@ActionDescription(name="SceneState", value="Sets the scene state")
public class SetSceneStateAction implements Action {
	@ActionPropertyDescription("The scene")
	@ActionProperty(type = Type.SCENE)
	private String scene;

	@ActionProperty
	@ActionPropertyDescription("The scene 'state'")
	private String state;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {			
		Scene s = (scene != null && !scene.isEmpty())? w.getScene(scene): w.getCurrentScene();
		
		s.setState(state);
		
		return false;
	}


}
