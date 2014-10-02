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
package com.bladecoder.engine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

public class BasePathResolver implements FileHandleResolver {
	String basePath;

	@Override
	public FileHandle resolve(String fileName) {
		
		String fullName;

		if (fileName.startsWith("/")) {
			fullName = fileName;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(basePath);
			sb.append("/");
			sb.append(fileName);
			fullName = sb.toString();
		}
		
		//EngineLogger.debug(fullName);

		return Gdx.files.absolute(fullName);
	}

	public BasePathResolver(String base) {
		basePath = base;
	}
}
