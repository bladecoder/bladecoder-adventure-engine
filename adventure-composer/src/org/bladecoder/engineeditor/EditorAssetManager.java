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
package org.bladecoder.engineeditor;

import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class EditorAssetManager extends AssetManager {
	public static final String ICON_ATLAS = "res/images/icons.atlas";
	
	public EditorAssetManager() {
		super();
		
		/*** LOAD ICON ATLAS ***/
		load(ICON_ATLAS, TextureAtlas.class);
		finishLoading();
	}

	public TextureRegion getIcon(String name) {
		TextureAtlas a = get(ICON_ATLAS, TextureAtlas.class);

		AtlasRegion region = a.findRegion(name);

		if (region == null) {
			EngineLogger.error("Region " + name + " not found in icon atlas ");
		}

		return region;
	}
}
