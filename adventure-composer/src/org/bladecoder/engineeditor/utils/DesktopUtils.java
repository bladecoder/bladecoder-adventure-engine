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
package org.bladecoder.engineeditor.utils;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import javax.swing.JOptionPane;

public class DesktopUtils {

	/**
	 * Opens the given website in the default browser.
	 */
	public static void browse(Component parent, String uri) {
		boolean error = false;

		if (Desktop.isDesktopSupported()
				&& Desktop.getDesktop().isSupported(Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI(uri));
			} catch (URISyntaxException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				error = true;
			}
		} else {
			error = true;
		}

		if (error) {
			String msg = "Impossible to open the default browser from the application";
			JOptionPane.showMessageDialog(parent, msg);
		}
	}

	public static File createTempDirectory() throws IOException {
		final File temp;

//		temp = File.createTemp("advtmp", Long.toString(System.nanoTime()));
		temp = Files.createTempDirectory("advtmp").toFile();

		return temp;
	}

	public static void removeDir(String dir) throws IOException {
		File f = new File(dir);

		File files[] = f.listFiles();

		if (files != null)
			for (File f2 : files)
				Files.delete(f2.toPath());

		Files.deleteIfExists(f.toPath());
	}

}
