package com.bladecoder.engineeditor;

import com.bladecoder.engineeditor.model.PropertyChange;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class TrackPropertyChangesInterceptor {
	public static final String PROPERTY_CHANGE_FIELD = "__propertyChange";
	public static final String MODIFIED_FIELD = "__modified";

	static Method firePropertyChange;

	static {
		try {
			firePropertyChange = PropertyChange.class.getDeclaredMethod("firePropertyChange", String.class, Object.class, Object.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		firePropertyChange.setAccessible(true);
	}

	// FIXME: This can be faster if we generate bytecode instead of reflection lookups for fields & methods
	@SuppressWarnings("unused")
	public static void fireSetterEvent(@SuperCall Callable<Void> method,
	                                   @This Object instance,
	                                   @Origin String methodName,
	                                   @AllArguments Object[] args) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		final String setterName = methodName.substring(methodName.lastIndexOf('.') + 1, methodName.lastIndexOf('('));
		if (!setterName.startsWith("set") || args.length != 1) {
			throw new RuntimeException("@TrackPropertyChanges can only be used on simple setters for now");
		}
		String propertyName = setterName.substring(3);
		propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);

		Class<?> clazz = instance.getClass();

		final Field propertyChangeField = getField(clazz, PROPERTY_CHANGE_FIELD);

		PropertyChange propertyChange = (PropertyChange) propertyChangeField.get(instance);
		if (propertyChange == null) {
			propertyChange = new PropertyChange();
			propertyChangeField.set(instance, propertyChange);
		}

		Field propertyField = getField(clazz, propertyName);
		final Object oldValue = propertyField.get(instance);

		System.out.println("Doing set for " + propertyName + ' ' + "old " + oldValue + ' ' + Arrays.asList(args));
		try {
			method.call();
		} catch (Exception e) {
			e.printStackTrace();
		}

		final Field modifiedField = getField(clazz, MODIFIED_FIELD);

		modifiedField.set(instance, true);

		firePropertyChange.invoke(propertyChange, propertyName, oldValue, args[0]);
		System.out.println("Set is done");
	}

	// FIXME: Not the best place for this method
	@IgnoreForBinding
	public static boolean isModified(Object bean) {
		try {
			Field field = getField(bean.getClass(), MODIFIED_FIELD);
			return (boolean) field.get(bean);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// FIXME: Not the best place for this method
	@IgnoreForBinding
	public static PropertyChange getPropertyChange(Object bean) {
		try {
			Field field = getField(bean.getClass(), PROPERTY_CHANGE_FIELD);
			return (PropertyChange) field.get(bean);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@IgnoreForBinding
	private static Field getField(Class<?> clazz, String field) throws NoSuchFieldException {
		Field modifiedField = clazz.getDeclaredField(field);
		if (!modifiedField.isAccessible())
			modifiedField.setAccessible(true);
		return modifiedField;
	}
}