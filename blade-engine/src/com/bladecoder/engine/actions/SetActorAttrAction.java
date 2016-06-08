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
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.SpriteActor.DepthType;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Change actor attributes.")
public class SetActorAttrAction implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The target actor")	
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor visibility")
	private Boolean visible;

	@ActionProperty
	@ActionPropertyDescription("When 'true' the actor responds to the user input")
	private Boolean interaction;

	@ActionPropertyDescription("The actor layer")
	@ActionProperty(type = Type.LAYER)
	private String layer;

	@ActionProperty
	@ActionPropertyDescription("The order to draw bigger is near")
	private Float zIndex;

	@ActionProperty
	@ActionPropertyDescription("Enable/Disable the Fake Depth for the actor")
	private Boolean fakeDepth;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor scale")
	private Float scale;
	
	@ActionPropertyDescription("The tint to draw the actor (RRGGBBAA).")
	@ActionProperty(type = Type.COLOR)
	private Color tint;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor 'stand' animation. Only supported for character actors.")
	private String standAnimation;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor 'walk' animation. Only supported for character actors.")
	private String walkAnimation;
	@ActionProperty
	@ActionPropertyDescription("Sets the actor 'talk' animation. Only supported for character actors.")
	private String talkAnimation;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor speed for walking. Only supported for character actors.")
	private Float walkingSpeed;

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = actor.getScene();

		BaseActor a = s.getActor(actor.getActorId(), true);

		if (visible != null)
			a.setVisible(visible);

		if (interaction != null) {
			if (a instanceof InteractiveActor)
				((InteractiveActor) a).setInteraction(interaction);
			else
				EngineLogger.error("'Interaction' property not supported for actor:" + a.getId());
		}

		if (layer != null) {
			if (a instanceof InteractiveActor) {
				InteractiveActor iActor = (InteractiveActor) a;
				
				String oldLayer = iActor.getLayer();

				s.getLayer(oldLayer).remove(iActor);

				iActor.setLayer(layer);

				SceneLayer l = s.getLayer(layer);
				l.add(iActor);

				if (!l.isDynamic())
					l.orderByZIndex();
			} else
				EngineLogger.error("'layer' property not supported for actor:" + a.getId());
		}

		if (zIndex != null) {
			if (a instanceof InteractiveActor) {
				InteractiveActor iActor = (InteractiveActor) a;
				
				iActor.setZIndex(zIndex);
				SceneLayer l = s.getLayer(iActor.getLayer());

				if (!l.isDynamic())
					l.orderByZIndex();
			} else
				EngineLogger.error("'zIndex' property not supported for actor:" + a.getId());
		}

		if (scale != null) {
			if (a instanceof SpriteActor)
				((SpriteActor) a).setScale(scale);
			else
				EngineLogger.error("'scale' property not supported for actor:" + a.getId());
		}
		
		if (tint != null) {
			if (a instanceof SpriteActor)
				((SpriteActor) a).setTint(tint);
			else
				EngineLogger.error("'tint' property not supported for actor:" + a.getId());
		}

		if (fakeDepth != null) {
			if (a instanceof SpriteActor) {
				if (fakeDepth)
					((SpriteActor) a).setDepthType(DepthType.VECTOR);
				else
					((SpriteActor) a).setDepthType(DepthType.NONE);
			} else
				EngineLogger.error("fakeDepth property not supported for actor:" + a.getId());
		}

		if (standAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setStandAnim(standAnimation);
			else
				EngineLogger.error("standAnimation property not supported for actor:" + a.getId());
		}

		if (walkAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setWalkAnim(walkAnimation);
			else
				EngineLogger.error("walkAnimation property not supported for actor:" + a.getId());
		}

		if (talkAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setTalkAnim(talkAnimation);
			else
				EngineLogger.error("talkAnimation property not supported for actor:" + a.getId());
		}

		if (walkingSpeed != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setWalkingSpeed(walkingSpeed);
			else
				EngineLogger.error("walkingSpeed property not supported for actor:" + a.getId());
		}

		return false;
	}

}
