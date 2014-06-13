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
package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.i18n.I18N;
import org.bladecoder.engine.model.Actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Pointer {
	private static final String FONT_STYLE = "POINTER_FONT";

	private BitmapFont font;
	private Sprite pointer;
	private Sprite leave;

	private Sprite pickupIcon;
	private Sprite lookatIcon;
	private Sprite talktoIcon;

	private float scale = 1.0f;

	private boolean showAction = true;
	private Actor target = null;

	private boolean freezeHotSpot = false;
	private final Vector2 freezePos = new Vector2();

	// True for lookat, false for talkto, pickup or leave
	private boolean selectedVerbLookat = true;

	public Pointer() {
	}

	public void draw(SpriteBatch batch, Viewport v) {
		draw(batch, false, v);
	}

	private final Vector2 mousepos = new Vector2();

	private void getInputUnproject(Viewport v, Vector2 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY());

		v.unproject(out);

		if (out.x >= v.getViewportWidth())
			out.x = v.getViewportWidth() - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= v.getViewportHeight())
			out.y = v.getViewportHeight() - 1;
		else if (out.y < 0)
			out.y = 0;
	}

	public void draw(SpriteBatch batch, boolean dragging, Viewport v) {

		getInputUnproject(v, mousepos);

		// DRAW TARGET DESCRIPTION
		if (target != null && target.getDesc() != null) {
			String str = target.getDesc();

			if (str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));

			int margin = 40;

			TextBounds b = font.getBounds(str);

			float x0 = mousepos.x;

			float y0 = mousepos.y + margin + b.height;

			float textX = x0 - b.width / 2;
			float textY = y0;

			if (freezeHotSpot) {
				textX = freezePos.x - b.width / 2;
				textY = freezePos.y;
			}

			if (textX < 0)
				textX = 0;

			font.draw(batch, str, textX, textY);

			x0 = x0 - lookatIcon.getWidth() / 2;
			y0 += margin / 4;

		}

		if (!dragging) {

			if (showAction) {
				if (target != null && target.getVerb("leave") == null) {
					if (selectedVerbLookat) {
						lookatIcon.setPosition(
								mousepos.x - lookatIcon.getWidth() / 2,
								mousepos.y - lookatIcon.getHeight() / 2);
						lookatIcon.draw(batch);
					} else {

						if (target.getVerb("talkto") != null) {
							talktoIcon.setPosition(
									mousepos.x - talktoIcon.getWidth() / 2,
									mousepos.y - talktoIcon.getHeight() / 2);
							talktoIcon.draw(batch);
						} else {
							pickupIcon.setPosition(
									mousepos.x - pickupIcon.getWidth() / 2,
									mousepos.y - pickupIcon.getHeight() / 2);
							pickupIcon.draw(batch);
						}
					}
				}
			}

			// leave action always shows
			if (target != null && target.getVerb("leave") != null) {
				leave.setPosition(mousepos.x - leave.getWidth() / 2, mousepos.y
						- leave.getHeight() / 2);
				leave.draw(batch);
			} else if (!Gdx.input
					.isPeripheralAvailable(Peripheral.MultitouchScreen)
					&& (!showAction || target == null)) {
				pointer.setPosition(mousepos.x - pointer.getWidth() / 2,
						mousepos.y - pointer.getHeight() / 2);

				pointer.draw(batch);
			}
		}
	}

	public void setTarget(Actor target) {
		if (!freezeHotSpot) {
			this.target = target;
		}
	}

	public Actor getTarget() {
		return target;
	}

	public void setFreezeHotSpot(boolean freeze, Viewport v) {
		freezeHotSpot = freeze;
		getInputUnproject(v, freezePos);
	}

	public void retrieveAssets(TextureAtlas atlas) {
		pointer = atlas.createSprite("pointer");
		leave = atlas.createSprite("leave");

		pickupIcon = atlas.createSprite("pickup");
		lookatIcon = atlas.createSprite("lookat");
		talktoIcon = atlas.createSprite("talkto");
	}

	public void toggleSelectedVerb() {
		selectedVerbLookat = !selectedVerbLookat;
	}

	public String getSelectedVerb() {
		if (target != null && target.getVerb("leave") == null) {
			if (selectedVerbLookat) {
				return "lookat";
			} else {
				if (target.getVerb("talkto") != null) {
					return "talkto";
				} else {
					return "pickup";
				}
			}
		}

		return null;
	}

	public void resize(int width, int height) {

		// Rectangle v = World.getInstance().getCameras().getViewport();

		// scale = (v.height / 20) / pointer.getHeight();
		//
		// // the minimum height of the inventory is 1/2"
		// if (scale * pointer.getHeight() < 160.0f * Gdx.graphics
		// .getDensity() / 4f) {
		// scale = 160.0f * Gdx.graphics.getDensity() / 2f
		// / pointer.getHeight();
		// }

		scale = 1;

		pointer.setOrigin(pointer.getWidth() / 2, pointer.getHeight() / 2);
		pointer.setScale(scale);

		leave.setOrigin(leave.getWidth() / 2, leave.getHeight() / 2);
		leave.setScale(scale);

		pickupIcon.setOrigin(pickupIcon.getWidth() / 2,
				pickupIcon.getHeight() / 2);
		pickupIcon.setScale(scale);

		lookatIcon.setOrigin(lookatIcon.getWidth() / 2,
				lookatIcon.getHeight() / 2);
		lookatIcon.setScale(scale);

		talktoIcon.setOrigin(talktoIcon.getWidth() / 2,
				talktoIcon.getHeight() / 2);
		talktoIcon.setScale(scale);
	}

	public void setShowAction(boolean show) {
		showAction = show;
	}

	public void createAssets() {
		if (font != null)
			font.dispose();

		font = EngineAssetManager.getInstance().loadFont(FONT_STYLE);
	}

	public void dispose() {
		font.dispose();
		font = null;
	}

}
