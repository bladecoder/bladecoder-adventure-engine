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
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;

@ActionDescription("Draw a text in the screen")
public class TextAction implements Action {
	@ActionPropertyDescription("The 'text' to show")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionPropertyDescription("The style to use (an entry in your `ui.json` in the `com.bladecoder.engine.ui.TextManagerUI$TextManagerUIStyle` section)")
	@ActionProperty(type = Type.TEXT_STYLE, required = true, defaultValue = "default")
	private String style;

	@ActionPropertyDescription("The color to use for the font ('white', 'black' or RRGGBBAA). If not set, the default color defined in the style is used.")
	@ActionProperty(type = Type.COLOR)
	private Color color;

	@ActionProperty
	@ActionPropertyDescription("The position of the text. -1 for center")

	private Vector2 pos;

	@ActionProperty(required = true, defaultValue = "RECTANGLE")
	@ActionPropertyDescription("The type of the text.")

	private Text.Type type = Text.Type.PLAIN;

	@ActionProperty(defaultValue = "false")
	@ActionPropertyDescription("Queue the text if other text is showing, or show it immediately.")

	private boolean queue = false;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")

	private boolean wait = true;

	@Override
	public boolean run(ActionCallback cb) {

		if (text != null) {
			float x =  TextManager.POS_CENTER, y =  TextManager.POS_CENTER;

			if (pos != null) {
				x = pos.x;
				y = pos.y;
			} else {

				if (type == Text.Type.RECTANGLE) {
					x = y = TextManager.POS_SUBTITLE;
				}
			}

			World.getInstance().getTextManager()
						.addText(text, x, y, queue, type, color, style, wait?cb:null);
		}
		
		return wait;
	}
}
