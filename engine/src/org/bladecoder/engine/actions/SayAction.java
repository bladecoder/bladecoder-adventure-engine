package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.Text;
import org.bladecoder.engine.model.TextManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SayAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Says a text";
	public static final Param[] PARAMS = {
			new Param("text", "The 'text' to show", Type.STRING),
			new Param("speech", "The 'soundId' to play if selected", Type.STRING),
			new Param(
					"wait",
					"If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true),
			new Param("type", "The type of the text: 'talk', 'rectangle' (default) and 'plain'",
					Type.STRING, true, "rectangle", new String[] {"rectangle", "talk", "plain"}), };

	private String soundId;
	private String text;
	private boolean wait = true;

	private String actorId;

	private Text.Type type = Text.Type.RECTANGLE;

	private String previousFA = null;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		soundId = params.get("speech");
		text = params.get("text");

		if (params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}

		if (params.get("type") != null) {
			if (params.get("type").equalsIgnoreCase("talk")) {
				type = Text.Type.TALK;
			} else if (params.get("type").equalsIgnoreCase("rectangle")) {
				type = Text.Type.RECTANGLE;
			} else if (params.get("type").equalsIgnoreCase("plain")) {
				type = Text.Type.PLAIN;
			}
		}

		// TODO Add parameters like color, position of text, etc...
	}

	@Override
	public void run() {
		EngineLogger.debug("SAY ACTION");
		Actor actor = World.getInstance().getCurrentScene().getActor(actorId);

		if (type == Text.Type.TALK)
			restoreStandPose((SpriteActor) actor);

		if (soundId != null)
			actor.playSound(soundId);

		if (text != null) {
			float x, y;
			boolean quee = false;

			if (type == Text.Type.RECTANGLE) {
				x = y = TextManager.POS_SUBTITLE;
			} else {
				Vector2 pos = ((SpriteActor) actor).getPosition();

				// WorldCamera c = World.getInstance().getCamera();
				// Vector3 p = c.scene2screen(pos.x, pos.y +
				// ((SpriteActor)actor).getHeight());

				x = pos.x;
				y = pos.y + ((SpriteActor) actor).getHeight();
				// quee = true;
			}

			if (type == Text.Type.TALK) {
				previousFA = ((SpriteActor) actor).getRenderer().getCurrentFrameAnimationId();
				((SpriteActor) actor).startFrameAnimation(getTalkFA(previousFA),
						EngineTween.FROM_FA, 0, null);
			}

			if (wait) {
				World.getInstance().getTextManager()
						.addSubtitle(text, x, y, quee, type, Color.BLACK, this);
			} else {
				World.getInstance().getTextManager()
						.addSubtitle(text, x, y, quee, type, Color.BLACK, null);
				onEvent();
			}
		}
	}

	@Override
	public void onEvent() {
		if (this.type == Text.Type.TALK) {
			SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene()
					.getActor(actorId);
			actor.startFrameAnimation(previousFA, EngineTween.FROM_FA, 0, null);
		}

		super.onEvent();
	}

	private void restoreStandPose(SpriteActor a) {
		if (a == null)
			return;

		String fa = a.getRenderer().getCurrentFrameAnimationId();

		if (fa.startsWith("talk.")) { // If the actor was already talking we
										// restore the actor to the 'stand' pose
			int idx = fa.indexOf('.');
			String prevFA = "stand" + fa.substring(idx);
			a.startFrameAnimation(prevFA, null);
		}
	}

	private String getTalkFA(String prevFA) {
		if (prevFA.endsWith("left"))
			return "talk.left";
		else if (prevFA.endsWith("right"))
			return "talk.right";

		return "talk";
	}

	@Override
	public void write(Json json) {

		json.writeValue("soundId", soundId);
		json.writeValue("text", text);
		json.writeValue("wait", wait);
		json.writeValue("actorId", actorId);
		json.writeValue("previousFA", previousFA);
		json.writeValue("type", type);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		soundId = json.readValue("soundId", String.class, jsonData);
		text = json.readValue("text", String.class, jsonData);
		wait = json.readValue("wait", Boolean.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		previousFA = json.readValue("previousFA", String.class, jsonData);
		type = json.readValue("type", Text.Type.class, jsonData);
		super.read(json, jsonData);
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
