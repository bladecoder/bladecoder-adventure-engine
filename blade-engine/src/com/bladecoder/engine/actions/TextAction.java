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
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Draw a text in the screen")
public class TextAction implements Action {
	@ActionPropertyDescription("The 'text' to show")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionPropertyDescription("The 'voice' file to play if selected.")
	@ActionProperty(type = Type.VOICE)
	private String voiceId;

	@ActionPropertyDescription("The style to use (an entry in your `ui.json` in the `com.bladecoder.engine.ui.TextManagerUI$TextManagerUIStyle` section)")
	@ActionProperty(type = Type.TEXT_STYLE, required = true, defaultValue = "default")
	private String style;

	@ActionPropertyDescription("The color to use for the font (RRGGBBAA). If not set, the default color defined in the style is used.")
	@ActionProperty(type = Type.COLOR)
	private Color color;

	@ActionProperty
	@ActionPropertyDescription("Obtain the text position from this actor.")
	private SceneActorRef target;

	@ActionProperty
	@ActionPropertyDescription("The position of the text. -1 for center. Absolute if no target is selected. Relative if the target is selected.")
	private Vector2 pos;

	@ActionProperty(required = true, defaultValue = "SUBTITLE")
	@ActionPropertyDescription("The type of the text.")
	private Text.Type type = Text.Type.PLAIN;

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

		if (text == null)
			return false;

		float x = TextManager.POS_CENTER, y = TextManager.POS_CENTER;

		if (target != null) {
			Scene ts = target.getScene(w);
			BaseActor anchorActor = ts.getActor(target.getActorId(), true);

			x = anchorActor.getX();
			y = anchorActor.getY();

			if (anchorActor instanceof InteractiveActor) {
				Vector2 refPoint = ((InteractiveActor) anchorActor).getRefPoint();
				x += refPoint.x;
				y += refPoint.y;
			}

			if (pos != null) {
				float scale = EngineAssetManager.getInstance().getScale();

				x += pos.x * scale;
				y += pos.y * scale;
			}
		} else if (pos != null) {
			float scale = EngineAssetManager.getInstance().getScale();

			if (pos.x != TextManager.POS_CENTER)
				x = pos.x * scale;

			if (pos.y != TextManager.POS_CENTER)
				y = pos.y * scale;

		} else {

			if (type == Text.Type.SUBTITLE) {
				x = y = TextManager.POS_SUBTITLE;
			}
		}

		w.getCurrentScene().getTextManager().addText(text, x, y, queue, type, color, style, null, voiceId,
				wait ? cb : null);

		return wait;

	}
}
