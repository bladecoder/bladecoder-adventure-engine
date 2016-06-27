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
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
import com.badlogic.gdx.files.FileHandle;
import com.bladecoder.engine.common.EngineLogger;
import com.bladecoder.engine.common.FileUtils;

public class EngineResolutionFileResolver implements FileHandleResolver {

	private final FileHandleResolver baseResolver;
	private Resolution[] descriptors;
	
	private Resolution bestDesc;
	private String fixResolution;

	public EngineResolutionFileResolver(FileHandleResolver baseResolver) {
		this.baseResolver = baseResolver;
	}

	@Override
	public FileHandle resolve(String fileName) {		
		FileHandle originalHandle = new FileHandle(fileName);
		FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestDesc.folder));
		
		if (!FileUtils.exists(handle))
			handle = baseResolver.resolve(fileName);
		
		return handle;
	}

	protected String resolve(FileHandle originalHandle, String suffix) {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(originalHandle.parent());
		stringBuilder.append("/");
		stringBuilder.append(suffix);
		stringBuilder.append("/");
		stringBuilder.append(originalHandle.name());
		
		return stringBuilder.toString();
	}
	
	
	public boolean exists(String fileName) {
		FileHandle originalHandle = new FileHandle(fileName);
		FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestDesc.folder));
		
		if (FileUtils.exists(handle))
			return true;
		
		handle = baseResolver.resolve(fileName);
		
		if (FileUtils.exists(handle))
			return true;
		
		return false;
	}
	
	public FileHandleResolver getBaseResolver() {
		return baseResolver;
	}
	
	/**
	 * Skip the resolution resolver. In Android the exists() method is expensive, so this
	 * method save a exists call.
	 */
	public FileHandle baseResolve(String fileName) {
		return baseResolver.resolve(fileName);
	}
	
	public Resolution[] getResolutions() {
		return descriptors;
	}
	
	public Resolution getResolution() {
		return bestDesc;
	}
	
	public void selectResolution() {
		if(fixResolution != null)
			selectFixedResolution();
		else
			selectBestResolution();
	}
	
	private void selectBestResolution() {
		bestDesc = descriptors[0];
		
		int width = Gdx.graphics.getWidth() > Gdx.graphics.getHeight() ?Gdx.graphics.getWidth():Gdx.graphics.getHeight();
		
		int bestDist = Math.abs(width - bestDesc.portraitWidth);

		for (int i = 1; i < descriptors.length; i++) {
			Resolution other = descriptors[i];
			int dist =  Math.abs(width - other.portraitWidth);
			
			if (dist < bestDist) {
				bestDesc = descriptors[i];
				bestDist = dist;
			}
		}
	}
	
	public void setResolutions(Resolution[] resolutions) {
		this.descriptors = resolutions;
	}
	
	/**
	 * Sets a fixed prefix, disabling choosing the best resolution.
	 */
	private void selectFixedResolution() {
		for (int i = 0; i < descriptors.length; i++) {
			if(descriptors[i].folder.equals(fixResolution)) {
				bestDesc = descriptors[i];
				return;
			}
		}
		
		if(bestDesc == null) {
			EngineLogger.error("Requested resolution not found: " + fixResolution);
			selectBestResolution();
		}
	}
	
	public void setFixedResolution(String suffix) {
		fixResolution = suffix;
	}

}
