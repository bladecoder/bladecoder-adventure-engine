package com.bladecoder.engine.actions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ActionPropertyType {
	Param.Type value();
}
