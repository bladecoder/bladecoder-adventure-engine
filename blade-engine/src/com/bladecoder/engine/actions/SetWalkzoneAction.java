package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription(name = "SetWalkzone", value = "Set the scene walkzone.")
public class SetWalkzoneAction implements Action {
	@ActionProperty(type = Type.SCENE_WALKZONE_ACTOR, required = false)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		if (actor == null) {
			w.getCurrentScene().setWalkZone(null);
		} else {
			Scene s = actor.getScene(w);

			s.setWalkZone(actor.getActorId());
			
			// We must recalc the walkzone when the target scene is the current scene or when
			// the scene is cached.
			if(s == w.getCurrentScene() || 
					w.getCachedScene(s.getId()) != null) {
				s.calcWalkzone();
			}
		}

		return false;
	}

}
