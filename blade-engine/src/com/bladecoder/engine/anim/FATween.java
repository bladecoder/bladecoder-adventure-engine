package com.bladecoder.engine.anim;

import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.AtlasRenderer;

/**
 * Tween for spriteactor position animation
 */
public class FATween extends Tween<AtlasRenderer> {
	
	public FATween() {
	}

	public void start(AtlasRenderer target, Tween.Type repeatType, int count, float duration, ActionCallback cb) {
		this.target = target;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}
	
	public void updateTarget() {
		if(!isComplete() && getPercent() < 1.0f)
			target.setFrame((int)(getPercent() * target.getNumFrames()));
	}
}
