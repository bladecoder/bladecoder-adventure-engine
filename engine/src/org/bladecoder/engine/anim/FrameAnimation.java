package org.bladecoder.engine.anim;

import com.badlogic.gdx.math.Vector2;

public class FrameAnimation {
	public	String id;
	public  String source;
	public	float duration;
	public	float delay;
	public  Vector2 inD;
	public  Vector2 outD;
	public	int animationType;
	public	int count;
	
	public String sound;
	
	public boolean preload;
	public boolean disposeWhenPlayed;
	
	public FrameAnimation() {
		
	}
	
	public void set(String id, String source, float duration, 
			float delay, int count, int animationType, String sound, 
			Vector2 inD, Vector2 outD, boolean preload, boolean disposeWhenPlayed) {
		this.id = id;
		this.duration = duration;
		this.delay = delay;
		this.animationType = animationType;
		this.count = count;
		
		this.source = source;
		this.sound = sound;
		
		this.inD = inD;
		this.outD = outD;
		
		this.preload = preload;
		this.disposeWhenPlayed = disposeWhenPlayed;
	}
	
	public static String getFlipId(String id) {
		StringBuilder sb = new StringBuilder();

		if (id.endsWith("left")) {
			sb.append(id.substring(0, id.length() - 4));
			sb.append("right");
		} else if (id.endsWith("right")) {
			sb.append(id.substring(0, id.length() - 5));
			sb.append("left");
		}

		return sb.toString();
	}
}
