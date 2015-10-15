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
import com.bladecoder.engine.model.World;

@ActionDescription("Load the specified Chapter. Scene can be empty to load the default scene.")
public class LoadChapterAction implements Action {
	@ActionPropertyDescription("The target chapter")
	@ActionProperty(type = Type.CHAPTER, required = true)
	private String chapter;

	@ActionPropertyDescription("The target scene")
	@ActionProperty(type=Type.SCENE, required = false)
	private String scene;

	@Override
	public boolean run(ActionCallback cb) {
		World.getInstance().loadChapter(chapter, scene);
		
		return false;
	}

}
