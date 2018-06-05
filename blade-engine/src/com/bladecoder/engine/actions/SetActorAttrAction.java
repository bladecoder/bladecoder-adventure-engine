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
import com.badlogic.gdx.utils.Disposable;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
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

	@ActionProperty
	@ActionPropertyDescription("Sets the actor rotation")
	private Float rotation;

	@ActionPropertyDescription("The tint to draw the actor (RRGGBBAA).")
	@ActionProperty(type = Type.COLOR)
	private Color tint;

	@ActionProperty
	@ActionPropertyDescription("Sets the actor as an UI Actor. UI actors persists between scenes and are not affected by the scroll.")
	private Boolean uiActor;

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
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = actor.getScene(w);

		BaseActor a = s.getActor(actor.getActorId(), true);

		if (a == null) {
			EngineLogger.error("SetActorAttr - Actor not found:" + this.actor.getActorId());

			return false;
		}

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

		if (rotation != null) {
			if (a instanceof SpriteActor)
				((SpriteActor) a).setRot(rotation);
			else
				EngineLogger.error("'rotation' property not supported for actor:" + a.getId());
		}

		if (tint != null) {
			if (a instanceof SpriteActor)
				((SpriteActor) a).setTint(tint);
			else
				EngineLogger.error("'tint' property not supported for actor:" + a.getId());
		}

		if (fakeDepth != null) {
			if (a instanceof SpriteActor) {
				((SpriteActor) a).setFakeDepth(fakeDepth);
			} else
				EngineLogger.error("'fakeDepth' property not supported for actor:" + a.getId());
		}

		if (standAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setStandAnim(standAnimation);
			else
				EngineLogger.error("'standAnimation' property not supported for actor:" + a.getId());
		}

		if (walkAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setWalkAnim(walkAnimation);
			else
				EngineLogger.error("'walkAnimation' property not supported for actor:" + a.getId());
		}

		if (talkAnimation != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setTalkAnim(talkAnimation);
			else
				EngineLogger.error("'talkAnimation' property not supported for actor:" + a.getId());
		}

		if (walkingSpeed != null) {
			if (a instanceof CharacterActor)
				((CharacterActor) a).setWalkingSpeed(walkingSpeed);
			else
				EngineLogger.error("'walkingSpeed' property not supported for actor:" + a.getId());
		}

		if (uiActor != null) {
			if (a instanceof InteractiveActor) {
				if (uiActor)
					setUIActor(s, (InteractiveActor) a);
				else
					removeUIActor(s, (InteractiveActor) a);
			} else
				EngineLogger.error("'uiActor' property not supported for actor:" + a.getId());
		}

		return false;
	}

	private void setUIActor(Scene scn, InteractiveActor actor) {

		scn.removeActor(actor);

		if (scn != w.getCurrentScene() && w.getCachedScene(scn.getId()) == null
				&& actor instanceof AssetConsumer) {
			((AssetConsumer) actor).loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			((AssetConsumer) actor).retrieveAssets();
		}

		w.getUIActors().addActor(actor);
	}

	private void removeUIActor(Scene scn, InteractiveActor actor) {
		InteractiveActor a = w.getUIActors().removeActor(actor.getId());

		if (a != null) {
			if (scn != w.getCurrentScene() && a instanceof Disposable)
				((Disposable) a).dispose();

			scn.addActor(a);
		} else {
			EngineLogger.debug("UIActor not found: " + actor.getId());
		}
	}

}
