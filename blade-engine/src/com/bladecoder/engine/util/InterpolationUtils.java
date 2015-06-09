package com.bladecoder.engine.util;

import com.badlogic.gdx.math.Interpolation;

public class InterpolationUtils {
	public static final String NAMES[] = { "linear", "elastic", "elasticIn", "elasticOut", "swing", "swingIn",
			"swingOut", "bounce", "bounceIn", "bounceOut", "pow2", "exp10" };
	public static final Interpolation INSTANCES[] = { null, Interpolation.elastic, Interpolation.elasticIn,
			Interpolation.elasticOut, Interpolation.swing, Interpolation.swingIn, Interpolation.swingOut,
			Interpolation.bounce, Interpolation.bounceIn, Interpolation.bounceOut, Interpolation.pow2,
			Interpolation.exp10 };
	
	public static Interpolation getInterpolation(String name) {
		
		for(int i = 0; i < NAMES.length; i++) {
			if(NAMES[i].equals(name))
				return INSTANCES[i];
		}
		
		return null;
	}

	public static String getName(Interpolation interpolation) {
		
		for(int i = 0; i < INSTANCES.length; i++) {
			if(INSTANCES[i] == interpolation)
				return NAMES[i];
		}
		
		return null;		
	}
}
