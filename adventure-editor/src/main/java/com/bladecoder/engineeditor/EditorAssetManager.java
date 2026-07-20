package com.bladecoder.engineeditor;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bladecoder.engineeditor.common.EditorLogger;

public class EditorAssetManager extends AssetManager {
	public static final String ICON_ATLAS = "images/icons.atlas";
	
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
			EditorLogger.error("Region " + name + " not found in icon atlas ");
		}

		return region;
	}
}
