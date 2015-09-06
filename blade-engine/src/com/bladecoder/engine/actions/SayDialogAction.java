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

import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.World;

@ActionDescription("Says the selected option from the current dialog. This action does the next steps:\n" +
"\n- Sets the player 'talk' animation and say the player text" +
"\n- Restore the previous player animation and set the target actor 'talk' animation and say the response text" +
"\n- Restore the target actor animation")
public class SayDialogAction extends BaseCallbackAction {
	private boolean characterTurn = false;
	private String characterName;
	private String responseText;

	private String previousAnim;

	@Override
	public void setParams(HashMap<String, String> params) {
	
	}

	@Override
	public boolean run(ActionCallback cb) {
		setVerbCb(cb);
		World w = World.getInstance();
		DialogOption o = World.getInstance().getCurrentDialog().getCurrentOption();
		String playerText = o.getText();
		
		responseText = o.getResponseText();
		characterName = w.getCurrentDialog().getActor();
		
		characterTurn = true;
		previousAnim = null;
		
		// If the player or the character is talking restore to 'stand' pose
		restoreStandPose(w.getCurrentScene().getPlayer());
		
		if(w.getCurrentScene().getActor(characterName, false) instanceof SpriteActor)
			restoreStandPose((CharacterActor)w.getCurrentScene().getActor(characterName, false));

		if (playerText != null) {
			CharacterActor player = World.getInstance().getCurrentScene().getPlayer();

//			WorldCamera c = World.getInstance().getCamera();
//			Vector3 p = c.scene2screen(pos.x, pos.y + player.getHeight());

			World.getInstance().getTextManager()
					.addText(playerText, player.getX(), player.getY() + player.getHeight(), false, Text.Type.TALK, player.getTextColor(), null, this);
 
			startTalkAnim(player);

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
			
			if(previousAnim!= null){
				SpriteActor player = World.getInstance().getCurrentScene().getPlayer();
				player.startAnimation(previousAnim, null);
			}

			if (responseText != null) {

//				WorldCamera c = World.getInstance().getCamera();
//				Vector3 p = c.scene2screen(pos.x, pos.y + actor.getHeight());

				World.getInstance()
						.getTextManager()
						.addText(responseText, actor.getX(), actor.getY() + actor.getBBox().getBoundingRectangle().getHeight() , false, Text.Type.TALK,
								((CharacterActor)actor).getTextColor(), null, this);

				if(actor instanceof CharacterActor) {
					startTalkAnim((CharacterActor)actor);
				}
			} else {
				super.resume();
			}
		} else {
			if(actor instanceof SpriteActor) {
				((SpriteActor)actor).startAnimation(previousAnim, null);
			}
			super.resume();			
		}
	}
	
	private void restoreStandPose(CharacterActor a) {
		if(a == null) return;
		
		String fa = a.getRenderer().getCurrentAnimationId();
		
		// If the actor was already talking we restore the actor to the 'stand' pose	
		if(fa.startsWith(a.getTalkAnim())){ 		
			a.stand();
		}
	}
	
	private void startTalkAnim(CharacterActor a) {
		previousAnim = a.getRenderer().getCurrentAnimationId();
		
		a.talk();
	}

	@Override
	public void write(Json json) {
		json.writeValue("previousFA", previousAnim);
		json.writeValue("responseText", responseText);
		json.writeValue("characterTurn", characterTurn);
		json.writeValue("characterName", characterName);
		super.write(json);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		previousAnim = json.readValue("previousFA", String.class, jsonData);
		responseText = json.readValue("responseText", String.class, jsonData);
		characterTurn = json.readValue("characterTurn", Boolean.class, jsonData);
		characterName = json.readValue("characterName", String.class, jsonData);
		super.read(json, jsonData);
	}


}
