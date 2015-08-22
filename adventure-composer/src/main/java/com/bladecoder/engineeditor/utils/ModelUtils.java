package com.bladecoder.engineeditor.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
	public static String getInfo(@Nonnull Class<?> clazz) {
		return clazz.getAnnotation(ModelDescription.class).value();
	}

	@Nonnull
	public static List<Param> getParams(@Nonnull Class<?> clazz) {
		final List<Param> params = new ArrayList<>();

		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final JsonProperty property = field.getAnnotation(JsonProperty.class);
				if (property == null) {
					continue;
				}
				final ModelPropertyType propertyType = field.getAnnotation(ModelPropertyType.class);
				final Param.Type type;
				if (propertyType != null) {
					type = propertyType.value();
				} else {
					type = typeFromFieldType(field);
					if (type == null) {
						throw new RuntimeException(clazz.getName() + '.' + field.getName() + " doesn't seem to be annotated with @ModelPropertyType, and its type can't be determined automatically");
					}
				}
				final JsonPropertyDescription propertyDescription = field.getAnnotation(JsonPropertyDescription.class);
				if (propertyDescription == null) {
					throw new RuntimeException(clazz.getName() + '.' + field.getName() + " doesn't seem to be annotated with @JsonPropertyDescription");
				}

				final String name = property.value() == null || property.value().trim().isEmpty() ? field.getName() : property.value();

				Enum[] options = null;
				if (field.getType().isEnum()) {
					final boolean accessible = field.isAccessible();
					field.setAccessible(true);
					options = (Enum[]) field.getType().getEnumConstants();
					field.setAccessible(accessible);
				}
				params.add(new Param(name, propertyDescription.value(), type, property.required(), property.defaultValue(), options));
			}
			clazz = clazz.getSuperclass();
		}
		return params;
	}

	private static Param.Type typeFromFieldType(Field field) {
		final Class<?> type = field.getType();
		if (type == String.class) {
			return null;        // Strings have special meaning, so they always need an explicit annotation
		} else if (type == Integer.class || type == int.class) {
			return Param.Type.INTEGER;
		} else if (type == Vector2.class) {
			return Param.Type.VECTOR2;
		} else if (type == Vector3.class) {
			return Param.Type.VECTOR3;
		}
		return null;
	}
}
