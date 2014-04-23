package org.bladecoder.engineeditor.glcanvas;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class Assets extends AssetManager {
	private static Assets instance = new Assets();
	public static Assets inst() {return instance;}

	public void initialize() {
		String[] texturesNearest = new String[] {
			"res/images/transparent-light.png",
			"res/images/transparent-dark.png",
			"res/images/white.png"
		};

		String[] texturesLinear = new String[] {
			"res/images/on.png",
			"res/images/off.png"
		};

		for (String tex : texturesNearest) load(tex, Texture.class);
		for (String tex : texturesLinear) load(tex, Texture.class);

		while (update() == false) {}

		for (String tex : texturesLinear) {
			get(tex, Texture.class).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}
	}

}
