package com.bladecoder.engine.actions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ActionProperty {
	String defaultValue() default "";
    boolean required() default false;
	Param.Type type() default Param.Type.NOT_SET;
}
