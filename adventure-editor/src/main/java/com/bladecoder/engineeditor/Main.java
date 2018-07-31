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
package com.bladecoder.engineeditor;

import java.io.File;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.EditorLogger.Levels;
import com.bladecoder.engineeditor.common.Versions;

public class Main extends LwjglApplication {

	public static void main(final String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = "Adventure Editor v" + Versions.getVersion();

		cfg.resizable = true;
		cfg.vSyncEnabled = true;
		// cfg.samples = 2;
		// cfg.useGL30 = true;

		if (Main.class.getResource("/images/ic_app64.png") != null)
			cfg.addIcon("images/ic_app64.png", FileType.Internal);

		if (Main.class.getResource("/images/ic_app32.png") != null)
			cfg.addIcon("images/ic_app32.png", FileType.Internal);

		if (Main.class.getResource("/images/ic_app16.png") != null)
			cfg.addIcon("images/ic_app16.png", FileType.Internal);

		parseArgs(args);

		new Main(new Editor(), cfg);
	}

	private static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f") && i < args.length - 1) {
				try {
					File file = new File(args[i + 1]).getCanonicalFile();
					Ctx.project.loadProject(file);
				} catch (Exception ex) {
					EditorLogger.printStackTrace(ex);
				}
			} else if (args[i].equals("-d")) {
				EditorLogger.setDebugLevel(Levels.DEBUG);
			}
		}
	}

	public Main(Editor editor, LwjglApplicationConfiguration cfg) {
		super(editor, cfg);

		Gdx.graphics.setWindowedMode(Math.max((int) (Gdx.graphics.getDisplayMode().width * 0.9), 1920 / 2),
				Math.max((int) (Gdx.graphics.getDisplayMode().height * 0.9), 1080 / 2));
	}

	@Override
	public void exit() {
		((Editor) listener).exit();
	}

	public void exitSaved() {
		super.exit();
	}
}
