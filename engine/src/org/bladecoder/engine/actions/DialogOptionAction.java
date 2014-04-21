package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.Dialog;
import org.bladecoder.engine.model.DialogOption;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

public class DialogOptionAction implements Action {
	public static final String INFO = "Change the selected dialog option properties";
	public static final Param[] PARAMS = {
		new Param("dialog", "The dialog", Type.STRING, true),	
		new Param("option", "The option", Type.STRING, true),
		new Param("visible", "Shows/Hide the dialog option", Type.BOOLEAN),
		new Param("set_current", "Sets the selected option as the current dialog option", Type.BOOLEAN)		
		};	
	
	
	String actorId;
	String dialog;
	String option;
	boolean setVisibility;
	boolean setCurrent = false;
	boolean visibility;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		dialog = params.get("dialog");
		option = params.get("option");

		if (params.get("visible") != null) {
			setVisibility = true;
			visibility = Boolean.parseBoolean(params.get("visible"));
		}

		if (params.get("set_current") != null) {
			setCurrent = Boolean.parseBoolean(params.get("set_current"));
		}
	}

	@Override
	public void run() {

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene()
				.getActor(actorId);
		Dialog d = actor.getDialog(dialog);

		if (d == null) {
			EngineLogger.error("DialogOptionAction: Dialog '" + dialog + "' not found");
			return;
		}

		DialogOption o = null;
		
		if (option != null) {
			o = d.findSerOption(option);

			if (o == null) {
				EngineLogger.error("DialogOptionAction: Option '" + option + "' not found");
				return;
			}
		}

		if (setVisibility && o != null)
			o.setVisible(visibility);

		if (setCurrent) {
			World.getInstance().setCurrentDialog(actor.getDialog(dialog));
			d.setCurrentOption(o);
		}
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
