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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Align;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.EditAnimationDialog;

public class AnimationWidget extends Widget {
	private AnimationDesc fa;
	private ActorRenderer renderer;
	EditAnimationDialog editFADialog;

	public AnimationWidget(EditAnimationDialog createEditFADialog) {
		this.editFADialog = createEditFADialog;
	}

	public void setSource(String type, AnimationDesc anim) {
		fa = anim;

		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}

		if (type.equals(Project.S3D_RENDERER_STRING)) {
			renderer = new Sprite3DRenderer();
			((Sprite3DRenderer) renderer).setSpriteSize(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		} else if (type.equals(Project.SPINE_RENDERER_STRING)) {
			renderer = new SpineRenderer();
			((SpineRenderer) renderer).enableEvents(false);
		} else if (type.equals(Project.IMAGE_RENDERER_STRING)) {
			renderer = new ImageRenderer();
		} else {
			renderer = new AtlasRenderer();
		}

		renderer.setOrgAlign(Align.bottom);
		renderer.loadAssets();
		EngineAssetManager.getInstance().finishLoading();
		renderer.retrieveAssets();
	}

	public String[] getAnimations() {
		try {
			return ((AnimationRenderer) renderer).getInternalAnimations(fa);
		} catch (Exception e) {
			// Message.show(getStage(),
			// "Error loading animations from selected source", 4);
			EditorLogger.error("Error loading animations from selected source:" + fa.source + ": " + e.getMessage());
			return new String[0];
		}

	}

	public void setAnimation(String id, String speedStr, Tween.Type t) {

		if (fa != null && id != null && !id.isEmpty()) {

			if (fa instanceof AtlasAnimationDesc)
				((AtlasAnimationDesc) fa).regions = null;

			Tween.Type type = Tween.Type.REPEAT;
			float speed = 2.0f;

			if (speedStr != null && !speedStr.isEmpty()) {
				try {
					speed = Float.parseFloat(speedStr);
				} catch (NumberFormatException e) {
					speed = 0;
				}
			}

			if (t == Tween.Type.YOYO)
				type = Tween.Type.YOYO;
			else if (t == Tween.Type.REVERSE)
				type = Tween.Type.REVERSE_REPEAT;

			fa.id = id;
			fa.duration = speed;
			fa.animationType = type;
			fa.count = -1;

			((AnimationRenderer) renderer).getAnimations().clear();

			((AnimationRenderer) renderer).addAnimation(fa);
			((AnimationRenderer) renderer).startAnimation(fa.id, Tween.Type.SPRITE_DEFINED, 1, null);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);

		if (renderer == null || ((AnimationRenderer) renderer).getCurrentAnimation() == null)
			return;

		float tmp = batch.getPackedColor();
		batch.setColor(Color.WHITE);

		renderer.update(Gdx.graphics.getDeltaTime());

		RectangleRenderer.draw(batch, getX(), getY(), getWidth(), getHeight(), Color.MAGENTA);

		float scalew = getWidth() / renderer.getWidth();
		float scaleh = getHeight() / renderer.getHeight();
		float scale = scalew > scaleh ? scaleh : scalew;
		renderer.draw((SpriteBatch) batch, getX() + renderer.getWidth() * scale / 2, getY(), scale, scale, 0f, null);
		batch.setPackedColor(tmp);
	}

	public void dispose() {
		renderer.dispose();
	}
}
