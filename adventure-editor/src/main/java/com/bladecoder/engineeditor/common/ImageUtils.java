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
package com.bladecoder.engineeditor.common;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.bladecoder.engine.model.World;

public class ImageUtils {

	public static ImageIcon getImageIcon(URL u, int w) throws IOException {
		BufferedImage img = null;
		BufferedImage tmp = ImageIO.read(u);
		ImageIcon icon = null;

		if (tmp != null) {
			float h = (float) tmp.getHeight() * (float) w / (float) tmp.getWidth();

			img = scaleImage(w, (int) h, tmp, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			icon = new ImageIcon((Image) img);
		}

		return icon;
	}

	public static BufferedImage scaleImage(int w, int h, BufferedImage img, Object interpolation) {
		BufferedImage bi;

		bi = new BufferedImage(w, h, img.getType());
		Graphics2D g2d = (Graphics2D) bi.createGraphics();
		// g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		// g2d.addRenderingHints(new
		// RenderingHints(RenderingHints.KEY_RENDERING,
		// RenderingHints.VALUE_RENDER_QUALITY));

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
		g2d.drawImage(img, 0, 0, w, h, null);
		g2d.dispose();
		return bi;
	}

	public static void scaleImageFile(File org, File dest, float scale) throws IOException {
		BufferedImage destImg = null;
		BufferedImage orgImg = ImageIO.read(org);

		if (orgImg != null) {
			Object interpolation;

			if (orgImg.getWidth() < 20) {
				interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
			} else {
				interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
			}

			destImg = scaleImage(Math.max(1, (int) (orgImg.getWidth() * scale)),
					Math.max(1, (int) (orgImg.getHeight() * scale)), orgImg, interpolation);
			ImageIO.write(destImg, org.getName().substring(org.getName().lastIndexOf('.') + 1), dest);
		}
	}

	/**
	 * Scale all images in a folder.
	 * 
	 * WARNING: .etc1 file format is not supported.
	 * 
	 * @param orgDir
	 * @param destDir
	 * @param scale
	 * @throws IOException
	 */
	public static void scaleDirFiles(File orgDir, File destDir, float scale) throws IOException {
		File[] files = orgDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"))
					return true;

				return false;
			}
		});

		if (files != null) {
			for (File f : files) {
				if (f.getName().endsWith(".9.png")) { // 9 patches doesn't scale
					Files.copy(f.toPath(), new File(destDir, f.getName()).toPath());
				} else {
					ImageUtils.scaleImageFile(f, new File(destDir, f.getName()), scale);
				}
			}
		}
	}

	public static void scaleAtlas(File orgAtlas, File destDir, float scale) throws IOException {
		CustomTextureUnpacker unpacker = new CustomTextureUnpacker();
		File outputDir = DesktopUtils.createTempDirectory();

		String atlasParentPath = orgAtlas.getParentFile().getAbsolutePath();

		TextureAtlasData atlas = new TextureAtlasData(new FileHandle(orgAtlas), new FileHandle(atlasParentPath), false);
		unpacker.splitAtlas(atlas, outputDir.getAbsolutePath());

		createAtlas(outputDir.getAbsolutePath(), destDir.getAbsolutePath(), orgAtlas.getName(), scale,
				TextureFilter.Linear, TextureFilter.Linear);

		DesktopUtils.removeDir(outputDir.getAbsolutePath());
	}

	public static void scaleDirAtlases(File orgDir, File destDir, float scale) throws IOException {
		File[] files = orgDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith(".atlas"))
					return true;

				return false;
			}
		});

		for (File f : files) {
			ImageUtils.scaleAtlas(f, destDir, scale);
		}
	}

	public static void createAtlas(String inDir, String outdir, String name, float scale, TextureFilter filterMin,
			TextureFilter filterMag) throws IOException {
		Settings settings = new Settings();

		settings.pot = false;
		settings.paddingX = 2;
		settings.paddingY = 2;
		settings.duplicatePadding = true;
		settings.edgePadding = true;
		settings.rotation = false;
		settings.minWidth = 16;
		settings.minWidth = 16;
		settings.stripWhitespaceX = true;
		settings.stripWhitespaceY = true;
		settings.alphaThreshold = 0;

		settings.filterMin = filterMin;
		settings.filterMag = filterMag;
		settings.wrapX = Texture.TextureWrap.ClampToEdge;
		settings.wrapY = Texture.TextureWrap.ClampToEdge;
		settings.format = Format.RGBA8888;
		settings.alias = true;
		settings.outputFormat = "png";
		settings.jpegQuality = 0.9f;
		settings.ignoreBlankImages = true;
		settings.fast = false;
		settings.debug = false;

		int wWidth = World.getInstance().getWidth();

		settings.maxWidth = MathUtils.nextPowerOfTwo((int) (wWidth * scale * 2f));
		settings.maxHeight = MathUtils.nextPowerOfTwo((int) (wWidth * scale * 2f));

		EditorLogger.debug("ATLAS MAXWIDTH: " + settings.maxWidth);

		File inTmpDir = new File(inDir);

		// Resize images to create atlas for diferent resolutions
		if (scale != 1.0f) {
			inTmpDir = DesktopUtils.createTempDirectory();

			ImageUtils.scaleDirFiles(new File(inDir), inTmpDir, scale);
		}

		TexturePacker.process(settings, inTmpDir.getAbsolutePath(), outdir,
				name.endsWith(".atlas") ? name : name + ".atlas");

		if (scale != 1.0f) {
			DesktopUtils.removeDir(inTmpDir.getAbsolutePath());
		}
	}
}
