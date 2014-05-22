package org.bladecoder.engineeditor;

import java.io.File;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main {
    public static void main(final String[] args) {
    	LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
    	
    	cfg.title = "Adventure Composer";
		cfg.width = 1920 / 2;
		cfg.height = 1080 / 2;

		cfg.resizable = true;
//		cfg.samples = 2;
//		cfg.useGL30 = true;
		
		if(Main.class.getResource("/res/images/ic_app.png")!=null)
			cfg.addIcon("res/images/ic_app.png", FileType.Internal);
		
//		if(Main.class.getResource("/icons/icon32.png")!=null)
//			cfg.addIcon("icons/icon32.png", FileType.Internal);
//		
//		if(Main.class.getResource("/icons/icon16.png")!=null)
//			cfg.addIcon("icons/icon16.png", FileType.Internal);			
		
		parseArgs(args);
    	
    	new LwjglApplication(new Editor(), cfg);
    }

	private static void parseArgs(String[] args) {
		for (int i=1; i<args.length; i++) {
			if (args[i-1].equals("-f")) {
				try {
					File file = new File(args[i]).getCanonicalFile();
					Ctx.project.loadProject(file);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
