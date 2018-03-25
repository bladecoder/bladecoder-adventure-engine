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

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Bot for testing a game. The bot will execute random actions in the scene.
 * 
 * @author rgarcia
 */
public class TesterBot {
	/** time between actions in secons */
	private float maxWaitInverval = 1f;
	private float waitInverval = 0f;
	private float deltaTime = 0f;
	
	/** Min. time to stay in scene before leave */
	private float inSceneTime = 20;
	private float inSceneTimeDelta = 0f;

	private Vector2 gotoVector = new Vector2();
	
	private boolean enabled = false;
	private boolean runLeaveVerbs = true;
	private boolean runGoto = false;
	private boolean passTexts = true;
	private boolean waitWhenWalking = true;
	
	private final ArrayList<String> excludeList = new ArrayList<String>();
	
	private final boolean inventoryAction;

	public TesterBot() {
		inventoryAction = !Config.getProperty(Config.SINGLE_ACTION_INVENTORY, false);
	}

	public void update(float d) {
		
		if(!enabled)
			return;
		
		World w = World.getInstance();

		deltaTime += d;
		inSceneTimeDelta += d;

		if(w.inCutMode() && isPassTexts())
			w.getTextManager().next();
		
		if (deltaTime > waitInverval && !w.inCutMode()) {
			deltaTime = 0;
			waitInverval = MathUtils.random(maxWaitInverval);
			
			boolean isWalking = false;
			SpriteActor player = w.getCurrentScene().getPlayer();
			if(player != null) {
				if(((AnimationRenderer)player.getRenderer()).getCurrentAnimationId().startsWith(CharacterActor.DEFAULT_WALK_ANIM))
					isWalking = true;
			}
			
			if(isWaitWhenWalking() && isWalking)
				return;
			
			Scene s = w.getCurrentScene();

			if (w.getCurrentDialog() == null) {

				// Select actor or goto
				boolean chooseActor = MathUtils.randomBoolean(.75f);

				if (!isRunGoto() || chooseActor) {

					// Select scene or actor inventory
					boolean chooseSceneActor = MathUtils.randomBoolean();

					if (chooseSceneActor && s.getActors().size() > 0) {
						// SCENE ACTOR
						int pos = MathUtils.random(s.getActors().size() - 1);
						BaseActor a = (BaseActor) (s.getActors().values().toArray()[pos]);
						
						if(!(a instanceof InteractiveActor))
							return;
						
						InteractiveActor scnActor = (InteractiveActor)a;

						if (excludeList.contains(scnActor.getId()) || !scnActor.isVisible() || 
								!scnActor.canInteract())
							return;
						
						String verb;

						if (scnActor.getVerb(Verb.LEAVE_VERB) != null) {
							verb = Verb.LEAVE_VERB;
						} else if (MathUtils.randomBoolean(0.33f)) {
							// LOOKAT
							verb = Verb.LOOKAT_VERB;
						} else {
							// ACTION			
							verb = scnActor.getVerb(Verb.TALKTO_VERB) != null ? Verb.TALKTO_VERB
									: Verb.ACTION_VERB;
						}
						
						if(!(verb.equals(Verb.LEAVE_VERB) && (!runLeaveVerbs || inSceneTime > inSceneTimeDelta))) {
							EngineLogger.debug("<TESTERBOT>: " + scnActor.getId() + "::" + verb);
							scnActor.runVerb(verb);
							
							if(verb.equals(Verb.LEAVE_VERB))
								inSceneTimeDelta = 0;
						}
					} else if (w.getInventory().getNumItems() > 0 && w.getInventory().isVisible()) {
						// INVENTORY ACTOR
						int pos = MathUtils.random(w.getInventory().getNumItems() - 1);
						SpriteActor invActor = w.getInventory().get(pos);
						
						if(excludeList.contains(invActor.getId()))
							return;

						// Select lookat, action or use
						int choosedVerb = MathUtils.random(3);

						if (choosedVerb == 0) {
							EngineLogger.debug("<TESTERBOT> INVENTORY: " + invActor.getId() + "::" + Verb.LOOKAT_VERB);
							invActor.runVerb(Verb.LOOKAT_VERB);
						} else if (choosedVerb == 1 && inventoryAction) {
							EngineLogger.debug("<TESTERBOT> INVENTORY: " + invActor.getId() + "::" + Verb.ACTION_VERB);
							invActor.runVerb(Verb.ACTION_VERB);
						} else { // 2 and 3

							InteractiveActor targetActor = null;

							if (w.getInventory().getNumItems() > 1 && MathUtils.randomBoolean(0.33f)) {
								// CHOOSE TARGET FROM INVENTORY
								int pos2 = MathUtils.random(w.getInventory().getNumItems() - 1);

								if (pos2 == pos)
									pos2 = (pos2 + 1) % w.getInventory().getNumItems();

								targetActor = w.getInventory().get(pos2);
								
								if(excludeList.contains(targetActor.getId()))
									return;
								
								EngineLogger.debug("<TESTERBOT> INVENTORY: " + invActor.getId() + "::" + Verb.USE_VERB + "::" + targetActor.getId());
								
								if(invActor.getVerb(Verb.USE_VERB, targetActor.getId()) != null)
									invActor.runVerb(Verb.USE_VERB, targetActor.getId());
								else
									targetActor.runVerb(Verb.USE_VERB, invActor.getId());
							} else {
								int pos2 = MathUtils.random(s.getActors().size() - 1);
								
								if(!(s.getActors().values().toArray()[pos2] instanceof InteractiveActor))
									return;
								
								targetActor = (InteractiveActor) (s.getActors().values().toArray()[pos2]);

								if (!excludeList.contains(targetActor.getId()) && targetActor.isVisible() && targetActor.canInteract()) {
									EngineLogger.debug("<TESTERBOT> INVENTORY: " + invActor.getId() + "::" + Verb.USE_VERB + "::" + targetActor.getId());
									
									if(invActor.getVerb(Verb.USE_VERB, targetActor.getId()) != null)
										invActor.runVerb(Verb.USE_VERB, targetActor.getId());
									else
										targetActor.runVerb(Verb.USE_VERB, invActor.getId());
								}
							}
						}
					}

				} else if (s.getPlayer() != null) {
					gotoVector.x = MathUtils.random() * w.getCurrentScene().getCamera().getScrollingWidth();
					gotoVector.y = MathUtils.random() * w.getCurrentScene().getCamera().getScrollingHeight();

					if (s.getPlayer().getVerb(Verb.GOTO_VERB) != null) {
						EngineLogger.debug("<TESTERBOT> GOTO: GOTO VERB");
						s.getPlayer().runVerb(Verb.GOTO_VERB);
					} else {
						EngineLogger.debug("<TESTERBOT> GOTO: " + gotoVector);
						s.getPlayer().goTo(gotoVector, null, false);
					}
				}
			} else {
				// DIALOG MODE
				List<String> visibleOptions = w.getDialogOptions();
						
				if(visibleOptions.size() > 0) {
					int pos = MathUtils.random(visibleOptions.size() - 1);
					EngineLogger.debug("<TESTERBOT> SELECT OPTION: " + pos);
					w.selectDialogOption(pos);
				}
			}
		} 
	}
	
