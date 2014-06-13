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
package org.bladecoder.engine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
import com.badlogic.gdx.files.FileHandle;

public class EngineResolutionFileResolver implements FileHandleResolver {

	protected final FileHandleResolver baseResolver;
	protected final Resolution[] descriptors;
	
	
	private Resolution bestDesc;

	public EngineResolutionFileResolver(FileHandleResolver baseResolver, Resolution... descriptors) {
		this.baseResolver = baseResolver;
		this.descriptors = descriptors;
		
		bestDesc = choose(descriptors);
	}

	@Override
	public FileHandle resolve(String fileName) {		
		FileHandle originalHandle = new FileHandle(fileName);
		FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestDesc.suffix));
		
		if (!handle.exists())
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
		FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestDesc.suffix));
		
		if (handle.exists())
			return true;
		
		handle = baseResolver.resolve(fileName);
		
		if (handle.exists())
			return true;
		
		return false;
	}
	
	/**
	 * Skip the resolution resolver. In Android the exists() method is expensive, so this
	 * method save a exists call.
	 * 
	 * @param fileName
	 * @return
	 */
	public FileHandle baseResolve(String fileName) {
		return baseResolver.resolve(fileName);
	}

	static public Resolution choose(Resolution... descriptors) {
		if (descriptors == null)
			throw new IllegalArgumentException("descriptors cannot be null.");
		
		Resolution best = descriptors[0];
		int bestDist = Math.abs(Gdx.graphics.getWidth() - best.portraitWidth);

		for (int i = 1; i < descriptors.length; i++) {
			Resolution other = descriptors[i];
			int dist =  Math.abs(Gdx.graphics.getWidth() - other.portraitWidth);
			
			if (dist < bestDist) {
				best = descriptors[i];
				bestDist = dist;
			}
		}

		return best;
	}
	
	public Resolution[] getResolutions() {
		return descriptors;
	}
	
	public Resolution getResolution() {
		return bestDesc;
	}
	
	/**
	 * Sets a fixed resolution, disabling choosing the best resolution.
	 * 
	 * @param width The width of the resolution
	 */
	public void forceResolution(int width) {
		for (int i = 0; i < descriptors.length; i++) {
			if(descriptors[i].portraitWidth == width) {
				bestDesc = descriptors[i];
				return;
			}
		}
	}
}
