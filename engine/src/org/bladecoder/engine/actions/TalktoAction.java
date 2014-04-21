package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;

public class TalktoAction implements Action {
	public static final String INFO = "Sets the dialog mode";
	public static final Param[] PARAMS = {
		new Param("dialog", "The 'dialogId' to show", Type.STRING, true)
		};		
	
	String actorId;
	String dialog;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		dialog = params.get("dialog");
	}

	@Override
	public void run() {
		
		SpriteActor actor = (SpriteActor)World.getInstance().getCurrentScene().getActor(actorId);
		
		World.getInstance().setCurrentDialog(actor.getDialog(dialog));
	}


	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}
