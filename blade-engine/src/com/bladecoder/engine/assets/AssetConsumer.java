package com.bladecoder.engine.assets;

import com.badlogic.gdx.utils.Disposable;

public interface AssetConsumer extends Disposable {
	/**
	 * Send the assets to the AssetManager queue to load asynchronous
	 */
    void loadAssets();

	/**
	 * Called when the AssetManager has loaded all the assets and can be retrieved.
	 */
    void retrieveAssets();
}
