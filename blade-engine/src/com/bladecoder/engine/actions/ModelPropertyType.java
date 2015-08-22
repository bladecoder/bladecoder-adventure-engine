package com.bladecoder.engine.actions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @deprecated We need to use the field type instead of this
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelPropertyType {
	Param.Type value();
}
