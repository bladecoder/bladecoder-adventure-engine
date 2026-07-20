package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription(value = "Sets the actor description")
public class SetDescAction implements Action {
	@ActionProperty(type = Type.SCENE_INTERACTIVE_ACTOR, required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("The actor 'desc'")
	private String text;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		final Scene s = actor.getScene(w);

		String actorId = actor.getActorId();
		if (actorId == null) {
			EngineLogger.error("SetDesc - Actor not set.");
			return false;
		}

		InteractiveActor a = (InteractiveActor) s.getActor(actorId, true);

		if (a != null)
			a.setDesc(text);
		else
			EngineLogger.error("SetDesc - Actor not found: " + actorId);

		return false;
	}

}
