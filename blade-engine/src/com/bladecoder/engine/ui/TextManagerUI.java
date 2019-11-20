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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

/**
 * TextManagerUI draws texts and dialogs on screen.
 * 
 * For now, only one subtitle is displayed in the screen at a time.
 * 
 * @author rgarcia
 * 
 */
public class TextManagerUI extends Actor implements ITextManagerUI {
	private static final float PADDING = DPIUtils.getMarginSize();

	private final Vector3 unprojectTmp = new Vector3();

	private ObjectMap<String, TextManagerUIStyle> styles;
	private Text text;
	private final GlyphLayout layout = new GlyphLayout();

	private float fontX = 0;

	private AtlasRegion charIcon = null;

	private TextManagerUIStyle style;

	private float maxWidth;
	private final World world;

	public TextManagerUI(Skin skin, World w) {
		this.world = w;
		setTouchable(Touchable.disabled);
		styles = skin.getAll(TextManagerUIStyle.class);

		for (TextManagerUIStyle style : styles.values()) {
			style.font.getData().markupEnabled = true;
		}

		setVisible(false);
	}

	@Override
	public void setText(Text t) {
		text = t;

		if (t == null && isVisible()) {
			setVisible(false);
		} else if (t != null && !isVisible()) {
			setVisible(true);
		}

		if (isVisible()) {
			style = getStyle(text);
			Color color = text.color != null ? text.color : style.defaultColor;

			if (color == null)
				color = Color.BLACK;

			maxWidth = Math.min(getStage().getViewport().getScreenWidth() - DPIUtils.getMarginSize() * 2,
					style.font.getXHeight()
							* (text.type == Text.Type.TALK ? style.maxTalkCharWidth : style.maxCharWidth));

			layout.setText(style.font, text.str, color, maxWidth, Align.center, true);
			setSize(layout.width + PADDING * 2, layout.height + PADDING * 2);

			calcPos();
		}
	}

	private void calcPos() {
		float posx = text.x;
		float posy = text.y;

		unprojectTmp.set(posx, posy, 0);
		world.getSceneCamera().scene2screen(getStage().getViewport(), unprojectTmp);

		if (posx == TextManager.POS_CENTER || posx == TextManager.POS_SUBTITLE) {
			posx = getStage().getViewport().getScreenWidth() / 2;
			fontX = getStage().getViewport().getScreenWidth() / 2;
		} else {
			posx = unprojectTmp.x;
			fontX = posx + (layout.width - maxWidth) / 2;
		}

		if (posy == TextManager.POS_CENTER) {
			posy = (getStage().getViewport().getScreenHeight() - layout.height) / 2;
		} else if (posy == TextManager.POS_SUBTITLE) {
			posy = getStage().getViewport().getScreenHeight() * style.subtitlePosPercent - layout.height;

			if (posy < 0) {
				posy = PADDING;
			}
		} else {
			posy = unprojectTmp.y;

			if (text.type != Text.Type.TALK) {
				posy -= layout.height;
			}
		}

		// CHAR ICON CALCS
		if (text.type == Text.Type.SUBTITLE && !Config.getProperty(Config.CHARACTER_ICON_ATLAS, "").equals("")
				&& text.actorId != null) {
			charIcon = EngineAssetManager.getInstance().getRegion(Config.getProperty(Config.CHARACTER_ICON_ATLAS, null),
					text.actorId);

			if (charIcon != null) {
				float scale = getStage().getViewport().getScreenHeight() / (float) world.getHeight();
				float iconPosY = getStage().getViewport().getScreenHeight() - charIcon.getRegionHeight() * scale
						- DPIUtils.getMarginSize();
				posy = Math.min(posy, iconPosY);
			}
		} else {
			charIcon = null;
		}

		setPosition(posx - PADDING, posy - PADDING);

		if (text.type == Text.Type.TALK) {
			if (style.talkBubble != null) {
				float bubbleHeight = DPIUtils.getTouchMinSize() * style.bubbleSize / 4;
				setY(getY() + bubbleHeight + PADDING);
			} else {
				setY(getY() + PADDING);
			}
		}

		setX(getX() - layout.width / 2);

		fontX = posx - maxWidth / 2;

		// check if the text exits the screen
		if (getX() < 0 && getX() > -getWidth()) {
			setX(0 + PADDING);
			fontX = getX() + PADDING + (layout.width - maxWidth) / 2;
		} else if (getX() + getWidth() > getStage().getViewport().getScreenWidth()
				&& getX() + getWidth() < getStage().getViewport().getScreenWidth() + getWidth()) {
			setX(getStage().getViewport().getScreenWidth() - getWidth() - PADDING);
			fontX = getStage().getViewport().getScreenWidth() - layout.width / 2 - PADDING * 2 - maxWidth / 2;
		}

		if (getY() + getHeight() > getStage().getViewport().getScreenHeight()) {
			setY(getStage().getViewport().getScreenHeight() - getHeight() - PADDING);
		}
	}

