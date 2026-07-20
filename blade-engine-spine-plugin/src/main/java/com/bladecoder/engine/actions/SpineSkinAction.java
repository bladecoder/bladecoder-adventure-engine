package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("ONLY FOR SPINE ACTORS: Sets a Skin.")
public class SpineSkinAction implements Action {

	@ActionPropertyDescription("The target actor")
	@ActionProperty(required = true)
	private SceneActorRef actor;

	@ActionProperty(required = false)
	@ActionPropertyDescription("The Skin. Empty to clear the skin.")
	private String skin;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		SpriteActor a = (SpriteActor) actor.getActor(w);
		
		if(a instanceof SpriteActor && a.getRenderer() instanceof SpineRenderer) {
			SpineRenderer r = (SpineRenderer) a.getRenderer();
			
			r.setSkin(skin);
		} else {
			EngineLogger.error("SpineSecondaryAnimation: The actor renderer has to be of Spine type.");
		}

		return false;
	}

}
