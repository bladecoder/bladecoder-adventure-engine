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
package com.bladecoder.engineeditor.ui.panels;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;

public class InputPanelFactory extends Table {
	public static InputPanel createInputPanel(Skin skin, String title, String desc, String[] options,
			boolean mandatory) {
		return createInputPanel(skin, title, desc, Type.OPTION, mandatory, null, options);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, Enum<?>[] options,
			boolean mandatory) {
		return createInputPanel(skin, title, desc, Type.OPTION, mandatory, null, options);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc) {
		return createInputPanel(skin, title, desc, Type.STRING, false, null);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, boolean mandatory) {
		return createInputPanel(skin, title, desc, Type.STRING, mandatory, null);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, boolean mandatory,
			String defaultValue) {
		return createInputPanel(skin, title, desc, Type.STRING, mandatory, defaultValue);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type,
			boolean mandatory) {
		return createInputPanel(skin, title, desc, type, mandatory, null, (String[]) null);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory,
			String defaultValue) {
		return createInputPanel(skin, title, desc, type, mandatory, defaultValue, (String[]) null);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory,
			String defaultValue, Enum<?>[] options) {
		return createInputPanel(skin, title, desc, type, mandatory, defaultValue, (Object[]) options);
	}

	public static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory,
			String defaultValue, String[] options) {
		return createInputPanel(skin, title, desc, type, mandatory, defaultValue, (Object[]) options);
	}

	private static InputPanel createInputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory,
			String defaultValue, Object[] options) {
		switch (type) {
		case ACTOR:
		case CHARACTER_ACTOR:
		case INTERACTIVE_ACTOR:
		case SPRITE_ACTOR:
			return new ActorInputPanel(skin, title, desc, mandatory, defaultValue, type);
		case LAYER:
			return new LayerInputPanel(skin, title, desc, mandatory, defaultValue);
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
		case COLOR:
			return new ColorInputPanel(skin, title, desc, mandatory, defaultValue);
		case SOUND:
		case TEXT_STYLE:
		case STRING:
			if (options != null)
				return getReadOnlyOptionsInputPanel(skin, title, desc, mandatory, defaultValue, options);

			return new StringInputPanel(skin, title, desc, mandatory, defaultValue);
		case VECTOR2:
			return new Vector2InputPanel(skin, title, desc, mandatory, defaultValue);
		case OPTION:
			return getReadOnlyOptionsInputPanel(skin, title, desc, mandatory, defaultValue, options);
		case EDITABLE_OPTION:
			return new EditableOptionsInputPanel<>(skin, title, desc, mandatory, defaultValue, options);
		case SCENE_ACTOR:
		case SCENE_CHARACTER_ACTOR:
		case SCENE_INTERACTIVE_ACTOR:
		case SCENE_SPRITE_ACTOR:
			return new SceneActorInputPanel(skin, title, desc, mandatory, defaultValue, type);
		case ACTOR_ANIMATION:
			return new ActorAnimationInputPanel(skin, title, desc, mandatory, defaultValue);
		case VECTOR3:
			break;
		case FILE:
			break;
		case TEXT:
			return new TextInputPanel(skin, title, desc, mandatory, defaultValue);
		case BIG_TEXT:
			TextInputPanel i = new TextInputPanel(skin, title, desc, mandatory, defaultValue);
			i.setRows(20);
			return i;
		case SMALL_TEXT:
			TextInputPanel i2 = new TextInputPanel(skin, title, desc, mandatory, defaultValue);
			i2.setRows(5);
			return i2;

		case ATLAS_ASSET:
		case MUSIC_ASSET:
		case SOUND_ASSET:
		case PARTICLE_ASSET:
		case FONT_ASSET:
			return getReadOnlyOptionsInputPanel(skin, title, desc, mandatory, defaultValue, getAssetList(type));
		case NOT_SET:
			break;
		default:
			break;
		}

		return new StringInputPanel(skin, title, desc, mandatory, defaultValue);
	}

	private static InputPanel getReadOnlyOptionsInputPanel(Skin skin, String title, String desc, boolean mandatory,
			String defaultValue, Object[] options) {
		if (options instanceof Enum[]) {
			return new EnumOptionsInputPanel(skin, title, desc, mandatory, defaultValue, (Enum[]) options);
		} else if (options instanceof String[]) {
			return new StringOptionsInputPanel(skin, title, desc, mandatory, defaultValue, (String[]) options);
		} else {
			throw new RuntimeException("Unknown type of options: " + options.getClass());
		}
	}

	private static String[] getAssetList(Param.Type type) {
		String path = null;
		final String exts[];
		boolean cutExt = false;

		if (type == Type.ATLAS_ASSET) {
			path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/" + Ctx.project.getResDir();
			exts = new String[1];
			exts[0] = EngineAssetManager.ATLAS_EXT;
			cutExt = true;
		} else if (type == Type.MUSIC_ASSET) {
			path = Ctx.project.getProjectPath() + Project.MUSIC_PATH + "/";
			exts = new String[2];
			exts[0] = ".ogg";
			exts[1] = ".mp3";
		} else if (type == Type.SOUND_ASSET) {
			path = Ctx.project.getProjectPath() + Project.SOUND_PATH + "/";
			exts = new String[2];
			exts[0] = ".ogg";
			exts[1] = ".mp3";
		} else if (type == Type.PARTICLE_ASSET) {
			path = Ctx.project.getProjectPath() + Project.PARTICLE_PATH + "/";
			exts = null;
		} else if (type == Type.FONT_ASSET) {
			path = Ctx.project.getProjectPath() + Project.FONT_PATH + "/";
			exts = new String[1];
			exts[0] = ".ttf";
			cutExt = true;	
		} else {
			exts = null;
		}

		File f = new File(path);

		String list[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (exts == null)
					return true;

				for (String ext : exts)
					if (arg1.endsWith(ext))
						return true;

				return false;
			}
		});

		if(list == null) {
			return new String[0];
		}
			
		Arrays.sort(list);

		if (cutExt) {
			for (int i = 0; i < list.length; i++) {
				int idx = list[i].lastIndexOf('.');
				if (idx != -1)
					list[i] = list[i].substring(0, idx);
			}
		}

		return list;
	}
}
