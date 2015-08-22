package com.bladecoder.engineeditor.utils;

import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ModelUtils {
	public static String getInfo(@Nonnull Object obj) {
		return obj.getClass().getAnnotation(ModelDescription.class).value();
	}

	@Nonnull
	public static List<Param> getParams(@Nonnull Object obj) {
		final List<Param> params = new ArrayList<>();
		Class<?> clazz = obj.getClass();

		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final JsonProperty property = field.getAnnotation(JsonProperty.class);
				if (property == null) {
					continue;
				}
				final ModelPropertyType propertyType = field.getAnnotation(ModelPropertyType.class);
				final JsonPropertyDescription propertyDescription = field.getAnnotation(JsonPropertyDescription.class);

				final String name = property.value() == null || property.value().trim().isEmpty() ? field.getName() : property.value();

				Enum[] options = null;
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
		return params;
	}
}
