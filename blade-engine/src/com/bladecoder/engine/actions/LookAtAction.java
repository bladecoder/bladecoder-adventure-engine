package com.bladecoder.engine.actions;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Shows the text and puts the player looking at the selected actor direction")
public class LookAtAction implements Action {
	public enum Direction {
		FRONT(AnimationRenderer.FRONT), BACK(AnimationRenderer.BACK), LEFT(AnimationRenderer.LEFT),
		RIGHT(AnimationRenderer.RIGHT), FRONTLEFT(AnimationRenderer.FRONTLEFT),
		FRONTRIGHT(AnimationRenderer.FRONTRIGHT), BACKLEFT(AnimationRenderer.BACKLEFT),
		BACKRIGHT(AnimationRenderer.BACKRIGHT);

		private final String direction;

		Direction(String direction) {
			this.direction = direction;
		}

		public String getDirection() {
			return direction;
		}
	}

	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.ACTOR)
	private String actor;

	@ActionPropertyDescription("The 'text' to show")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionPropertyDescription("The 'voice' file to play if selected.")
	@ActionProperty(type = Type.VOICE)
	private String voiceId;

	@ActionProperty
	@ActionPropertyDescription("The direction to lookat. If empty, the player lookat to the actor")
	private Direction direction;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is shown and the action continues inmediatly")
	private boolean wait = true;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		// EngineLogger.debug("LOOKAT ACTION");
		InteractiveActor a = (InteractiveActor) w.getCurrentScene().getActor(actor, true);

		if (w.getInventory().get(actor) == null) {
			CharacterActor player = w.getCurrentScene().getPlayer();

			if (direction != null && player != null)
				player.lookat(direction.getDirection());
			else if (a != null && player != null) {
				Rectangle bbox = a.getBBox().getBoundingRectangle();
				player.lookat(new Vector2(bbox.width / 2 + bbox.x, bbox.y));
			}
		}

		if (text != null) {
			String actorId = w.getCurrentScene().getPlayer() != null ? w.getCurrentScene().getPlayer().getId() : null;

			w.getCurrentScene().getTextManager().addText(text, TextManager.POS_SUBTITLE, TextManager.POS_SUBTITLE,
					false, Text.Type.SUBTITLE, null, null, actorId, voiceId, null, wait ? cb : null);

			return wait;
		}

		return false;
	}

}
