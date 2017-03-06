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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Json.Serializable;
import com.bladecoder.engine.assets.AssetConsumer;

public interface ActorRenderer extends Serializable, AssetConsumer {

	public void update(float delta);
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint);
	
	public float getWidth();
	public float getHeight();
	
	public int getOrgAlign();
	public void setOrgAlign(int align);
	
	/**
	 * Compute the bbox based in the size of the animation/sprite. T
	 * 
	 * @param bbox The polygon to update. It will be updated when an animation starts/finishs.
	 */
	public void updateBboxFromRenderer(Polygon bbox);
}

