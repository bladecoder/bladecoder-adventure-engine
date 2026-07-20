package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("ONLY FOR SPINE ACTORS: Sets a secondary animation")
public class SpineSecondaryAnimationAction implements Action {

	@ActionProperty(required = false)
	@ActionPropertyDescription("The Animation to set. Empty to clear the secondary animation")
	private ActorAnimationRef animation;

	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}
	
	@Override
	public boolean run(VerbRunner cb) {
		
		String actorId = animation.getActorId();
		
		SpriteActor a = (SpriteActor) w.getCurrentScene().getActor(actorId, true);
		
		if(a.getRenderer() instanceof SpineRenderer) {
			SpineRenderer r = (SpineRenderer) a.getRenderer();
			String anim = animation.getAnimationId();
			
			if(anim.isEmpty())
				anim = null;
			
			r.setSecondaryAnimation(anim);
		} else {
			EngineLogger.error("SpineSecondaryAnimation: The actor renderer has to be of Spine type.");
		}

		return false;
	}

}
