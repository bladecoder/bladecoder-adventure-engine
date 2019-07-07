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
package com.bladecoder.engine.ui.defaults;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.ui.AnimationDrawable;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.RectangleRenderer;

public class ScenePointer {
	private static final String LEAVE_ICON = "leave";
	private static final String POINTER_ICON = "pointer";
	private static final String HOTSPOT_ICON = "hotspotpointer";

	private static final Color DRAG_NOT_HOTSPOT_COLOR = new Color(.5f, 0.5f, 0.5f, 1f);

	private Color tmpTint = new Color();

	private BitmapFont font;

	private String desc = null;

	private Drawable leaveIcon;
	private Drawable pointerIcon;
	private Drawable hotspotIcon;
	private Drawable currentIcon;
	private SpriteActor draggingActor;

	private final Vector2 mousepos = new Vector2();

	private float pointerScale;
	private float leaveRotation = 0f;
	// private Skin skin;

	private final GlyphLayout layout = new GlyphLayout();

	public ScenePointer(Skin skin) {
		// this.skin = skin;
		font = skin.getFont("desc");
		pointerIcon = skin.getDrawable(POINTER_ICON);
		leaveIcon = skin.getDrawable(LEAVE_ICON);
		hotspotIcon = skin.getDrawable(HOTSPOT_ICON);
		reset();
	}

	public void reset() {
		setDefaultIcon();
		draggingActor = null;
	}

	public void drag(SpriteActor a) {
		draggingActor = a;
		tmpTint.set(DRAG_NOT_HOTSPOT_COLOR);

		if (a != null && draggingActor.getTint() != null)
			tmpTint.mul(draggingActor.getTint());
	}

	public void setDefaultIcon() {
		currentIcon = pointerIcon;
		desc = null;
	}

	public void setLeaveIcon(float rot) {
		currentIcon = leaveIcon;
		leaveRotation = rot;
	}

	public void setHotspotIcon() {
		currentIcon = hotspotIcon;
	}

	public void setIcon(Drawable r) {
		currentIcon = r;
	}

	public void setDesc(String s) {
		desc = s;

		if (desc != null) {

			if (desc.charAt(0) == I18N.PREFIX)
				desc = I18N.getString(desc.substring(1));

			layout.setText(font, desc);
		}
	}

	private void getInputUnproject(Viewport v, Vector2 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY());

		v.unproject(out);
	}

	public void update(float delta) {
		if (pointerIcon instanceof AnimationDrawable)
			((AnimationDrawable) pointerIcon).act(delta);
	}

	public void draw(SpriteBatch batch, Viewport v) {

		getInputUnproject(v, mousepos);

		boolean multiTouch = Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen);

		// DRAW TARGET DESCRIPTION
		if (desc != null && (!multiTouch || Gdx.input.isTouched())) {
			float margin = DPIUtils.UI_SPACE;

			float textX = mousepos.x - layout.width / 2;
			float textY = mousepos.y + layout.height + DPIUtils.UI_SPACE + DPIUtils.getTouchMinSize();

			if (textX < 0)
				textX = 0;

			RectangleRenderer.draw(batch, textX - margin, textY - layout.height - margin, layout.width + margin * 2,
					layout.height + margin * 2, Color.BLACK);
			font.draw(batch, layout, textX, textY);
		}

		// batch.setColor(Color.WHITE);

		if (draggingActor == null) {
			if (!multiTouch || (currentIcon == leaveIcon && Gdx.input.isTouched())) {

				if (currentIcon instanceof TransformDrawable) {

					TransformDrawable i = (TransformDrawable) currentIcon;

					i.draw(batch, mousepos.x - currentIcon.getMinWidth() / 2,
							mousepos.y - currentIcon.getMinHeight() / 2, currentIcon.getMinWidth() / 2,
							currentIcon.getMinHeight() / 2, currentIcon.getMinWidth(), currentIcon.getMinHeight(),
							pointerScale, pointerScale, currentIcon == leaveIcon ? leaveRotation : 0);
				} else {
					float width = currentIcon.getMinWidth() * pointerScale;
					float height = currentIcon.getMinHeight() * pointerScale;
					float x = mousepos.x - width / 2;
					float y = mousepos.y - height / 2;

					currentIcon.draw(batch, x, y, width, height);
				}
			}
		} else {
			float h = (draggingActor.getHeight() > draggingActor.getWidth() ? draggingActor.getHeight()
					: draggingActor.getWidth());
			float size = DPIUtils.getTouchMinSize() / h * 1.8f;

			ActorRenderer r = draggingActor.getRenderer();

			r.draw(batch, mousepos.x, mousepos.y - draggingActor.getHeight() * size / 2, size, size, 0f,
					currentIcon != hotspotIcon ? tmpTint : draggingActor.getTint());
		}
	}

	public void resize(int width, int height) {
		pointerScale = DPIUtils.getTouchMinSize() / pointerIcon.getMinHeight() * .8f;
	}

}
