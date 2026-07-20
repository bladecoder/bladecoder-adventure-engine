package com.bladecoder.engine.actions;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.SpriteScaleTween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;

@ActionDescription(name = "ScaleAnimXY", value = "Scale animation for sprite actors")
public class ScaleAnimActionXY implements Action {
	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.SPRITE_ACTOR)
	private String actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The target scale")
	private Vector2 scale;

	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("Duration of the animation in seconds")
	private float speed;

	@ActionProperty
	@ActionPropertyDescription("The The times to repeat")
	private int count = 1;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the transition is showed and the action continues inmediatly")
	private boolean wait = true;

	@ActionProperty(required = true, defaultValue = "REPEAT")
	@ActionPropertyDescription("The repeat mode")
	private Tween.Type repeat = Tween.Type.REPEAT;

	@ActionProperty
	@ActionPropertyDescription("The interpolation mode")
	private InterpolationMode interpolation;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		SpriteActor a = (SpriteActor) w.getCurrentScene().getActor(actor, true);

		SpriteScaleTween t = new SpriteScaleTween();
		t.start(a, repeat, count, scale.x, scale.y, speed, interpolation, wait ? cb : null);

		a.addTween(t);

		return wait;
	}

}
