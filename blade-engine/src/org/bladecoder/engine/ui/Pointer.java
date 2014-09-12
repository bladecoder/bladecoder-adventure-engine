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
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.util.DPIUtils;
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
	
	private static final Color DRAG_NOT_HOTSPOT_COLOR = new Color(.5f, 0.5f, 0.5f, 1f);

	private BitmapFont font;

	private String desc = null;

	private TextureRegion leaveIcon;
	private TextureRegion pointerIcon;
	private TextureRegion hotspotIcon;
	private TextureRegion currentIcon;
	private SpriteRenderer draggingRenderer;

	private final Vector2 mousepos = new Vector2();
	
	private float pointerScale;

	public Pointer() {
		reset();
	}

	public void reset() {
		setDefaultIcon();
		draggingRenderer = null;
	}
	
	public void drag(SpriteRenderer r) {
		draggingRenderer = r;
	}

	public void setDefaultIcon() {
		currentIcon = pointerIcon;
		desc = null;
	}

	public void setLeaveIcon() {
		currentIcon = leaveIcon;
	}

	public void setHotspotIcon() {
		currentIcon = hotspotIcon;
	}

	public void setIcon(TextureRegion r) {
		currentIcon = r;
	}

	public void setDesc(String s) {
		desc = s;

		if (desc != null && desc.charAt(0) == '@')
			desc = I18N.getString(desc.substring(1));
	}

	private void getInputUnproject(Viewport v, Vector2 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY());

		v.unproject(out);
	}

	public void drawHotspot(SpriteBatch batch, float x, float y, String desc) {

		if (desc == null) {
			batch.setColor(Color.BLUE);
			batch.draw(hotspotIcon, x - hotspotIcon.getRegionWidth() * pointerScale
					/ 2, y - hotspotIcon.getRegionHeight() * pointerScale / 2,
					hotspotIcon.getRegionWidth() * pointerScale,
					hotspotIcon.getRegionHeight() * pointerScale);
			batch.setColor(Color.WHITE);
		} else {
			if (desc != null && desc.charAt(0) == '@')
				desc = I18N.getString(desc.substring(1));

			TextBounds b = font.getBounds(desc);

			float textX = x - b.width / 2;
			float textY = y + b.height;

			RectangleRenderer.draw(batch, textX - 8, textY - b.height - 8,
					b.width + 16, b.height + 16, Color.BLACK);
			font.draw(batch, desc, textX, textY);
		}
	}

	public void draw(SpriteBatch batch, Viewport v) {

		getInputUnproject(v, mousepos);

		// DRAW TARGET DESCRIPTION
		if (desc != null) {
			TextBounds b = font.getBounds(desc);
			float margin = DPIUtils.UI_SPACE;

			float textX = mousepos.x - b.width / 2;
			float textY = mousepos.y + b.height + DPIUtils.UI_SPACE + DPIUtils.getMinSize();

			if (textX < 0)
				textX = 0;

			RectangleRenderer.draw(batch, textX - margin, textY - b.height - margin,
					b.width + margin*2, b.height + margin*2, Color.BLACK);
			font.draw(batch, desc, textX, textY);
		}

		if (draggingRenderer == null) {
			if (!Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen)
					|| currentIcon == leaveIcon) {

				batch.draw(currentIcon,
						mousepos.x - currentIcon.getRegionWidth() * pointerScale
								/ 2, mousepos.y - currentIcon.getRegionHeight()
								* pointerScale / 2, currentIcon.getRegionWidth()
								* pointerScale, currentIcon.getRegionHeight()
								* pointerScale);
			}
		} else {
			float h = (draggingRenderer.getHeight() > draggingRenderer.getWidth()? draggingRenderer.getHeight():draggingRenderer.getWidth());
			float size =  DPIUtils.getMinSize() / h * 1.8f;
	         
	        if(currentIcon != hotspotIcon) {
	        	batch.setColor(DRAG_NOT_HOTSPOT_COLOR);
	        }
	     	
	        draggingRenderer.draw(batch, mousepos.x,
	        		mousepos.y - draggingRenderer.getHeight() * size / 2, size);
	        
	        if(currentIcon != hotspotIcon) {
	        	batch.setColor(Color.WHITE);
	        }	
		}
	}

	public void retrieveAssets(TextureAtlas atlas) {
		pointerIcon = atlas.findRegion(POINTER_ICON);
		leaveIcon = atlas.findRegion(LEAVE_ICON);
		hotspotIcon = atlas.findRegion(HOTSPOT_ICON);

		if (font == null)
			font = EngineAssetManager.getInstance().loadFont(FONT_STYLE);
		
		
	}

	public void resize(int width, int height) {
		pointerScale = DPIUtils.getMinSize(width, height) / pointerIcon.getRegionHeight() * .8f;
	}

	public void dispose() {
		EngineAssetManager.getInstance().disposeFont(font);
		font = null;
	}

}
