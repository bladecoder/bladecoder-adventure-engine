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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.RectangleRenderer;

public class PieMenu2 extends com.badlogic.gdx.scenes.scene2d.Group {
	
	private final static int NUM_VERBS = 10;

	private BitmapFont font;

	private Button[] buttons;
	private Vector2[] endPositions;

	private float x = 0, y = 0;
	
	private float distance = 100;
    private float minAngle = 0, maxAngle = 360, startAngle = 0;

	private InteractiveActor iActor = null;

	private final SceneScreen sceneScreen;
	
	private int viewportWidth, viewportHeight;
	
	private final GlyphLayout layout = new GlyphLayout();
	
	private String desc = null;

	public PieMenu2(SceneScreen scr) {
		sceneScreen = scr;
		font = scr.getUI().getSkin().getFont("desc");
		buttons = new Button[NUM_VERBS];
		endPositions = new Vector2[NUM_VERBS];
		
		for(int i = 0; i < NUM_VERBS; i++) {
			buttons[i] =  new Button(scr.getUI().getSkin(), "pie_lookat");
			endPositions[i] = new Vector2();
			addActor(buttons[i]);
			
			buttons[i].addListener(new ChangeListener() {			
				@Override
				public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
					if (iActor != null) {
						sceneScreen.runVerb(iActor, "lookat", null);
					}

					hide();
				}
			});
		}
	}

	@Override
	public void draw(Batch batch, float alpha) {

		super.draw(batch, alpha);

		// DRAW TARGET DESCRIPTION
		String desc = iActor.getDesc();

		if (desc != null) {			
			float margin = DPIUtils.UI_SPACE;

			float textX = x - layout.width / 2;
			float textY = y - layout.height - DPIUtils.UI_SPACE;

			if (textX < 0)
				textX = 0;

			RectangleRenderer.draw(batch, textX - margin, textY - layout.height - margin,
					layout.width + margin*2, layout.height + margin*2, Color.BLACK);
			font.draw(batch, layout, textX, textY);
		}
	}

	public void hide() {
		setVisible(false);
		iActor = null;
	}

	public void show(InteractiveActor a, float x, float y) {
		setVisible(true);
		this.x = x;
		this.y = y;
		iActor = a;
		
		// DRAW TARGET DESCRIPTION
		desc = iActor.getDesc();

		if (desc != null) {

			if (desc.charAt(0) == I18N.PREFIX)
				desc = I18N.getString(desc.substring(1));
					
			layout.setText(font, desc);
		}
		
		float margin = DPIUtils.getMarginSize();
		
		// FITS TO SCREEN
		if(x < distance + buttons[0].getWidth() / 2 + margin)
			this.x = distance + buttons[0].getWidth() / 2 + margin;
		else if(x > viewportWidth - distance - buttons[0].getWidth() / 2 - margin)
			this.x = viewportWidth - distance - buttons[0].getWidth() / 2 - margin;
		
		if(y < distance + buttons[0].getHeight() / 2 + margin)
			this.y = distance + buttons[0].getHeight() / 2 + margin;
		else if(y > viewportHeight - distance - buttons[0].getHeight() / 2 - margin)
			this.y = viewportHeight - distance - buttons[0].getHeight() / 2 - margin;
		
		float offsetAngle = ((maxAngle - minAngle)) / (NUM_VERBS - 1);    
	    float angle = startAngle;
	        
		for(int i = 0; i < NUM_VERBS; i++) {
			
			endPositions[i].x = (float)(Math.cos(Math.toRadians(angle))) * distance + this.x;
			endPositions[i].y = (float)(Math.sin(Math.toRadians(angle))) * distance + this.y;
			
			buttons[i].setPosition(this.x - buttons[i].getWidth() / 2, this.y - buttons[i].getHeight() / 2);
			buttons[i].addAction(Actions
					.moveTo(endPositions[i].x - buttons[i].getWidth() / 2, endPositions[i].y - buttons[i].getWidth() / 2, .1f));
			
			angle += offsetAngle;
		}

	}

	public void resize(int width, int height) {
		viewportWidth = width;
		viewportHeight = height;
		
		setBounds(0, 0, width, height);
	}
	
	/**
	 * The style for the PieMenu2.
	 * 
	 * @author Rafael Garcia
	 */
	static public class PieMenu2Style {
		public ImageButtonStyle buttonStyle;
		public float distance;
		public float minAngle, maxAngle, startAngle;

		public PieMenu2Style() {
		}

		public PieMenu2Style(PieMenu2Style style) {
			buttonStyle = style.buttonStyle;
			distance = style.distance;
			minAngle = style.minAngle;
			maxAngle = style.maxAngle;
			startAngle = style.startAngle;
		}
	}
}
