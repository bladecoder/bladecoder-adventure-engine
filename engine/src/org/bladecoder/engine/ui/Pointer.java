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
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Pointer {
	private static final String FONT_STYLE = "POINTER_FONT";
	private static final String LEAVE_ICON = "leave3";
	private static final String POINTER_ICON = "pointer3";
	private static final String HOTSPOT_ICON = "hotspotpointer3";

	private BitmapFont font;

	private float scale = 1.0f;

	private boolean freezeHotSpot = false;
	private final Vector2 freezePos = new Vector2();

	private boolean showDesc = true;
	private String desc = null;

	private TextureRegion leaveIcon;
	private TextureRegion pointerIcon;
	private TextureRegion hotspotIcon;
	private TextureRegion currentIcon;

	private final Vector2 mousepos = new Vector2();

	public Pointer() {
		reset();
	}

	public void reset() {
		setDefaultIcon();
		freezeHotSpot = false;
		desc=null;
	}

	public void setDefaultIcon() {
		currentIcon = pointerIcon;
		
		if(!freezeHotSpot)
			desc = null;
	}

	public void setLeaveIcon() {
		if (!freezeHotSpot)
			currentIcon = leaveIcon;
	}

	public void setHotspotIcon() {
		if (!freezeHotSpot)
			currentIcon = hotspotIcon;
	}

	public void setIcon(TextureRegion r) {
		currentIcon = r;
	}

	public void draw(SpriteBatch batch, Viewport v) {
		draw(batch, false, v);
	}

	public void setDesc(String s) {
		if (!freezeHotSpot) {
			desc = s;

			if (desc.charAt(0) == '@')
				desc = I18N.getString(desc.substring(1));
		}
	}

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
		if (showDesc && desc != null) {
			int margin = 50;

			TextBounds b = font.getBounds(desc);

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

			
			RectangleRenderer.draw(batch, textX - 8, textY - b.height - 8, b.width + 16, b.height + 16, Color.BLACK);
			font.draw(batch, desc, textX, textY);
		}

		if (!dragging) {
			if (!Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen)
					|| currentIcon == leaveIcon) {

				batch.draw(currentIcon,
						mousepos.x - currentIcon.getRegionWidth() * scale / 2,
						mousepos.y - currentIcon.getRegionHeight() * scale / 2,
						currentIcon.getRegionWidth() * scale,
						currentIcon.getRegionHeight() * scale);
			}
		}
	}

	public void setFreezeHotSpot(boolean freeze, Viewport v) {
		freezeHotSpot = freeze;
		setDefaultIcon();
		getInputUnproject(v, freezePos);
	}

	public void retrieveAssets(TextureAtlas atlas) {
		pointerIcon = atlas.findRegion(POINTER_ICON);
		leaveIcon = atlas.findRegion(LEAVE_ICON);
		hotspotIcon = atlas.findRegion(HOTSPOT_ICON);
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
