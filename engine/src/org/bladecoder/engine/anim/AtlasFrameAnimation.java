package org.bladecoder.engine.anim;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;

public class AtlasFrameAnimation extends FrameAnimation implements
		AssetConsumer {

	public transient Array<AtlasRegion> regions;

	@Override
	public void dispose() {
		if (regions != null) {
			EngineAssetManager.getInstance().disposeAtlas(atlas);
			regions = null;
		}
	}

	@Override
	public void loadAssets() {
		EngineAssetManager.getInstance().loadAtlas(atlas);
	}

	@Override
	public void retrieveAssets() {
		regions = EngineAssetManager.getInstance().getRegions(atlas, id);
	}

}
