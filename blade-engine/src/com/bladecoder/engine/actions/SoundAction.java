package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@Deprecated
@ActionDescription("Play/Stop a sound. Deprecated: Use PlaySound action instead.")
public class SoundAction implements Action {
	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.INTERACTIVE_ACTOR, required = true)
	private String actor;

	@ActionPropertyDescription("The actor 'soundId' to play. If empty the current sound will be stopped.")
	@ActionProperty(type = Type.STRING)
	private String play;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		if ("$PLAYER".equals(actor))
			actor = w.getCurrentScene().getPlayer().getId();

		if (play != null) {
			w.getCurrentScene().getSoundManager().playSound(actor + "_" + play);
		} else {
			w.getCurrentScene().getSoundManager().stopCurrentSound(actor);
		}

		return false;
	}

}
