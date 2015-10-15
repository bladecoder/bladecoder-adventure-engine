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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Draw a text in the screen")
public class TextAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("The 'text' to show")
	@ActionPropertyType(Type.SMALL_TEXT)
	private String text;

	@JsonProperty(required = true, defaultValue = "default")
	@JsonPropertyDescription("The style to use (an entry in your `ui.json` in the `com.bladecoder.engine.ui.TextManagerUI$TextManagerUIStyle` section)")
	@ActionPropertyType(Type.TEXT_STYLE)
	private String style;

	@JsonProperty
	@JsonPropertyDescription("The color to use for the font ('white', 'black' or RRGGBBAA). If not set, the default color defined in the style is used.")
	@ActionPropertyType(Type.COLOR)
	private Color color;

	@JsonProperty
	@JsonPropertyDescription("The position of the text. -1 for center")
	@ActionPropertyType(Type.VECTOR2)
	private Vector2 pos;

	@JsonProperty(required = true, defaultValue = "RECTANGLE")
	@JsonPropertyDescription("The type of the text.")
	@ActionPropertyType(Type.STRING)
	private Text.Type type = Text.Type.PLAIN;

	@JsonProperty(defaultValue = "false")
	@JsonPropertyDescription("Queue the text if other text is showing, or show it immediately.")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean queue = false;
	
	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
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
