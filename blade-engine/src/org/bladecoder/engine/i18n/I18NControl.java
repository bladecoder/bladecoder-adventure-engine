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
package org.bladecoder.engine.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.files.FileHandle;

public class I18NControl extends Control {

	String encoding;

	public I18NControl(String encoding) {
		this.encoding = encoding;
	}

	@Override
	public ResourceBundle newBundle(String baseName, Locale locale, String format,
			ClassLoader loader, boolean reload) throws IllegalAccessException,
			InstantiationException, IOException {
		String bundleName = toBundleName(baseName, locale);
		String resourceName = toResourceName(bundleName, "properties");
		ResourceBundle bundle = null;
		InputStream inputStream = null;

		FileHandle fileHandle = EngineAssetManager.getInstance().getAsset(resourceName);

		if (fileHandle.exists()) {
			try {
				// inputStream = loader.getResourceAsStream(resourceName);
				inputStream = fileHandle.read();
				bundle = new PropertyResourceBundle(new InputStreamReader(inputStream, encoding));
			} finally {
				if (inputStream != null)
					inputStream.close();
			}
		}
		return bundle;
	}

}
