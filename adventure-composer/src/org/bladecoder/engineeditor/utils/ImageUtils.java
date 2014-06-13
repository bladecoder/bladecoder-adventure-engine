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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ImageUtils {

	public static ImageIcon getImageIcon(URL u, int w) throws IOException {
		BufferedImage img = null;
		BufferedImage tmp = ImageIO.read(u);
		ImageIcon icon = null;

		if (tmp != null) {
			float h = (float) tmp.getHeight() * (float) w / (float) tmp.getWidth();

			img = scaleImage(w, (int) h, tmp);
			icon = new ImageIcon((Image) img);
		}

		return icon;
	}

	public static BufferedImage scaleImage(int w, int h, BufferedImage img) {
		BufferedImage bi;
		bi = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
		Graphics2D g2d = (Graphics2D) bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY));
		g2d.drawImage(img, 0, 0, w, h, null);
		g2d.dispose();
		return bi;
	}
	
	public static void scaleImageFile(File org, File dest, float scale) throws IOException {
		BufferedImage destImg = null;
		BufferedImage orgImg = ImageIO.read(org);

		if (orgImg != null) {
			destImg = scaleImage((int)(orgImg.getWidth() * scale), (int)(orgImg.getHeight() * scale), orgImg);
			ImageIO.write(destImg, org.getName().substring(org.getName().lastIndexOf('.') + 1), dest);
		}	
	}
	
	public static void scaleDirFiles(File orgDir, File destDir, float scale) throws IOException {
		File[] files = orgDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if(name.toLowerCase().endsWith("png")||
						name.toLowerCase().endsWith("jpg")||
						name.toLowerCase().endsWith("etc1")) return true;
				
				return false;
			}
		});
		
		for(File f:files) {					
			ImageUtils.scaleImageFile(f, new File(destDir, f.getName()), scale);
		}		
	}
}
