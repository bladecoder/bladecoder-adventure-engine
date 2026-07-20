package com.bladecoder.engine.actions;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Says the selected option from the current dialog. This action does the next steps:\n"
		+ "\n- Sets the player 'talk' animation and say the player text"
		+ "\n- Restore the previous player animation and set the target actor 'talk' animation and say the response text"
		+ "\n- Restore the target actor animation")
public class SayDialogAction implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		if (w.getCurrentDialog() == null || w.getCurrentDialog().getCurrentOption() == null) {
			EngineLogger.debug("SayDialogAction WARNING: Current dialog doesn't found.");

			return false;
		}

		DialogOption o = w.getCurrentDialog().getCurrentOption();
		String playerText = o.getText();
		String responseText = o.getResponseText();

		if (playerText != null) {
			CharacterActor player = w.getCurrentScene().getPlayer();

			Rectangle boundingRectangle = player.getBBox().getBoundingRectangle();
			float x = boundingRectangle.getX() + boundingRectangle.getWidth() / 2;
			float y = boundingRectangle.getY() + boundingRectangle.getHeight();

			Vector2 talkingTextPos = player.getTalkingTextPos();

			if (talkingTextPos != null) {
				x += talkingTextPos.x;
				y += talkingTextPos.y;
			}

			w.getCurrentScene().getTextManager().addText(playerText, x, y, false, Text.Type.TALK, player.getTextColor(),
					null, player.getId(), o.getVoiceId(), null, responseText == null && wait ? cb : null);
		}

		if (responseText != null) {
			CharacterActor actor = w.getCurrentDialog().getActor();

			String responseVoiceId = o.getResponseVoiceId();

			Rectangle boundingRectangle = actor.getBBox().getBoundingRectangle();
			float x = boundingRectangle.getX() + boundingRectangle.getWidth() / 2;
			float y = boundingRectangle.getY() + boundingRectangle.getHeight();
			
			Vector2 talkingTextPos = actor.getTalkingTextPos();

			if (talkingTextPos != null) {
				x += talkingTextPos.x;
				y += talkingTextPos.y;
			}

			w.getCurrentScene().getTextManager().addText(responseText, x, y, true, Text.Type.TALK, actor.getTextColor(),
					null, actor.getId(), responseVoiceId, null, wait ? cb : null);
		}

		return wait;
	}

}
