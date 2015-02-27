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
package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;

public class InputPanelFactory extends Table {
	public static InputPanel createInputPanel(Skin skin, String title, String desc, String[] options) {
    	return createInputPanel(skin, title, desc, Type.OPTION, false, null, options);
    }
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc) {
    	return createInputPanel(skin, title, desc, Type.STRING, false, null);
    }
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc, boolean mandatory) {
    	return createInputPanel(skin, title, desc,Type.STRING, mandatory, null);
    }    
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
    	return createInputPanel(skin, title, desc,Type.STRING, mandatory, defaultValue);
    }
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory) {
    	return createInputPanel(skin, title, desc, type, mandatory, null, null);
    }
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory, String defaultValue) {
    	return createInputPanel(skin, title, desc, type, mandatory, defaultValue, null);
    }
    
    public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory, String defaultValue, String[] options) {
    	switch (type) {
		case ACTOR:
			return new ActorInputPanel(skin, title, desc, mandatory, defaultValue);
		case BOOLEAN:
			return new BooleanInputPanel(skin, title, desc, mandatory, defaultValue);
		case CHAPTER:
			return new ChapterInputPanel(skin, title, desc, mandatory, defaultValue);
		case DIMENSION:
			return new DimensionInputPanel(skin, title, desc, mandatory, defaultValue);
		case FLOAT:
			return new FloatInputPanel(skin, title, desc, mandatory, defaultValue);
		case INTEGER:			
			return new IntegerInputPanel(skin, title, desc, mandatory, defaultValue);
		case SCENE:
			return new SceneInputPanel(skin, title, desc, mandatory, defaultValue);
		case STRING:
			return new StringInputPanel(skin, title, desc, mandatory, defaultValue);
		case VECTOR2:
			return new Vector2InputPanel(skin, title, desc, mandatory, defaultValue);
		case OPTION:
			return new OptionsInputPanel(skin, title, desc, mandatory, defaultValue, options);
		case SCENE_ACTOR:
			return new SceneActorInputPanel(skin, title, desc, mandatory, defaultValue);
		case ACTOR_ANIMATION:
			return new ActorAnimationInputPanel(skin, title, desc, mandatory, defaultValue);
		case VECTOR3:
			break;
		case FILE:
			break;			
		}
    	
    	return new StringInputPanel(skin, title, desc, mandatory, defaultValue);
	}
}
