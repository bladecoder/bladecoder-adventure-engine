/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.common;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Hashtable;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;

public class ActionDetector {

	private static HashMap<String, Class<?>> actions = null;

	public static void detect() {

		actions = new HashMap<String, Class<?>>();

		final TypeReporter reporter = new TypeReporter() {

			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { ActionDescription.class };
			}

			@SuppressWarnings("unchecked")
			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				Class<Action> c = null;

				try {
					c = ClassReflection.forName(className);
				} catch (ReflectionException e) {
					e.printStackTrace();
				}

				String name = ActionUtils.getName(c);

				if (!name.equals("End"))
					actions.put(name, c);
			}

		};

		final AnnotationDetector cf = new AnnotationDetector(reporter);

		try {
			cf.detect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] getActionNames() {
		if (actions == null) {
			detect();
		}

		addCustomActions();

		return actions.keySet().toArray(new String[actions.size()]);
	}

	public static Action create(String name, HashMap<String, String> params) {
		Class<?> c = actions.get(name);
		
		if(c == null) {
			addCustomActions();
			 c = actions.get(name);
		}

		if (c == null) {
			EngineLogger.error("Action with name '" + name + "' not found.");

			return null;
		}

		try {
			return ActionFactory.createByClass(c.getName(), params);
		} catch (ClassNotFoundException | ReflectionException e) {
			EngineLogger.error("Action with name '" + name + "' not found.");

			return null;
		}
	}

	private static void addCustomActions() {
		Hashtable<String, Class<?>> coreClasses = ((FolderClassLoader) ActionFactory.getActionClassLoader())
				.getClasses();

		for (Class<?> c : coreClasses.values()) {
			if (c.getAnnotation(ActionDescription.class) != null) {
				actions.put(ActionUtils.getName(c), c);
			}
		}
	}

}
