package org.bladecoder.engine.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;

public interface UIAssetConsumer extends Disposable {
	/**
	 * For dinamically created unmanaged assets. 
	 * This method will be called on GL context lost.
	 */
	public void createAssets();

	/**
	 * For managed assets, this method can retrieve his assets from the atlas.
	 * Also other managed resources can be loaded here.
	 */
	public void retrieveAssets(TextureAtlas atlas);
}
