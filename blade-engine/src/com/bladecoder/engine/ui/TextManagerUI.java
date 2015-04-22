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

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
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

	private float maxRectangleWidth;
	private float maxTalkWidth;

	private SceneScreen sceneScreen;
	private final Vector3 unprojectTmp = new Vector3();

	private TextManagerUIStyle style;
	private Text subtitle;
	private final GlyphLayout layout = new GlyphLayout();
	
	private float fontX = 0;

	public TextManagerUI(SceneScreen sceneScreen) {
		this.sceneScreen = sceneScreen;
		setTouchable(Touchable.disabled);
		style = sceneScreen.getUI().getSkin().get(TextManagerUIStyle.class);
		style.font.getData().markupEnabled = true;
		setVisible(false);
	}

	@Override
	public void act(float delta) {
		super.act(delta);

		Text currentSubtitle = World.getInstance().getTextManager().getCurrentSubtitle();

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
				World.getInstance().getSceneCamera().scene2screen(sceneScreen.getViewport(), unprojectTmp);
				
				if (currentSubtitle.type == Text.Type.TALK) {
					layout.setText(style.font, currentSubtitle.str, currentSubtitle.color, maxTalkWidth, Align.center, true);
				} else {
					layout.setText(style.font, currentSubtitle.str, currentSubtitle.color, maxRectangleWidth, Align.center, true);
				}

				if (posx == TextManager.POS_CENTER || posx == TextManager.POS_SUBTITLE) {					
					posx = (sceneScreen.getViewport().getScreenWidth() - layout.width)/2;
					fontX = (sceneScreen.getViewport().getScreenWidth() - maxRectangleWidth)/2;
				} else {
					posx = unprojectTmp.x;
				}

				if (posy == TextManager.POS_CENTER) {
					posy = (sceneScreen.getViewport().getScreenHeight() - layout.height)/2;
				} else if (posy == TextManager.POS_SUBTITLE) {
					posy = sceneScreen.getViewport().getScreenHeight() - layout.height - DPIUtils.getMarginSize() * 4;
				} else {
					posy = unprojectTmp.y;
				}

				setPosition(posx - PADDING, posy - PADDING);

				if (currentSubtitle.type == Text.Type.TALK) {
					if (style.talkBubble != null) {
						setY(getY() + DPIUtils.getTouchMinSize() / 3 + PADDING);
					}
					
					setX(getX() - layout.width / 2);
					
					fontX = posx - maxTalkWidth / 2;
				}

				setSize(layout.width + PADDING * 2, layout.height + PADDING * 2);
				
				// check if the text exits the screen
				if (getX() < 0) {
					setX(0);
				} else if (getX() + getWidth() > sceneScreen.getViewport().getScreenWidth()) {
					setX(sceneScreen.getViewport().getScreenWidth() - getWidth());
				}

				if (getY() + getHeight() > sceneScreen.getViewport().getScreenHeight()) {
					setY(sceneScreen.getViewport().getScreenHeight() - getHeight());
				}
			}
		}

	}

	@Override
	public void draw(Batch batch, float alpha) {

		if (subtitle.type == Text.Type.TALK) {
			if (style.talkBubble != null) {
				float scale = DPIUtils.getTouchMinSize() / 4 / style.talkBubble.getMinHeight();
//				float bubbleX = getX() + (getWidth()  - style.talkBubble.getMinWidth() * scale)/ 2;
				unprojectTmp.set(subtitle.x, subtitle.y, 0);
				World.getInstance().getSceneCamera().scene2screen(sceneScreen.getViewport(), unprojectTmp);
				
				float bubbleX = unprojectTmp.x  - style.talkBubble.getMinWidth() * scale / 2;
				
				float bubbleY = getY() - style.talkBubble.getMinHeight() * scale + 2;

//				style.talkBubble.draw(batch, bubbleX, bubbleY, style.talkBubble.getMinWidth() * scale,
//						style.talkBubble.getMinHeight() * scale);
				
				style.talkBubble.draw(batch, bubbleX, bubbleY, style.talkBubble.getMinWidth() * scale,
						style.talkBubble.getMinHeight() * scale);
			}

			if (style.talkBackground != null) {
				style.talkBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}

		} else if (subtitle.type == Text.Type.RECTANGLE) {
			if (style.rectBackground != null) {
				style.rectBackground.draw(batch, getX(), getY(), getWidth(), getHeight());
			}
		}

		style.font.draw(batch, layout, fontX, getY() + PADDING + layout.height);
	}

	public void resize(int width, int height) {	
		maxRectangleWidth = Math.min(width - DPIUtils.getMarginSize() * 2, style.font.getSpaceWidth() * 80);
		maxTalkWidth = Math.min(width - DPIUtils.getMarginSize() * 2, style.font.getSpaceWidth() * 35);
	}

	/** The style for the TextManagerUI */
	static public class TextManagerUIStyle {
		/** Optional. */
		public Drawable rectBackground;
		public Drawable talkBackground;
		public Drawable talkBubble;
		public BitmapFont font;

		public TextManagerUIStyle() {
		}

		public TextManagerUIStyle(TextManagerUIStyle style) {
			rectBackground = style.rectBackground;
			talkBackground = style.talkBackground;
			talkBubble = style.talkBubble;
			font = style.font;
		}
	}
}
