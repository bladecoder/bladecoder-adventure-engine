package com.bladecoder.engine.serialization;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.ParticleRenderer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.TextRenderer;
import com.bladecoder.engine.model.WalkZoneActor;
import com.bladecoder.engine.model.World;

/**
 * The libgdx Json object with the World instance, the serialization mode and
 * the ActionCallback + World serializers added.
 * 
 * @author rgarcia
 */
public class BladeJson extends Json {
	public enum Mode {
		MODEL, STATE
	};

	private final World w;
	private final Mode mode;
	private boolean init;

	public BladeJson(World w, Mode mode, boolean init) {
		super();

		this.w = w;
		this.mode = mode;
		this.init = init;

		// Add tags for known classes to reduce .json size.
		addClassTag(SpineAnimationDesc.class);
		addClassTag(AtlasAnimationDesc.class);

		addClassTag(CharacterActor.class);
		addClassTag(AnchorActor.class);
		addClassTag(ObstacleActor.class);
		addClassTag(InteractiveActor.class);
		addClassTag(SpriteActor.class);
		addClassTag(WalkZoneActor.class);

		addClassTag(AtlasRenderer.class);
		addClassTag(ImageRenderer.class);
		addClassTag(ParticleRenderer.class);
		addClassTag(Sprite3DRenderer.class);
		addClassTag(TextRenderer.class);

		ObjectMap<String, Class<? extends Action>> classTags = ActionFactory.getClassTags();

		for (ObjectMap.Entry<String, Class<? extends Action>> e : classTags.entries()) {
			addClassTag(e.key, e.value);
		}

	}

	public BladeJson(World w, Mode mode) {
		this(w, mode, true);
	}

	public World getWorld() {
		return w;
	}

	public Mode getMode() {
		return mode;
	}

	public boolean getInit() {
		return init;
	}

	public void setInit(boolean init) {
		this.init = init;
	}

	public void addClassTag(Class<?> tag) {
		addClassTag(tag.getSimpleName(), tag);
	}
}
