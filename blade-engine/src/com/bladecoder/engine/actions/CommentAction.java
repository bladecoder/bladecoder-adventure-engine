package com.bladecoder.engine.actions;

import com.badlogic.gdx.graphics.Color;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription(name = "Comment", value = "Comment action for documentation porposes. Does nothing.")
public class CommentAction implements Action {
	@ActionProperty(required = false, type = Type.SMALL_TEXT)
	@ActionPropertyDescription("The comment.")
	private String comment;

	@ActionProperty(required = true, defaultValue = "false")
	@ActionPropertyDescription("When true, the comment will be showed on screen while testing.")
	private boolean debug = false;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		if (EngineLogger.debugMode() && comment != null) {
			EngineLogger.debug(comment);

			if (debug)
				w.getCurrentScene().getTextManager().addText(comment, TextManager.POS_SUBTITLE,
						TextManager.POS_SUBTITLE, false, Text.Type.UI, Color.YELLOW, null, null, null, null, null);
		}

		return false;
	}

}