	@Override
	public void draw(Batch batch, float alpha) {

		if (text.type == Text.Type.TALK) {

			// When the type is TALK we recalc the pos because of scrolling
			calcPos();

			if (getX() < 0 || getX() > getStage().getViewport().getScreenWidth())
				return;

			if (style.talkBubble != null) {
				float scale = DPIUtils.getTouchMinSize() * style.bubbleSize / 4 / style.talkBubble.getMinHeight();

				float bubbleX = unprojectTmp.x - style.talkBubble.getMinWidth() * scale / 2 + style.bubbledx;
				// check screen exit
				bubbleX = Math.max(bubbleX, getX() + PADDING - style.talkBubble.getMinWidth() * scale / 2);
				bubbleX = Math.min(bubbleX, getX() + getWidth() - PADDING - style.talkBubble.getMinWidth() * scale / 2);
				float bubbleY = getY() - style.talkBubble.getMinHeight() * scale + style.bubbledy;

				style.talkBubble.draw(batch, bubbleX, bubbleY, style.talkBubble.getMinWidth() * scale,
						style.talkBubble.getMinHeight() * scale);
			}

			if (style.talkBackground != null) {
				style.talkBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}

		} else if (text.type == Text.Type.SUBTITLE) {
			if (style.rectBackground != null) {
				style.rectBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}

			if (charIcon != null) {
				float scale = getStage().getViewport().getScreenHeight() / (float) world.getHeight();
				batch.draw(charIcon, getX() - charIcon.getRegionWidth() * scale, getY(),
						charIcon.getRegionWidth() * scale, charIcon.getRegionHeight() * scale);
			}

		}

		style.font.draw(batch, layout, fontX, getY() + PADDING + layout.height);
	}

	private TextManagerUIStyle getStyle(Text text) {
		String key = "default";

		if (text != null && text.style != null && !text.style.isEmpty()) {
			key = text.style;
		} else if (text.actorId != null) {
			CharacterActor a = (CharacterActor) world.getCurrentScene().getActor(text.actorId, false);

			if (a != null && a.getTextStyle() != null)
				key = a.getTextStyle();
		}

		TextManagerUIStyle s = styles.get(key);

		if (s == null) {
			EngineLogger.error("TextManagerUIStyle not found: " + key);
			return styles.get("default");
		}

		return s;
	}

	/** The style for the TextManagerUI */
	static public class TextManagerUIStyle {
		/** Optional. */
		public Drawable rectBackground;
		public Drawable talkBackground;
		public Drawable talkBubble;
		public BitmapFont font;
		public Color defaultColor;
		public float subtitlePosPercent = 0.90f;
		public float bubbleSize = 1f;
		public float bubbledx = 0;
		public float bubbledy = 0;
		public int maxCharWidth = 80;
		public int maxTalkCharWidth = 35;

		public TextManagerUIStyle() {
		}

		public TextManagerUIStyle(TextManagerUIStyle style) {
			rectBackground = style.rectBackground;
			talkBackground = style.talkBackground;
			talkBubble = style.talkBubble;
			font = style.font;
			defaultColor = style.defaultColor;
			subtitlePosPercent = style.subtitlePosPercent;
			bubbleSize = style.bubbleSize;
			bubbledx = style.bubbledx;
			bubbledy = style.bubbledy;
			maxCharWidth = style.maxCharWidth;
			maxTalkCharWidth = style.maxTalkCharWidth;
		}
	}
}
