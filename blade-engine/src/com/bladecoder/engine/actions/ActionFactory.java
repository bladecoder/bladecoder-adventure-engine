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
package com.bladecoder.engine.actions;

import java.util.HashMap;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

public class ActionFactory {

	private static ClassLoader loader = ActionFactory.class.getClassLoader();

	public static void setActionClassLoader(ClassLoader loader) {
		ActionFactory.loader = loader;
	}
	
	public static ClassLoader getActionClassLoader() {
		return loader;
	}

	public static Action createByClass(String className, HashMap<String, String> params) throws ClassNotFoundException, ReflectionException {

		Action a = null;

		Class<?> c = Class.forName(className, true, loader);
		a = (Action) ClassReflection.newInstance(c);

		if (params != null) {

			for (String key : params.keySet()) {
				String value = params.get(key);

				try {
					ActionUtils.setParam(a, key, value);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					EngineLogger.error("Error Setting Action Param - Action:" + className + " Param: " + key
							+ " Value: " + value + " Msg: NOT FOUND " + e.getMessage());
				}
			}
		}

		return a;
	}
}
