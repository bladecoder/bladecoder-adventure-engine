package org.bladecoder.engine.anim;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class FrameAnimation {
	public	String id;
	public  String atlas;
	public	float speed;
	public	float delay;
	public  Vector2 inD;
	public  Vector2 outD;
	public	int animationType;
	public	int count;
	public  transient Array<AtlasRegion> regions;
	
	public String sound;
	
	public FrameAnimation() {
		
	}
	
	public FrameAnimation(String id, String atlas, float speed, 
			float delay, int count, int animationType, String sound, 
			Vector2 inD, Vector2 outD) {
		this.id = id;
		this.speed = speed;
		this.delay = delay;
		this.animationType = animationType;
		this.count = count;
		
		this.atlas = atlas;
		this.sound = sound;
		
		this.inD = inD;
		this.outD = outD;
	}
}
