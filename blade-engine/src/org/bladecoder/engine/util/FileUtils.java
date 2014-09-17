package org.bladecoder.engine.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.files.FileHandle;

public class FileUtils {
	/**
	 * For android, the exists method is very slow, this is a fast
	 * implementation
	 * 
	 * @return true if file exists
	 */
	public static boolean exists(FileHandle fh) {

		if (Gdx.app.getType() == ApplicationType.Android) {
			try {
				fh.read().close();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		return fh.exists();
	}
}
