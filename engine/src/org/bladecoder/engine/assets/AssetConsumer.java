package org.bladecoder.engine.assets;

import com.badlogic.gdx.utils.Disposable;

public interface AssetConsumer extends Disposable {
	/**
	 * Send the assets to the AssetManager queue to load asynchronous
	 */
	public void loadAssets();

	/**
	 * Called when the AssetManager has loaded all the assets and can be retrieved.
	 */
	public void retrieveAssets();
}
