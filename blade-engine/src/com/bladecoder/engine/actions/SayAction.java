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
package com.bladecoder.engine.actions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Says a text")
public class SayAction implements Action {
	@ActionPropertyDescription("The target actor.")
	@ActionProperty(type = Type.CHARACTER_ACTOR, required = true)
	private String actor;

	@ActionPropertyDescription("The 'text' to show.")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionPropertyDescription("The 'voice' file to play if selected.")
	@ActionProperty(type = Type.VOICE)
	private String voiceId;

	@ActionProperty(required = true, defaultValue = "SUBTITLE")
	@ActionPropertyDescription("The type of the text.")
	private Text.Type type = Text.Type.SUBTITLE;

	@ActionPropertyDescription("The animation to set when talking.")
	@ActionProperty(required = false)
	private String animation;

	@ActionPropertyDescription("The style to use (an entry in your `ui.json` in the `com.bladecoder.engine.ui.TextManagerUI$TextManagerUIStyle` section)")
	@ActionProperty(type = Type.TEXT_STYLE, required = false)
	private String style;

	@ActionProperty(defaultValue = "false")
	@ActionPropertyDescription("Queue the text if other text is showing, or show it immediately.")
	private boolean queue = false;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		float x = TextManager.POS_SUBTITLE, y = TextManager.POS_SUBTITLE;
		Color color = null;

		if (text == null)
			return false;

		InteractiveActor a = (InteractiveActor) w.getCurrentScene().getActor(actor, false);

		if (type == Text.Type.TALK && a != null) {
			Rectangle boundingRectangle = a.getBBox().getBoundingRectangle();

			x = boundingRectangle.getX() + boundingRectangle.getWidth() / 2;
			y = boundingRectangle.getY() + boundingRectangle.getHeight();

			color = ((CharacterActor) a).getTextColor();
		}

		w.getCurrentScene().getTextManager().addText(text, x, y, queue, type, color, style,
				a != null ? a.getId() : actor, voiceId, animation, wait?cb:null);

		return wait;

	}
}
