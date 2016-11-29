package com.bladecoder.engine.util;

import com.badlogic.gdx.math.Interpolation;

/**
 * Visual graphics and explanation:  https://github.com/libgdx/libgdx/wiki/Interpolation
 *
 * @author rgarcia
 */
public enum InterpolationMode {
	LINEAR(Interpolation.linear),
	FADE(Interpolation.fade),
	
	POW2(Interpolation.pow2),
	POW2IN(Interpolation.pow2In),
	POW2OUT(Interpolation.pow2Out),
	POW2ININVERSE(Interpolation.pow2InInverse),
	POW2OUTINVERSE(Interpolation.pow2OutInverse),
	
	POW3(Interpolation.pow3),
	POW3IN(Interpolation.pow3In),
	POW3OUT(Interpolation.pow3Out),
	POW3ININVERSE(Interpolation.pow3InInverse),
	POW3OUTINVERSE(Interpolation.pow3OutInverse),	
	
	POW4(Interpolation.pow4),
	POW4IN(Interpolation.pow4In),
	POW4OUT(Interpolation.pow4Out),
	
	POW5(Interpolation.pow5),
	POW5IN(Interpolation.pow5In),
	POW5OUT(Interpolation.pow5Out),
	
	SINE(Interpolation.sine),
	SINEIN(Interpolation.sineIn),
	SINEOUT(Interpolation.sineOut),
	
	EXP10(Interpolation.exp10),
	EXP10EIN(Interpolation.exp10In),
	EXP10OUT(Interpolation.exp10Out),
	
	CIRCLE(Interpolation.circle),
	CIRCLEIN(Interpolation.circleIn),
	CIRCLEOUT(Interpolation.circleOut),
	
	ELASTIC(Interpolation.elastic),
	ELASTICIN(Interpolation.elasticIn),
	ELASTICOUT(Interpolation.elasticOut),
	
	SWING(Interpolation.swing),
	SWINGIN(Interpolation.swingIn),
	SWINGOUT(Interpolation.swingOut),
	
	BOUNCE(Interpolation.bounce),
	BOUNCEIN(Interpolation.bounceIn),
	BOUNCEOUT(Interpolation.bounceOut);

	private final Interpolation interpolation;

	InterpolationMode(Interpolation interpolation) {
		this.interpolation = interpolation;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}
}
