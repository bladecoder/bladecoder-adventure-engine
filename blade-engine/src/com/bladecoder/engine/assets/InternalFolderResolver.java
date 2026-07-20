package com.bladecoder.engine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

public class InternalFolderResolver implements FileHandleResolver {
	String basePath;

	@Override
	public FileHandle resolve(String fileName) {
		return Gdx.files.internal(basePath + fileName);
	}

	public InternalFolderResolver(String base) {
		basePath = base;
		
		if(!basePath.endsWith("/")) {
			basePath = base + "/";
		}
	}
}
