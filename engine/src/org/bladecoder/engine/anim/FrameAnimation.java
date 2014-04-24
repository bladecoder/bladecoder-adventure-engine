package org.bladecoder.engine.anim;

import com.badlogic.gdx.math.Vector2;

public interface FrameAnimation {
	public String getId();
	
	/**
	 * Input Displacement: The Sprite is displaced when the FA is started.
	 * @return
	 */
	public Vector2 getInD();
	
	/**
	 * Output Displacement: The Sprite is displaced when the FA is replaced.
	 * @return
	 */	
	public Vector2 getOutD();
	
	public String getSound();
}
