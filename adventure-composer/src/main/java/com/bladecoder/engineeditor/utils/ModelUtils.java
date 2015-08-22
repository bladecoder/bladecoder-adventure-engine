package com.bladecoder.engineeditor.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;

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

				Object[] options = null;
				if (field.getType().isEnum()) {
					final boolean accessible = field.isAccessible();
					field.setAccessible(true);
					options = field.getType().getEnumConstants();
					field.setAccessible(accessible);
				}
				if (options == null && type == Param.Type.OPTION) {
					options = optionsFromFieldType(field);
					if (options == null) {
						throw new RuntimeException(clazz.getName() + '.' + field.getName() + " is an OPTION type, but we can't find suitable options for it");
					}
				}
				params.add(new Param(formatName(name), propertyDescription.value(), type, property.required(), property.defaultValue(), options));
			}
			clazz = clazz.getSuperclass();
		}
		return params;
	}

	private static String formatName(String name) {
		StringBuilder sb = new StringBuilder();
		char[] charArray = name.toCharArray();
		for (int j = 0; j < charArray.length; j++) {
			char c = charArray[j];

			if (Character.isUpperCase(c) && j != 0) {
				sb.append(' ');
				sb.append(Character.toLowerCase(c));
			} else if (j == 0) {
				sb.append(Character.toUpperCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static Object[] optionsFromFieldType(Field field) {
		final Class<?> type = field.getType();
		// FIXME: This can miss values, as converters can be registered thorugh other means
		final JsonSubTypes jsonSubTypes = type.getAnnotation(JsonSubTypes.class);
		if (jsonSubTypes == null) {
			return null;
		}

		final List<String> result = new ArrayList<>();
		for (JsonSubTypes.Type jsonType : jsonSubTypes.value()) {
			String name = jsonType.name();
			if (name.isEmpty()) {
				final JsonTypeName jsonTypeName = jsonType.value().getAnnotation(JsonTypeName.class);
				if (jsonTypeName == null) {
					throw new RuntimeException(jsonType.value().getName() + " is a JSON SubType, but it doesn't have a @JsonTypeName or a name on its parent (" + type.getName() + ")");
				}
				name = jsonTypeName.value();
			}
			result.add(name);
		}

		return result.toArray(new String[result.size()]);
	}

	private static Param.Type typeFromFieldType(Field field) {
		final Class<?> type = field.getType();
		if (type == String.class) {
			return null;        // Strings have special meaning, so they always need an explicit annotation
		} else if (type == Boolean.class || type == boolean.class) {
			return Param.Type.BOOLEAN;
		} else if (type.isEnum()) {
			return Param.Type.OPTION;
		} else if (type == Integer.class || type == int.class) {
			return Param.Type.INTEGER;
		} else if (type == Float.class || type == float.class) {
			return Param.Type.FLOAT;
		} else if (type == Vector2.class) {
			return Param.Type.VECTOR2;
		} else if (type == Vector3.class) {
			return Param.Type.VECTOR3;
		}
		return null;
	}
}
