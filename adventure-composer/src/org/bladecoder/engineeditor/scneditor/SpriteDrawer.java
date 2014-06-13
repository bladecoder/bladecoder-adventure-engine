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
package org.bladecoder.engineeditor.scneditor;

import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.AtlasRenderer;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.SpineRenderer;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Frame Animation renderer.
 * 
 * @author rgarcia
 */
public class SpriteDrawer {

	public static final Color BG_COLOR = Color.MAGENTA;
	private static final float HEIGHT = 200;

	FrameAnimation currentFrameAnimation;
	private SpriteRenderer renderer;
	private float viewportW, viewportH;

	public void setViewport(float w, float h) {
		viewportW = w;
		viewportH = h;
	}

	public void setActor(Actor a) {
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}

		if (a instanceof SpriteActor) {
			SpriteRenderer r = ((SpriteActor) a).getRenderer();

			if (r instanceof Sprite3DRenderer) {
				renderer = new Sprite3DRenderer();
				((Sprite3DRenderer) renderer).setSpriteSize(new Vector2(r
						.getWidth(), r.getHeight()));
			} else if (r instanceof SpineRenderer) {
				renderer = new SpineRenderer();
			} else {
				renderer = new AtlasRenderer();
			}
		}
	}

	public void setFrameAnimation(FrameAnimation fa) {
		currentFrameAnimation = fa;

		if (renderer != null) {

			renderer.getFrameAnimations().clear();

			if (fa != null) {

				renderer.addFrameAnimation(fa);

				renderer.retrieveAssets();

				renderer.startFrameAnimation(fa.id, Tween.REPEAT, Tween.INFINITY, null);
			}
		}
	}

	public void draw(SpriteBatch batch) {
		if (renderer != null && currentFrameAnimation != null) {

			float width = HEIGHT / renderer.getHeight() * renderer.getWidth();

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
		if (renderer != null && currentFrameAnimation != null) {
			renderer.update(delta);
		}
	}

	public void dispose() {
		if (renderer != null)
			renderer.dispose();
	}

}
