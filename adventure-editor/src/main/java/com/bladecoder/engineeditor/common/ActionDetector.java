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

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.common.ActionUtils;
import com.bladecoder.engine.common.EngineLogger;

import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;

public class ActionDetector {
	
	private static final HashMap<String, String> actions = new HashMap<String, String>();
	
	public static void detect() {
		actions.clear();
		
		final TypeReporter reporter = new TypeReporter() {

		    @SuppressWarnings("unchecked")
		    @Override
		    public Class<? extends Annotation>[] annotations() {
		        return new Class[]{ActionDescription.class};
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
				
				if(!name.equals("End"))
					actions.put(name, className);
			}


		};
		
		final AnnotationDetector cf = new AnnotationDetector(reporter);
		
		try {
			cf.detect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String []getActionNames() {
		detect();
		
		return  actions.keySet().toArray(new String[actions.size()]);
	}
	
	public static Action create(String name,
			HashMap<String, String> params) {
		String className = actions.get(name);

		if (className == null) {
			EngineLogger.error( "Action with name '" + name
					+ "' not found.");

			return null;
		}

		return ActionFactory.createByClass(className, params);
	}
	

}
