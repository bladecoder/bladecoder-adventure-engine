package org.bladecoder.engine.anim;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class AtlasFrameAnimation extends FrameAnimation {

	public  transient Array<AtlasRegion> regions;
	
	public AtlasFrameAnimation() {
	}
	
	public AtlasFrameAnimation(String id, String atlas, float duration, 
			float delay, int count, int animationType, String sound, 
			Vector2 inD, Vector2 outD) {
		super(id, atlas, duration, 
				delay, count, animationType, sound, 
				inD, outD);
	}
	
}
