package org.bladecoder.engine.assets;

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
