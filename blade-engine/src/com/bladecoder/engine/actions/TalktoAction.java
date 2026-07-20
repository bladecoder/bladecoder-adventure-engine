package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets the dialog mode")
public class TalktoAction implements Action {
	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.CHARACTER_ACTOR, required = true)
	private String actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The 'dialogId' to show")
	private String dialog;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		CharacterActor a = (CharacterActor)w.getCurrentScene().getActor(actor, false);
		
		w.setCurrentDialog(a.getDialog(dialog));
		
		return false;
	}


}
