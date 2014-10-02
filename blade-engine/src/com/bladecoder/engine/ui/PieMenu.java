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
package com.bladecoder.engine.ui;

import com.bladecoder.engine.ui.SceneScreen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Actor;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.RectangleRenderer;

public class PieMenu extends com.badlogic.gdx.scenes.scene2d.Actor {

	private BitmapFont font;

	private AtlasRegion piePiece;
	private AtlasRegion rightIcon;
	private AtlasRegion leftIcon;

	private AtlasRegion talktoIcon;
	private AtlasRegion pickupIcon;

	private float scale;
	private float x = 0, y = 0;
	private String selected;

	private Actor actor = null;

	private String rightVerb;
	private String leftVerb;

	private final SceneScreen sceneScreen;

	private final static Color COLOR = new Color(0.0f, 0.0f, 0.0f, 0.7f);

	int viewPortWidth, viewPortHeight;

	public PieMenu(SceneScreen scr) {
		sceneScreen = scr;
		font = scr.getUI().getSkin().getFont("desc");
		piePiece = scr.getUI().getSkin().getAtlas().findRegion("pie");
		// rightIcon = atlas.findRegion("pickup");
		leftIcon = scr.getUI().getSkin().getAtlas().findRegion("lookat");

		talktoIcon = scr.getUI().getSkin().getAtlas().findRegion("talkto");
		pickupIcon = scr.getUI().getSkin().getAtlas().findRegion("pickup");

		addListener(new InputListener() {
			@Override
			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				if (selected != null && actor != null) {
					sceneScreen.runVerb(actor, selected, null);
				}

				hide();
				selected = null;
			}

			@Override
			public boolean mouseMoved(InputEvent event, float x, float y) {

				return true;
			}

			@Override
			public boolean touchDown(InputEvent event, float x2, float y2,
					int pointer, int button) {

				Rectangle bboxLeft;
				Rectangle bboxRight;

				bboxLeft = new Rectangle(x - piePiece.getRegionWidth() * scale,
						y, piePiece.getRegionWidth() * scale,
						piePiece.getRegionHeight() * scale);
				bboxRight = new Rectangle(x, y, piePiece.getRegionWidth()
						* scale, piePiece.getRegionHeight() * scale);

				if (bboxLeft.contains(x2, y2)) {
					selected = leftVerb;
				} else if (bboxRight.contains(x2, y2)) {
					selected = rightVerb;
				} else {
					selected = null;
				}

				if (selected == null) {
					hide();
					return false;
				}

				return true;
			}
		});
	}

	@Override
	public void draw(Batch batch, float alpha) {

		float leftX, leftY, rightX, rightY;

		leftX = x - (piePiece.getRegionWidth() + leftIcon.getRegionWidth())
				* scale / 2;
		leftY = y + (piePiece.getRegionHeight() - leftIcon.getRegionHeight())
				* scale / 2;
		rightX = x + (piePiece.getRegionWidth() - rightIcon.getRegionWidth())
				* scale / 2;
		rightY = y + (piePiece.getRegionHeight() - rightIcon.getRegionHeight())
				* scale / 2;

		if (selected != leftVerb)
			batch.setColor(COLOR);
		else
			batch.setColor(Color.LIGHT_GRAY);

		batch.draw(piePiece, x - piePiece.getRegionWidth(), y,
				piePiece.getRegionWidth(), 0, piePiece.getRegionWidth(),
				piePiece.getRegionHeight(), scale, scale, 0);

		if (selected != rightVerb)
			batch.setColor(COLOR);
		else
			batch.setColor(Color.LIGHT_GRAY);

		batch.draw(piePiece, x - piePiece.getRegionWidth(), y,
				piePiece.getRegionWidth(), 0, piePiece.getRegionWidth(),
				piePiece.getRegionHeight(), -scale, scale, 0);

		batch.setColor(Color.WHITE);

		batch.draw(leftIcon, leftX, leftY, 0, 0, leftIcon.getRegionWidth(),
				leftIcon.getRegionHeight(), scale, scale, 0);

		batch.draw(rightIcon, rightX, rightY, 0, 0, rightIcon.getRegionWidth(),
				rightIcon.getRegionHeight(), scale, scale, 0);

		// DRAW TARGET DESCRIPTION
		String desc = actor.getDesc();

		if (desc != null) {

			if (desc.charAt(0) == '@')
				desc = I18N.getString(desc.substring(1));
					
			TextBounds b = font.getBounds(desc);
			float margin = DPIUtils.UI_SPACE;

			float textX = x - b.width / 2;
			float textY = y - b.height - DPIUtils.UI_SPACE;

			if (textX < 0)
				textX = 0;

			RectangleRenderer.draw(batch, textX - margin, textY - b.height - margin,
					b.width + margin*2, b.height + margin*2, Color.BLACK);
			font.draw(batch, desc, textX, textY);
		}
	}

	public void hide() {
		setVisible(false);
		actor = null;
	}

	public void show(Actor a, float x, float y) {
		setVisible(true);
		this.x = x;
		this.y = y;
		actor = a;

		leftVerb = "lookat";

		if (a.getVerb("talkto") != null) {
			rightVerb = "talkto";
			rightIcon = talktoIcon;
		} else {
			rightVerb = "pickup";
			rightIcon = pickupIcon;
		}
		
		// FITS TO SCREEN
		if(x < piePiece.getRegionWidth() * scale)
			this.x = piePiece.getRegionWidth() * scale;
		else if(x > viewPortWidth - piePiece.getRegionWidth() * scale)
			this.x = viewPortWidth - piePiece.getRegionWidth() * scale;
		
		if(y < piePiece.getRegionHeight() * scale)
			this.y = piePiece.getRegionHeight() * scale;
		else if(y > viewPortHeight - piePiece.getRegionHeight() * scale)
			this.y = viewPortHeight - piePiece.getRegionHeight() * scale;
	}

	public void resize(int width, int height) {
		viewPortWidth = width;
		viewPortHeight = height;

		setBounds(0, 0, width, height);

		scale = DPIUtils.getPrefButtonSize(width, height) * 1.7f
				/ piePiece.getRegionHeight();
	}
}
