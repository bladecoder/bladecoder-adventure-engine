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

import java.io.InputStream;
import java.util.Properties;

public class Versions {
	private static final String VERSION_PROP="version";
	private static final String LIBGDX_VERSION_PROP="libgdxVersion";
	private static final String ROBOVM_VERSION_PROP="roboVMVersion";
	private static final String BUILDTOOLS_VERSION_PROP="buildToolsVersion";
	private static final String ANDROID_API_LEVEL_PROP="androidAPILevel";
	private static final String ANDROID_GRADLE_VERSION_PROP = "androidGradlePluginVersion";
	private static final String ROBOVM_GRADLE_VERSION_PROP = "roboVMGradlePluginVersion";
	private static final String BLADE_INK_VERSION_PROP = "bladeInkVersion";
	
	public static final String PROPERTIES_FILENAME = "/versions.properties";

	private static Properties config = null;
	
	private static String getProperty(String key, String defaultValue) {
		if(config == null) {
			config = new Properties();
			
			try {
				InputStream is = Versions.class.getResourceAsStream(PROPERTIES_FILENAME);
				config.load(is);
			} catch (Exception e) {
				EditorLogger.error("ERROR LOADING VERSION FILE: " + e.getMessage());
			}
		}
		
		return config.getProperty(key, defaultValue);
	}
	
	public static String getVersion() {
		return getProperty(VERSION_PROP,null);
	}
	
	public static String getLibgdxVersion() {
		return getProperty(LIBGDX_VERSION_PROP,null);
	}
	
	public static String getRoboVMVersion() {
		return getProperty(ROBOVM_VERSION_PROP,null);
	}
	
	public static String getBuildToolsVersion() {
		return getProperty(BUILDTOOLS_VERSION_PROP,null);
	}
	
	public static String getAndroidAPILevel() {
		return getProperty(ANDROID_API_LEVEL_PROP,null);
	}
	
	public static void setBuildToolsVersion(String v) {
		config.setProperty(BUILDTOOLS_VERSION_PROP, v);
	}
	
	public static void setAndroidAPILevel(String v) {
		config.setProperty(ANDROID_API_LEVEL_PROP, v);
	}
	
	public static String getAndroidGradlePluginVersion() {
		return getProperty(ANDROID_GRADLE_VERSION_PROP,null);
	}
	
	public static String getROBOVMGradlePluginVersion() {
		return getProperty(ROBOVM_GRADLE_VERSION_PROP,null);
	}
	
	public static String getBladeInkVersion() {
		return getProperty(BLADE_INK_VERSION_PROP,null);
	}

}