	public void draw(SpriteBatch batch) {
		if (enabled) {
			// TODO draw bot icon
		}
	}	

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		
		if(enabled)
			EngineLogger.debug("<TESTERBOT> BOT ENABLED...");
		else
			EngineLogger.debug("<TESTERBOT> BOT DISABLED...");
	}

	public boolean isRunLeaveVerbs() {
		return runLeaveVerbs;
	}

	public void setRunLeaveVerbs(boolean runLeaveVerbs) {
		this.runLeaveVerbs = runLeaveVerbs;
	}

	public boolean isRunGoto() {
		return runGoto;
	}

	public void setRunGoto(boolean runGoto) {
		this.runGoto = runGoto;
	}

	public float getMaxWaitInverval() {
		return maxWaitInverval;
	}
	
	public void setMaxWaitInverval(float maxWaitInverval) {
		this.maxWaitInverval =  maxWaitInverval;
	}

	public float getInSceneTime() {
		return inSceneTime ;
	}

	public void setInSceneTime(float inSceneTime) {
		this.inSceneTime = inSceneTime;
	}

	public boolean isPassTexts() {
		return passTexts;
	}

	public void setPassTexts(boolean passTexts) {
		this.passTexts = passTexts;
	}

	public boolean isWaitWhenWalking() {
		return waitWhenWalking;
	}

	public void setWaitWhenWalking(boolean waitWhenWalking) {
		this.waitWhenWalking = waitWhenWalking;
	}
	
	public String getExcludeList() {
		StringBuilder s = new StringBuilder();
		
		for(int i = 0; i < excludeList.size(); i++) {
			s.append(excludeList.get(i));
			
			if(i < excludeList.size() - 1)
				s.append(',');
		}
		
		return s.toString();
	}
	
	public void setExcludeList(String l) {
		String[] split = l.split(",");
		
		excludeList.clear();
		
		for(int i = 0; i < split.length; i++)
			excludeList.add(split[i].trim());
	}
}
