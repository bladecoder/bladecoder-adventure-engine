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
package com.bladecoder.engineeditor.scneditor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.RectangleRenderer;

/**
 * Frame Animation renderer.
 * 
 * @author rgarcia
 */
public class SpriteDrawer {

	public static final Color BG_COLOR = Color.MAGENTA;
	private static final float HEIGHT = 200;

	AnimationDesc currentAnimation;
	private ActorRenderer renderer;
	private float viewportW, viewportH;

	public void setViewport(float w, float h) {
		viewportW = w;
		viewportH = h;
	}

	public void setActor(BaseActor a) {
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}

		if (a instanceof SpriteActor) {
			ActorRenderer r = ((SpriteActor) a).getRenderer();

			if (r instanceof Sprite3DRenderer) {
				renderer = new Sprite3DRenderer();
				((Sprite3DRenderer) renderer).setSpriteSize(new Vector2(r
						.getWidth(), r.getHeight()));
			} else if (r instanceof SpineRenderer) {
				renderer = new SpineRenderer();
				((SpineRenderer)renderer).enableEvents(false);
			} else if (r instanceof ImageRenderer) {
				renderer = new ImageRenderer();				
			} else {
				renderer = new AtlasRenderer();
			}
		}
	}

	public void setAnimation(AnimationDesc fa) {
		currentAnimation = fa;

		if (renderer != null) {

			renderer.getAnimations().clear();

			if (fa != null) {

				renderer.addAnimation(fa);

				renderer.retrieveAssets();

				renderer.startAnimation(fa.id, Tween.REPEAT, Tween.INFINITY, null);
			}
		}
	}

	public void draw(SpriteBatch batch) {
		if (renderer != null && currentAnimation != null) {

			float width;
			
			if(renderer.getWidth() < renderer.getHeight())
				width = HEIGHT / renderer.getHeight() * renderer.getWidth();
			else
				width = HEIGHT / renderer.getWidth() * renderer.getHeight();

			RectangleRenderer.draw(batch, viewportW - width - 5, viewportH
					- HEIGHT - 55, width + 10, HEIGHT + 10, Color.BLACK);
			RectangleRenderer.draw(batch, viewportW - width, viewportH - HEIGHT
					- 50, width, HEIGHT, BG_COLOR);

			float scaleh = width / renderer.getWidth();
			renderer.draw(batch, viewportW - width / 2,
					viewportH - HEIGHT - 50, scaleh);

		}
	}

	public void update(float delta) {
		if (renderer != null && currentAnimation != null) {
			try {
				renderer.update(delta);
			} catch(Exception e) {
				
			}
		}
	}

	public void dispose() {
		if (renderer != null)
			renderer.dispose();
	}

}
