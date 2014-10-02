/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.model;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json.Serializable;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.FrameAnimation;
import com.bladecoder.engine.assets.AssetConsumer;

public interface SpriteRenderer extends Serializable, AssetConsumer {

	public void update(float delta);
	public void draw(SpriteBatch batch, float x, float y, float scale);
	
	public float getWidth();
	public float getHeight();
	
	public FrameAnimation getCurrentFrameAnimation();
	public String getCurrentFrameAnimationId();
	
	public void lookat(float x, float y, Vector2 pf);
	public void lookat(String direction);
	public void stand();
	public void startWalkFA(Vector2 p0, Vector2 pf);
	public void startFrameAnimation(String id, int repeatType,
			int count, ActionCallback cb);
	
	
	public void addFrameAnimation(FrameAnimation fa);
	public void setInitFrameAnimation(String fa);
	public String getInitFrameAnimation();
	
	public String[] getInternalAnimations(String source);
	public HashMap<String, FrameAnimation> getFrameAnimations();
}

