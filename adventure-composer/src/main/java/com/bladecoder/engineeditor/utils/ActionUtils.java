package com.bladecoder.engineeditor.utils;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionPropertyType;
import com.bladecoder.engine.actions.Param;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ActionUtils {
	public static String getInfo(Action action) {
		return action.getClass().getAnnotation(ActionDescription.class).value();
	}

	public static Param[] getParams(Action action) {
		List<Param> params = new ArrayList<>();
		Class<?> clazz = action.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final JsonProperty property = field.getAnnotation(JsonProperty.class);
				if (property == null) {
					continue;
				}
				final ActionPropertyType propertyType = field.getAnnotation(ActionPropertyType.class);
				final JsonPropertyDescription propertyDescription = field.getAnnotation(JsonPropertyDescription.class);

				final String name = property.value() == null || property.value().trim().isEmpty() ? field.getName() : property.value();

				Enum<?>[] options = null;
				if (field.getType().isEnum()) {
					final boolean accessible = field.isAccessible();
					field.setAccessible(true);
					options = (Enum[]) field.getType().getEnumConstants();
					field.setAccessible(accessible);
				}
				params.add(new Param(name, propertyDescription.value(), propertyType.value(), property.required(), property.defaultValue(), options));
			}
			clazz = clazz.getSuperclass();
		}
		return params.toArray(new Param[params.size()]);
	}
}
