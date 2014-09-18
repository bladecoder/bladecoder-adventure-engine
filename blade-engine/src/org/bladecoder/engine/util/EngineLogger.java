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
package org.bladecoder.engine.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class EngineLogger {
	private static String TAG = "ENGINE";
	private static int level = Application.LOG_ERROR;

	public static final int DEBUG0 = 0;
	public static final int DEBUG1 = 1;
	public static final int DEBUG2 = 2;

	public static int debugLevel = DEBUG0;

	public static void debug(String message) {
		Gdx.app.debug(TAG, message);
	}

	public static void error(String message) {
		if(message != null)
			Gdx.app.error(TAG, message);
	}

	public static void error(String message, Exception e) {
		if(message != null && e != null)
			Gdx.app.error(TAG, message, e);
	}

	public static void toggle() {
		if (level == Application.LOG_DEBUG)
			level = Application.LOG_ERROR;
		else
			level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}

	public static boolean debugMode() {
		if (level == Application.LOG_DEBUG)
			return true;

		return false;
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int level) {
		debugLevel = level;
	}
	
	public static void setDebug() {
		level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}
}
