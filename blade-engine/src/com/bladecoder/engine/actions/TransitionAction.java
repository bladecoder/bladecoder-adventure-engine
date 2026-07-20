package com.bladecoder.engine.actions;

import com.badlogic.gdx.graphics.Color;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets a transition effect (FADEIN/FADEOUT)")
public class TransitionAction implements Action {
	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("Duration of the transition")
	private float time = 1;

	@ActionPropertyDescription("The color to fade ('white', 'black' or RRGGBBAA).")
	@ActionProperty(type = Type.COLOR, required = true, defaultValue = "black")
	private Color color = new Color(0,0,0,1);

	@ActionProperty(required = true, defaultValue = "FADE_IN")
	@ActionPropertyDescription("The transition type (fadein/fadeout)")
	private Transition.Type type = Transition.Type.FADE_IN;     // FIXME: This adds NONE as a valid value

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the transition is showed and the action continues inmediatly")
	private boolean wait = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Transition t = w.getTransition();
		t.create(time, color, type, wait?cb:null);
		
		return wait;
	}

}
