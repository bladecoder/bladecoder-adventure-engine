package org.bladecoder.engineeditor.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.SwingUtilities;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.Theme;
import org.bladecoder.engineeditor.utils.EditorLogger;

public class Main {
    public static void main(final String[] args) {
    	
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
//				try {
//					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//				} catch (Exception ex) {
//					ex.printStackTrace();
//				}
				
				EditorLogger.setDebug();
				
				Theme.setTheme();
				
				MainWindow mw = Ctx.window;

				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				mw.setSize(
					Math.min(1150, screenSize.width - 100),
					Math.min(800, screenSize.height - 100)
				);

				mw.setLocationRelativeTo(null);
				mw.setVisible(true);

				parseArgs(args);
			}
		});
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
