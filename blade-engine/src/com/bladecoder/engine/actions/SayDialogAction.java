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

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Says the selected option from the current dialog. This action does the next steps:\n"
		+ "\n- Sets the player 'talk' animation and say the player text"
		+ "\n- Puts the player 'stand' animation and sets the target actor 'talk' animation and says the response text"
		+ "\n- Puts the target 'stand' animation")
public class SayDialogAction extends BaseCallbackAction {
	private boolean characterTurn = false;
	private String characterName;
	private String responseText;

	@Override
	public boolean run(VerbRunner cb) {
		World w = World.getInstance();

		if (w.getCurrentDialog() == null || w.getCurrentDialog().getCurrentOption() == null) {
			EngineLogger.debug("SayDialogAction WARNING: Current dialog doesn't found.");

			return false;
		}

		setVerbCb(cb);
		DialogOption o = w.getCurrentDialog().getCurrentOption();
		String playerText = o.getText();

		responseText = o.getResponseText();
		characterName = w.getCurrentDialog().getActor();

		characterTurn = true;

		if (playerText != null) {
			CharacterActor player = w.getCurrentScene().getPlayer();

			w.getTextManager().addText(playerText, player.getX(), player.getY() + player.getHeight(), false,
					Text.Type.TALK, player.getTextColor(), null, this);

			player.talk();

		} else {
			resume();
		}

		return getWait();
	}

	@Override
	public void resume() {

		World w = World.getInstance();
		BaseActor actor = w.getCurrentScene().getActor(characterName, false);

		if (characterTurn) {
			characterTurn = false;

			CharacterActor player = World.getInstance().getCurrentScene().getPlayer();
			player.stand();

			if (responseText != null) {
				World.getInstance().getTextManager().addText(responseText, actor.getX(),
						actor.getY() + actor.getBBox().getBoundingRectangle().getHeight(), false, Text.Type.TALK,
						((CharacterActor) actor).getTextColor(), null, this);

				if (actor instanceof CharacterActor) {
					((CharacterActor) actor).talk();
				}
			} else {
				super.resume();
			}
		} else {
			if (actor instanceof CharacterActor) {
				((CharacterActor) actor).stand();
			}
			
			super.resume();
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("responseText", responseText);
		json.writeValue("characterTurn", characterTurn);
		json.writeValue("characterName", characterName);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		responseText = json.readValue("responseText", String.class, jsonData);
		characterTurn = json.readValue("characterTurn", boolean.class, false, jsonData);
		characterName = json.readValue("characterName", String.class, jsonData);
		super.read(json, jsonData);
	}

}
