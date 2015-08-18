package com.bladecoder.engineeditor.utils;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;

public class ActionUtils {
	public static String getInfo(Action action) {
		return action.getClass().getAnnotation(ActionDescription.class).value();
	}
}
