package org.bladecoder.engine.anim;

import com.badlogic.gdx.math.Vector2;

public class Sprite3DFrameAnimation implements FrameAnimation {
	public	String id;
	public  String model;
	public	float speed;
	public	float delay;
	public  Vector2 inD;
	public  Vector2 outD;
	public	int animationType;
	public	int count;
	
	public String sound;
	
	public Sprite3DFrameAnimation() {
		
	}
	
	public Sprite3DFrameAnimation(String id, String atlas, float speed, 
			float delay, int count, int animationType, String sound, 
			Vector2 inD, Vector2 outD) {
		this.id = id;
		this.speed = speed;
		this.delay = delay;
		this.animationType = animationType;
		this.count = count;
		
		this.model = atlas;
		this.sound = sound;
		
		this.inD = inD;
		this.outD = outD;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public Vector2 getInD() {
		return inD;
	}

	@Override
	public Vector2 getOutD() {
		return outD;
	}

	@Override
	public String getSound() {
		return sound;
	}
}
