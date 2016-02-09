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
public class SayAction extends BaseCallbackAction {
	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.ACTOR, required=true)
	private String actor;

	@ActionPropertyDescription("The 'text' to show")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionPropertyDescription("The 'soundId' to play if selected")
	@ActionProperty(type = Type.SOUND)
	private String soundId;

	@ActionProperty(required = true, defaultValue = "SUBTITLE")
	@ActionPropertyDescription("The type of the text.")
	private Text.Type type = Text.Type.SUBTITLE;

	@ActionProperty(defaultValue = "false")
	@ActionPropertyDescription("Queue the text if other text is showing, or show it immediately.")
	private boolean queue = false;

	@Override
	public boolean run(VerbRunner cb) {
		float x, y;
		Color color = null;

		setVerbCb(cb);
		InteractiveActor a = (InteractiveActor)World.getInstance().getCurrentScene().getActor(actor, false);
		
		boolean textWait = getWait();

		if (soundId != null)
			a.playSound(soundId);

		if (text != null) {
			if (type != Text.Type.TALK) {
				x = y = TextManager.POS_SUBTITLE;
			} else {
				
				Rectangle boundingRectangle = a.getBBox().getBoundingRectangle();
				
				x = boundingRectangle.getX() + boundingRectangle.getWidth() / 2;
				y = boundingRectangle.getY() + boundingRectangle.getHeight();
				
				CharacterActor ca = (CharacterActor)a;
				
				color = ca.getTextColor();
				
				ca.talk();
				
				// always wait to restore the 'stand' animation when TALK.
				textWait = true;
			}

			World.getInstance().getTextManager().addText(text, x, y, queue, type, color, null,
					textWait?this:null);
		}

		return getWait();
	}

	@Override
	public void resume() {
		if (type == Text.Type.TALK) {
			CharacterActor a = (CharacterActor) World.getInstance().getCurrentScene().getActor(actor, false);
			a.stand();
		}

		// after restore the 'stand' pose, only continue if the verb is waiting.
		if(getWait())
			super.resume();
	}
}
