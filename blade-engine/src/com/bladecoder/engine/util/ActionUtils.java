package com.bladecoder.engine.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionProperty;
import com.bladecoder.engine.actions.ActionPropertyDescription;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.actions.SceneActorRef;

public class ActionUtils {
	public static String getInfo(Action action) {
		return action.getClass().getAnnotation(ActionDescription.class).value();
	}

	public static Param[] getParams(Action action) {
		List<Param> params = new ArrayList<>();
		Class<?> clazz = action.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final ActionProperty property = field.getAnnotation(ActionProperty.class);
				if (property == null) {
					continue;
				}
				
				final ActionPropertyDescription propertyDescription = field.getAnnotation(ActionPropertyDescription.class);
				
				// properties without description are not editables but will be saved in the model.
				if(propertyDescription == null)
					continue;

				final String name = field.getName();
				Param.Type type = property.type();
				
				Enum<?>[] options = null;
				
				if (field.getType().isEnum()) {
					final boolean accessible = field.isAccessible();
					field.setAccessible(true);
					options = (Enum[]) field.getType().getEnumConstants();
					field.setAccessible(accessible);
					
					type = Param.Type.OPTION;
				} else if(property.type() == Param.Type.NOT_SET){
					type = getType(field);
				}
				
				params.add(new Param(name, propertyDescription != null?propertyDescription.value():"", type, property.required(), property.defaultValue(), options));
			}
			clazz = clazz.getSuperclass();
		}
		return params.toArray(new Param[params.size()]);
	}
	
	public static String[] getFieldNames(Action a) {
		List<String> result = new ArrayList<>();
		Class<?> clazz = a.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final ActionProperty property = field.getAnnotation(ActionProperty.class);
				if (property == null) {
					continue;
				}

				final String name = field.getName();
				result.add(name);
			}
			clazz = clazz.getSuperclass();
		}
		return result.toArray(new String[result.size()]);
	}
	
	private static Type getType(Field field) {
		if(field.getType().isAssignableFrom(String.class)) {
			return Param.Type.STRING;
		} else if(field.getType().isAssignableFrom(boolean.class)) {
			return Param.Type.BOOLEAN;
		} else if(field.getType().isAssignableFrom(Boolean.class)) {
			return Param.Type.BOOLEAN;
		} else if(field.getType().isAssignableFrom(float.class)) {
			return Param.Type.FLOAT;	
		} else if(field.getType().isAssignableFrom(Float.class)) {
			return Param.Type.FLOAT;				
		} else if(field.getType().isAssignableFrom(int.class)) {
			return Param.Type.INTEGER;
		} else if(field.getType().isAssignableFrom(Vector2.class)) {
			return Param.Type.VECTOR2;
		} else if(field.getType().isAssignableFrom(SceneActorRef.class)) {
			return Param.Type.SCENE_ACTOR;
		} else if(field.getType().isAssignableFrom(ActorAnimationRef.class)) {
			return Param.Type.ACTOR_ANIMATION;
		} else if(field.getType().isAssignableFrom(Color.class)) {
			return Param.Type.COLOR;			
		} else if (field.getType().isEnum()) {
			return Param.Type.OPTION;
		} else {
			EngineLogger.error("ACTION FIELD TYPE NOT SUPPORTED -  type: " + field.getType());
		}
		
		return Param.Type.NOT_SET;
	}

	@SuppressWarnings("unchecked")
	public static void setParam(Action action, String param, String value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = action.getClass();
		
		Field field = getField(clazz, param);
		
		if(field == null)
			throw new NoSuchFieldException(param);
		
		
		final boolean accessible = field.isAccessible();
		field.setAccessible(true);

		if(field.getType().isAssignableFrom(String.class)) {
			field.set(action, value);
		} else if(field.getType().isAssignableFrom(boolean.class)) {
			field.setBoolean(action, Boolean.parseBoolean(value));
		} else if(field.getType().isAssignableFrom(Boolean.class)) {
			Boolean b = null;
			
			if(value != null)
				b = Boolean.valueOf(value);
			
			field.set(action, b);
		} else if(field.getType().isAssignableFrom(float.class)) {
			try {
				field.setFloat(action, Float.parseFloat(value));
			} catch (NumberFormatException ignored) {
			}		
		} else if(field.getType().isAssignableFrom(Float.class)) {
			try {
				Float f = null;
				
				if(value != null)
					f = Float.valueOf(value);
				
				field.set(action, f);
			} catch (NumberFormatException ignored) {
			}				
		} else if(field.getType().isAssignableFrom(int.class)) {
			try {
				Integer i = null;
				
				if(value != null)
					i = Integer.parseInt(value);
				
				field.setInt(action, i);
			} catch (NumberFormatException ignored) {
			}
		} else if(field.getType().isAssignableFrom(Vector2.class)) {
			field.set(action, Param.parseVector2(value));
		} else if(field.getType().isAssignableFrom(SceneActorRef.class)) {
			field.set(action, new SceneActorRef(value));
		} else if(field.getType().isAssignableFrom(ActorAnimationRef.class)) {
			field.set(action, new ActorAnimationRef(value));
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
	
	
	public static String getStringValue(Action a, String param) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		String result = null;
		
		Class<?> clazz = a.getClass();
		
		Field field = getField(clazz, param);
		
		if(field == null)
			throw new NoSuchFieldException(param);
		
		
		final boolean accessible = field.isAccessible();
		field.setAccessible(true);

		if(field.getType().isAssignableFrom(String.class)) {
			result = (String)field.get(a);
		} else if(field.getType().isAssignableFrom(boolean.class)) {
			result = Boolean.toString(field.getBoolean(a));
		} else if(field.getType().isAssignableFrom(Boolean.class)) {
			Object o = field.get(a);
			
			if(o != null)
				result = o.toString();
		} else if(field.getType().isAssignableFrom(float.class)) {
			result = Float.toString(field.getFloat(a));		
		} else if(field.getType().isAssignableFrom(Float.class)) {
			Object o = field.get(a);
			
			if(o != null)
				result = o.toString();			
		} else if(field.getType().isAssignableFrom(int.class)) {
			result = Integer.toString(field.getInt(a));
		} else if(field.getType().isAssignableFrom(Vector2.class)) {
			result = Param.toStringParam((Vector2)field.get(a));
		} else if(field.getType().isAssignableFrom(SceneActorRef.class)) {
			Object o = field.get(a);
			
			if(o != null)
				result = o.toString();
		} else if(field.getType().isAssignableFrom(ActorAnimationRef.class)) {
			Object o = field.get(a);
			
			if(o != null)
				result = o.toString();
		} else if(field.getType().isAssignableFrom(Color.class)) {
			Object o = field.get(a);
			
			if(o != null)
				result = o.toString();		
		} else if (field.getType().isEnum()) {
			Object o = field.get(a);
			
			if(o != null)
				result = ((Enum<?>)o).name();	
		} else {
			EngineLogger.error("ACTION FIELD TYPE NOT SUPPORTED -  type: " + field.getType());
		}

		field.setAccessible(accessible);
		
		return result;
	}
	
	public static Field getField(Class<?> clazz, String fieldName) {
	    Class<?> current = clazz;
	    
	    do {
	       try {
	           return current.getDeclaredField(fieldName);
	       } catch(NoSuchFieldException e) {}
	    } while((current = current.getSuperclass()) != null);
	    
	    return null;
	}
	
	public static void writeJson(Action a, Json json) {
		Class<?> clazz = a.getClass();
		json.writeObjectStart(clazz, null);
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				final ActionProperty property = field.getAnnotation(ActionProperty.class);
				if (property == null) {
					continue;
				}

				// json.writeField(a, field.getName());
				final boolean accessible = field.isAccessible();
				field.setAccessible(true);

				try {
					Object o = field.get(a);

					// doesn't write null fields
					if (o == null)
						continue;

					if(o instanceof SceneActorRef) {
						SceneActorRef sceneActor = (SceneActorRef)o;
						json.writeValue(field.getName(), sceneActor.toString());
					} else if(o instanceof ActorAnimationRef) {
						ActorAnimationRef aa = (ActorAnimationRef)o;
						json.writeValue(field.getName(), aa.toString());
					} else if(o instanceof Color) {
					} else if(o instanceof Vector2) {
						json.writeValue(field.getName(), Param.toStringParam((Vector2) o));
					} else {
						json.writeValue(field.getName(), o);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {

				}

				field.setAccessible(accessible);
			}
			clazz = clazz.getSuperclass();
		}
		json.writeObjectEnd();		
	}
	
	public static Action readJson(Json json, JsonValue jsonData) {
		String className = jsonData.getString("class", null);
		Action action = null;
		if (className != null) {
			jsonData.remove("class");
			Class<?> clazz = null;

			
			try {
				clazz = ClassReflection.forName(className);
				action = (Action)clazz.newInstance();
			} catch (ReflectionException|InstantiationException | IllegalAccessException ex) {
				throw new SerializationException(ex);
			}
			
			for(int j = 0; j<jsonData.size; j++) {
				JsonValue v = jsonData.get(j);
				try {
					if(v.isNull())
						ActionUtils.setParam(action, v.name, null);
					else
						ActionUtils.setParam(action, v.name, v.asString());
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					throw new SerializationException("Action: " + action.getClass().getCanonicalName() + "Param: " + v.name, e);
				}
			}
		}
		
		
		return action;
	}
	
}
