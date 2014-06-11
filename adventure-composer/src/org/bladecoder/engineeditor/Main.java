package org.bladecoder.engineeditor;

import java.io.File;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main extends LwjglApplication {

	public static void main(final String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = "Adventure Composer";
		cfg.width = 1920 / 2;
		cfg.height = 1080 / 2;

		cfg.resizable = true;
		// cfg.samples = 2;
		// cfg.useGL30 = true;

		if (Main.class.getResource("/res/images/ic_app64.png") != null)
			cfg.addIcon("res/images/ic_app64.png", FileType.Internal);

		if (Main.class.getResource("/res/images/ic_app32.png") != null)
			cfg.addIcon("res/images/ic_app32.png", FileType.Internal);

		if (Main.class.getResource("/res/images/ic_app16.png") != null)
			cfg.addIcon("res/images/ic_app16.png", FileType.Internal);

		parseArgs(args);

		new Main(new Editor(), cfg);
	}

	private static void parseArgs(String[] args) {
		for (int i = 1; i < args.length; i++) {
			if (args[i - 1].equals("-f")) {
				try {
					File file = new File(args[i]).getCanonicalFile();
					Ctx.project.loadProject(file);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public Main(Editor editor, LwjglApplicationConfiguration cfg) {
		super(editor,cfg);
	}
	
	@Override
	public void exit() {
		((Editor) listener).exit();
	}
	
	public void exitSaved() {
		super.exit();
	}
}
