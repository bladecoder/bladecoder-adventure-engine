package com.bladecoder.engine.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionPropertyType;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.SceneActorRef;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

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
	
	@SuppressWarnings("unchecked")
	public static void setParam(Action action, String param, String value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = action.getClass();
		
		Field field = getParam(clazz, param);
		
		if(field == null)
			throw new NoSuchFieldException(param);
		
		
		final boolean accessible = field.isAccessible();
		field.setAccessible(true);

		if(field.getType().isAssignableFrom(String.class)) {
			field.set(action, value);
		} else if(field.getType().isAssignableFrom(boolean.class)) {
			field.setBoolean(action, Boolean.parseBoolean(value));
		} else if(field.getType().isAssignableFrom(Boolean.class)) {
			field.set(action, Boolean.valueOf(value));
		} else if(field.getType().isAssignableFrom(float.class)) {
			try {
				field.setFloat(action, Float.parseFloat(value));
			} catch (NumberFormatException ignored) {
			}		
		} else if(field.getType().isAssignableFrom(Float.class)) {
			try {
				field.set(action, Float.valueOf(value));
			} catch (NumberFormatException ignored) {
			}				
		} else if(field.getType().isAssignableFrom(int.class)) {
			try {
				field.setInt(action, Integer.parseInt(value));
			} catch (NumberFormatException ignored) {
			}
		} else if(field.getType().isAssignableFrom(Vector2.class)) {
			field.set(action, Param.parseVector2(value));
		} else if(field.getType().isAssignableFrom(SceneActorRef.class)) {
			String[] a = Param.parseString2(value);
			
			if(a != null)
				field.set(action, new SceneActorRef(a[0], a[1]));
		} else if(field.getType().isAssignableFrom(ActorAnimationRef.class)) {
			String[] a = Param.parseString2(value);
			
			field.set(action, new ActorAnimationRef(a[0], a[1]));
		} else if(field.getType().isAssignableFrom(Color.class)) {
			Color a = Param.parseColor(value);
			
			field.set(action, a);			
		} else if (field.getType().isEnum()) {
			field.set(action, Enum.valueOf(field.getType().asSubclass(Enum.class), value.toUpperCase()));
		} else {
			EngineLogger.error("ACTION FIELD TYPE NOT SUPPORTED -  type: " + field.getType());
		}

		field.setAccessible(accessible);
	}
	
	public static Field getParam(Class<?> clazz, String fieldName) {
	    Class<?> current = clazz;
	    
	    do {
	       try {
	           return current.getDeclaredField(fieldName);
	       } catch(NoSuchFieldException e) {}
	    } while((current = current.getSuperclass()) != null);
	    
	    return null;
	}
}
