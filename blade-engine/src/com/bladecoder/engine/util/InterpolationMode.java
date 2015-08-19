package com.bladecoder.engine.util;

import com.badlogic.gdx.math.Interpolation;

public enum InterpolationMode {
	LINEAR(null),
	ELASTIC(Interpolation.elastic),
	ELASTICIN(Interpolation.elasticIn),
	ELASTICOUT(Interpolation.elasticOut),
	SWING(Interpolation.swing),
	SWINGIN(Interpolation.swingIn),
	SWINGOUT(Interpolation.swingOut),
	BOUNCE(Interpolation.bounce),
	BOUNCEIN(Interpolation.bounceIn),
	BOUNCEOUT(Interpolation.bounceOut),
	POW2(Interpolation.pow2),
	EXP10(Interpolation.exp10);

	private final Interpolation interpolation;

	InterpolationMode(Interpolation interpolation) {
		this.interpolation = interpolation;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}
}
