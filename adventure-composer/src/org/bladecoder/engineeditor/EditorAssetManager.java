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
