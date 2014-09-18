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
package org.bladecoder.engine.desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Properties;

import org.bladecoder.engine.BladeEngine;
import org.bladecoder.engine.util.Config;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main extends BladeEngine {

	private boolean fullscreen = true;
	private LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

	Main() {
		Properties p = new Properties();
		
		try {
			InputStream s = Main.class.getResourceAsStream(Config.PROPERTIES_FILENAME);
			if(s!=null)
				p.load(s);
		} catch (IOException e) {
		}
		
		cfg.title = p.getProperty(Config.TITLE_PROP, "Blade Engine Adventure");
//		cfg.useGL30 = true;

		// cfg.width = World.getInstance().getWidth();
		// cfg.height = World.getInstance().getHeight();

		cfg.width = 1920 / 2;
		cfg.height = 1080 / 2;

		cfg.resizable = true;
		cfg.samples = 2;
	}

	public void run() {
		if(Main.class.getResource("/icons/icon128.png")!=null)
			cfg.addIcon("icons/icon128.png", FileType.Internal);
		
		if(Main.class.getResource("/icons/icon32.png")!=null)
			cfg.addIcon("icons/icon32.png", FileType.Internal);
		
		if(Main.class.getResource("/icons/icon16.png")!=null)
			cfg.addIcon("icons/icon16.png", FileType.Internal);		
		
		new LwjglApplication(this, cfg);
	}

	public void parseParams(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String s = args[i];
			if (s.equals("-t")) {
				if (i + 1 < args.length) {
					i++;
					setTestMode(args[i]);
				}
			} else if (s.equals("-p")) {
				if (i + 1 < args.length) {
					i++;
					setPlayMode(args[i]);
				}
			} else if (s.equals("-chapter")) {
				if (i + 1 < args.length) {
					i++;
					setChapter(args[i]);
				}							
			} else if (s.equals("-f")) {
				fullscreen = true;

				//cfg.fullscreen = true;
			} else if (s.equals("-d")) {
				setDebugMode();
			} else if (s.equals("-r")) {
				setRestart();				
			} else if (s.equals("-res")) {
				if (i + 1 < args.length) {
					i++;
					forceResolution(args[i]);
				}					
			} else if (s.equals("-w")) {
				fullscreen = false;
			} else if (s.equals("-l")) {
				if (i + 1 < args.length) {
					i++;
					loadGameState(args[i]);
				}
			} else if (s.equals("-adv-dir")) {
				if (i + 1 < args.length) {
					i++;
					setAdvDir(args[i]);
				}
			} else {
				usage();
			}
		}
	}
	
	private void setAdvDir(String dir) {
		if(!new File(dir).exists()) {
			System.out.println("Adventure folder does NOT exists");
			System.exit(0);
		}
		
		
		try {
			DinamicClassPath.addFile(dir + "/bin");
			DinamicClassPath.addFile(dir + "/assets");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void usage() {
		System.out.println(
				"Usage:\n" +
				"-chapter chapter\tLoads the selected chapter\n" +
			    "-t scene_name\tStart test mode for the scene\n" +
			    "-p record_name\tPlay previusly recorded games\n" +
			    "-f\tSet fullscreen mode\n" +
			    "-w\tSet windowed mode\n" +
			    "-d\tShow debug messages\n" +
			    "-res width\tForce the resolution width\n" +
			    "-l game_state\tLoad the previusly saved game state\n" + 
			    "-r\tRun the game from the begining\n" +
			    "-adv-dir dir\tLoad the adventure in <dir>\n"
				);
		
		System.exit(0);
	}

	@Override
	public void create() {
		// Gdx.input.setCursorCatched(false);
		if (fullscreen)
			Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode());
		
		hideCursor();
		
		super.create();
	}

	private void hideCursor() {
		Cursor emptyCursor;

		int min = org.lwjgl.input.Cursor.getMinCursorSize();
		IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
		try {
			emptyCursor = new org.lwjgl.input.Cursor(min, min, min / 2,
					min / 2, 1, tmp, null);

			Mouse.setNativeCursor(emptyCursor);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		Main game = new Main();
		game.parseParams(args);
		game.run();
	}
}
