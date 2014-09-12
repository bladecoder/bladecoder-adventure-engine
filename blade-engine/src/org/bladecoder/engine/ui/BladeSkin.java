package org.bladecoder.engine.ui;

import org.bladecoder.engine.util.DPIUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.ReadOnlySerializer;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

/**
 * Custom Skin class to add support for TTF fonts
 * 
 * @author rgarcia
 */
public class BladeSkin extends Skin {

	public BladeSkin(FileHandle skinFile, TextureAtlas atlas) {
		super(skinFile, atlas);
	}

	/**
	 * Override BitmapFont.class serializer to support TTF fonts
	 * 
	 * Also add the size parameter to support bitmaps font size in pt
	 */
	@Override
	protected Json getJsonLoader(final FileHandle skinFile) {
		Json json = super.getJsonLoader(skinFile);

		final Skin skin = this;

		json.setSerializer(BitmapFont.class, new ReadOnlySerializer<BitmapFont>() {
			public BitmapFont read(Json json, JsonValue jsonData, @SuppressWarnings("rawtypes") Class type) {
				String path = json.readValue("file", String.class, jsonData);
				int scaledSize = json.readValue("scaledSize", int.class, -1, jsonData);
				Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
				int size = json.readValue("size", int.class, -1, jsonData);

				FileHandle fontFile = skinFile.parent().child(path);
				if (!fontFile.exists())
					fontFile = Gdx.files.internal(path);
				if (!fontFile.exists())
					throw new SerializationException("Font file not found: " + fontFile);
				
				BitmapFont font;

				if (fontFile.extension().equalsIgnoreCase("ttf")) {
					
					if (size == -1) 
						throw new SerializationException("'size' parameter mandatory for .ttf fonts");
					
					FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
					FreeTypeFontParameter parameter = new FreeTypeFontParameter();
					parameter.size = size;
					font = generator.generateFont(parameter); 
					generator.dispose(); 
				} else {

					// Use a region with the same name as the font, else use a
					// PNG file in the same directory as the FNT file.
					String regionName = fontFile.nameWithoutExtension();
					try {
						TextureRegion region = skin.optional(regionName, TextureRegion.class);
						if (region != null)
							font = new BitmapFont(fontFile, region, flip);
						else {
							FileHandle imageFile = fontFile.parent().child(regionName + ".png");
							if (imageFile.exists())
								font = new BitmapFont(fontFile, imageFile, flip);
							else
								font = new BitmapFont(fontFile, flip);
						}
						// Scaled size is the desired cap height to scale the
						// font to.
						if (scaledSize != -1)
							font.setScale(scaledSize / font.getCapHeight());
						else if(size != -1) // TODO set size in points (dpi independent)
							font.setScale(DPIUtils.ptToPixels(size) / font.getCapHeight());
					} catch (RuntimeException ex) {
						throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
					}
				}
				
				return font;
			}
		});

		return json;
	}
}
