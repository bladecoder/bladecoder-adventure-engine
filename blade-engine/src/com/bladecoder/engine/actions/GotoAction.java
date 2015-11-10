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

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Walks to the selected position")
public class GotoAction implements Action {
	public enum Align {
		CENTER, LEFT, RIGHT
	}

	@ActionPropertyDescription("The walking actor")
	@ActionProperty(type = Type.ACTOR, required=true)
	private String actor;

	@ActionProperty
	@ActionPropertyDescription("The position to walk to")
	private Vector2 pos;

	@ActionPropertyDescription("Walks to the target actor position")
	@ActionProperty(type = Type.ACTOR)
	private String target;

	@ActionProperty(defaultValue = "CENTER")
	@ActionPropertyDescription("When selecting a target actor, an align can be selected")
	private Align align = Align.CENTER;

	@ActionProperty
	@ActionPropertyDescription("When selecting a target actor, the relative distance to the anchor in each axis")
	private Vector2 distance;

	@ActionProperty(required = true, defaultValue = "true")
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	@Override
	public boolean run(VerbRunner cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		CharacterActor actor = (CharacterActor) World.getInstance().getCurrentScene().getActor(this.actor, false);

		if (target != null) {
			BaseActor target = World.getInstance().getCurrentScene().getActor(this.target, false);
			float x = target.getX();
			float y = target.getY();
			float targetBBoxWidth2 = 0;
			final float actorBBoxWidth2 = actor.getBBox().getBoundingRectangle().width / 2;

			if (!(target instanceof AnchorActor)) {
				targetBBoxWidth2 = target.getBBox().getBoundingRectangle().width / 2;
			}

			switch (align) {
			case LEFT:
				x = x - targetBBoxWidth2 - actorBBoxWidth2;
				break;
			case RIGHT:
				x = x + targetBBoxWidth2 + actorBBoxWidth2;
				break;
			case CENTER:
				if (!(target instanceof SpriteActor))
					x = x + targetBBoxWidth2;
				break;
			}

			if (distance != null) {
				x += distance.x;
				y += distance.y;
			}

			actor.goTo(new Vector2(x, y), wait ? cb : null);
		} else
			actor.goTo(new Vector2(pos.x * scale, pos.y * scale), wait ? cb : null);

		return wait;
	}

	/**
	 * If 'player' is far from 'actor', we bring it close. If 'player' is closed
	 * from 'actor' do nothing.
	 * 
	 * TODO: DOESN'T WORK NOW
	 * 
	 * @param player
	 * @param actor
	 */
	@SuppressWarnings("unused")
	private void goNear(CharacterActor player, BaseActor actor, ActionCallback cb) {
		Rectangle rdest = actor.getBBox().getBoundingRectangle();

		// Vector2 p0 = new Vector2(player.getSprite().getX(),
		// player.getSprite().getY());
		Vector2 p0 = new Vector2(player.getX(), player.getY());

		// calculamos el punto más cercano al objeto
		Vector2 p1 = new Vector2(rdest.x, rdest.y); // izquierda
		Vector2 p2 = new Vector2(rdest.x + rdest.width, rdest.y); // derecha
		Vector2 p3 = new Vector2(rdest.x + rdest.width / 2, rdest.y); // centro
		float d1 = p0.dst(p1);
		float d2 = p0.dst(p2);
		float d3 = p0.dst(p3);
		Vector2 pf;

		if (d1 < d2 && d1 < d3) {
			pf = p1;
		} else if (d2 < d1 && d2 < d3) {
			pf = p2;
		} else {
			pf = p3;
		}

		player.goTo(pf, cb);
	}

}
