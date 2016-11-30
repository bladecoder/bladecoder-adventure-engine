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
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;

/**
 * TextManagerUI draws texts and dialogs on screen.
 * 
 * For now, only one subtitle is displayed in the screen.
 * 
 * @author rgarcia
 * 
 */
public class TextManagerUI extends Actor {
	private static final float PADDING = DPIUtils.getMarginSize();

	private final Vector3 unprojectTmp = new Vector3();

	private ObjectMap<String, TextManagerUIStyle> styles;
	private Text subtitle;
	private final GlyphLayout layout = new GlyphLayout();

	private float fontX = 0;

	private AtlasRegion charIcon = null;

	public TextManagerUI(Skin skin) {
		setTouchable(Touchable.disabled);
		styles = skin.getAll(TextManagerUIStyle.class);

		for (TextManagerUIStyle style : styles.values()) {
			style.font.getData().markupEnabled = true;
		}

		setVisible(false);
	}

	@Override
	public void act(float delta) {
		super.act(delta);

		Text currentSubtitle = World.getInstance().getTextManager().getCurrentText();

		if (subtitle != currentSubtitle) {
			subtitle = currentSubtitle;

			if (currentSubtitle == null && isVisible()) {
				setVisible(false);
			} else if (currentSubtitle != null && !isVisible()) {
				setVisible(true);
			}

			if (isVisible()) {
				float posx = currentSubtitle.x;
				float posy = currentSubtitle.y;

				unprojectTmp.set(posx, posy, 0);
				World.getInstance().getSceneCamera().scene2screen(getStage().getViewport(), unprojectTmp);
				
				final TextManagerUIStyle style = getStyle(currentSubtitle);
				
				float maxWidth = Math.min(getStage().getViewport().getScreenWidth() - DPIUtils.getMarginSize() * 2,
						style.font.getXHeight() * (currentSubtitle.type == Text.Type.TALK ? style.maxTalkCharWidth : style.maxCharWidth));

				Color color = currentSubtitle.color != null ? currentSubtitle.color : style.defaultColor;

				if (color == null)
					color = Color.BLACK;

				layout.setText(style.font, currentSubtitle.str, color, maxWidth, Align.center, true);

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
					
					if (subtitle.type != Text.Type.TALK) {
						posy -= layout.height;
					}
				}

				// CHAR ICON CALCS
				if (!Config.getProperty(Config.CHARACTER_ICON_ATLAS, "").equals("") && subtitle.actorId != null) {
					charIcon = EngineAssetManager.getInstance()
							.getRegion(Config.getProperty(Config.CHARACTER_ICON_ATLAS, null), subtitle.actorId);
					
					if(charIcon != null) {
						float scale = getStage().getViewport().getScreenHeight() / (float)World.getInstance().getHeight();
						float iconPosY = getStage().getViewport().getScreenHeight() - charIcon.getRegionHeight() * scale - DPIUtils.getMarginSize();
						posy = Math.min(posy, iconPosY);
					}
				} else {
					charIcon = null;
				}

				setPosition(posx - PADDING, posy - PADDING);
				setSize(layout.width + PADDING * 2, layout.height + PADDING * 2);

				if (currentSubtitle.type == Text.Type.TALK) {
					if (style.talkBubble != null) {
						setY(getY() + DPIUtils.getTouchMinSize() / 3 + PADDING);
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
		}

	}

	@Override
	public void draw(Batch batch, float alpha) {
		batch.setColor(Color.WHITE);

		final TextManagerUIStyle style = getStyle(subtitle);

		if (subtitle.type == Text.Type.TALK) {
			if (getX() < 0 || getX() > getStage().getViewport().getScreenWidth())
				return;

			if (style.talkBubble != null) {
				float scale = DPIUtils.getTouchMinSize()  * style.bubbleSize / 4 / style.talkBubble.getMinHeight();

				float bubbleX = unprojectTmp.x - style.talkBubble.getMinWidth() * scale / 2;
				bubbleX = Math.max(bubbleX, getX() + PADDING);
				bubbleX = Math.min(bubbleX, getStage().getViewport().getScreenWidth() - PADDING);
				float bubbleY = getY() - style.talkBubble.getMinHeight() * scale + 2;

				style.talkBubble.draw(batch, bubbleX, bubbleY, style.talkBubble.getMinWidth() * scale,
						style.talkBubble.getMinHeight() * scale);
			}

			if (style.talkBackground != null) {
				style.talkBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}

		} else if (subtitle.type == Text.Type.SUBTITLE) {
			if (style.rectBackground != null) {
				style.rectBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}

			if (charIcon != null) {
				float scale = getStage().getViewport().getScreenHeight() / (float)World.getInstance().getHeight();
				batch.draw(charIcon, getX() - charIcon.getRegionWidth() * scale, getY(),
						charIcon.getRegionWidth() * scale, charIcon.getRegionHeight() * scale);
			}

		}

		style.font.draw(batch, layout, fontX, getY() + PADDING + layout.height);
	}

	public void resize() {
	}

	private TextManagerUIStyle getStyle(Text text) {
		String key = "default";
		if (text != null) {
			key = text.style;
		}
		if (key == null || key.isEmpty()) {
			key = "default";
		}
		return styles.get(key);
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
			maxCharWidth = style.maxCharWidth;
			maxTalkCharWidth = style.maxTalkCharWidth;
		}
	}
}
