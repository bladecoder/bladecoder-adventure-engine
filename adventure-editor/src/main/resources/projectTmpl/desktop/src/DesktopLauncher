package com.bladecoder.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Properties;

import com.bladecoder.engine.BladeEngine;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.ui.SceneScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopLauncher extends BladeEngine {

	private boolean fullscreen = true;
	private LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
	
	private float speed = 1.0f;

	DesktopLauncher() {
		Properties p = new Properties();
		
		try {
			InputStream s = DesktopLauncher.class.getResourceAsStream("/" + Config.PROPERTIES_FILENAME);
			if(s!=null)
				p.load(s);
		} catch (IOException e) {
			System.out.println("Could not load properties file.");
		}
		
		cfg.title = p.getProperty(Config.TITLE_PROP, "Blade Engine Adventure") + " " + p.getProperty(Config.VERSION_PROP, "");
//		cfg.useGL30 = true;

		cfg.width = 1920 / 2;
		cfg.height = 1080 / 2;

		cfg.resizable = true;
		//cfg.samples = 2;
		cfg.vSyncEnabled = true;
	}

	public void run() {
		if(DesktopLauncher.class.getResource("/icons/icon128.png")!=null)
			cfg.addIcon("icons/icon128.png", FileType.Internal);
		
		if(DesktopLauncher.class.getResource("/icons/icon32.png")!=null)
			cfg.addIcon("icons/icon32.png", FileType.Internal);
		
		if(DesktopLauncher.class.getResource("/icons/icon16.png")!=null)
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
			} else if (s.equals("-s")) {
				if (i + 1 < args.length) {
					i++;
					speed = Float.parseFloat(args[i]);
				}				
			} else if (s.equals("-chapter")) {
				if (i + 1 < args.length) {
					i++;
					setChapter(args[i]);
				}							
			} else if (s.equals("-f")) {
				fullscreen = true;
			} else if (s.equals("-d")) {
				setDebugMode();
			} else if (s.equals("-r")) {
				setRestart();				
			} else if (s.equals("-res")) {
				if (i + 1 < args.length) {
					i++;
					forceResolution(args[i]);
				}
			} else if (s.equals("-aspect")) {
				if (i + 1 < args.length) {
					i++;
					String aspect = args[i];
					
					if(aspect.equals("16:9")) {
						cfg.height = cfg.width * 9/16;
					} else if(aspect.equals("4:3")) {
						cfg.height = cfg.width * 3/4;
					} else if(aspect.equals("16:10") || 
							aspect.equals("8:5") ) {
						cfg.height = cfg.width * 10/16;
					}
				}	
			} else if (s.equals("-w")) {
				fullscreen = false;
			} else if (s.equals("-l")) {
				if (i + 1 < args.length) {
					i++;
					loadGameState(args[i]);
				}
			} else if (s.equals("-h")) {
				usage();
			} else {
				if(i == 0 && !s.startsWith("-")) continue; // When embeded JRE the 0 parameter is the app name
				System.out.println("Unrecognized parameter: " + s);
				usage();
			}
		}
	}
	
	public void usage() {
		System.out.println("Usage:\n" 
				+ "-chapter chapter\tLoads the selected chapter\n"
				+ "-t scene_name\tStart test mode for the scene\n" 
				+ "-p record_name\tPlay previusly recorded games\n"
				+ "-f\tSet fullscreen mode\n"
				+ "-w\tSet windowed mode\n" 
				+ "-d\tShow debug messages\n"
				+ "-res width\tForce the resolution width\n" 
				+ "-l game_state\tLoad the previusly saved game state\n"
				+ "-r\tRun the game from the begining\n"
				+ "-s speed\tSets the game speed\n"
				+ "-aspect aspect_ratio\tSets the specified screen aspect (16:9, 4:3, 16:10)\n");

		System.exit(0);
	}

	@Override
	public void create() {
		// Gdx.input.setCursorCatched(false);
		if (fullscreen)
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		
		hideCursor();
		
		super.create();
		
		if(getUI().getScreen(Screens.SCENE_SCREEN) instanceof SceneScreen)
			((SceneScreen) getUI().getScreen(Screens.SCENE_SCREEN)).setSpeed(speed);
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
		DesktopLauncher game = new DesktopLauncher();
		game.parseParams(args);
		game.run();
	}
}
