package com.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.ReadOnlySerializer;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.FileUtils;

/**
 * Custom Skin class to add TTF font support
 * 
 * @author rgarcia
 */
public class BladeSkin extends Skin {

	public BladeSkin(FileHandle skinFile) {
		super(skinFile);
	}

	public BladeSkin(FileHandle skinFile, TextureAtlas atlas) {
		super(skinFile, atlas);
	}

	public BladeSkin(TextureAtlas atlas) {
		super(atlas);
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

		json.setSerializer(Skin.class, new ReadOnlySerializer<Skin>() {
			@Override
			public Skin read(Json json, JsonValue typeToValueMap, @SuppressWarnings("rawtypes") Class ignored) {
				for (JsonValue valueMap = typeToValueMap.child; valueMap != null; valueMap = valueMap.next) {
					try {
						Class<?> type = json.getClass(valueMap.name());
						if (type == null)
							type = ClassReflection.forName(valueMap.name());
						readNamedObjects(json, type, valueMap);
					} catch (ReflectionException ex) {
						throw new SerializationException(ex);
					}
				}
				return skin;
			}

			private void readNamedObjects(Json json, Class<?> type, JsonValue valueMap) {
				Class<?> addType = type == TintedDrawable.class ? Drawable.class : type;
				for (JsonValue valueEntry = valueMap.child; valueEntry != null; valueEntry = valueEntry.next) {
					Object object = json.readValue(type, valueEntry);
					if (object == null)
						continue;
					try {
						add(valueEntry.name, object, addType);
						if (addType != Drawable.class && ClassReflection.isAssignableFrom(Drawable.class, addType))
							add(valueEntry.name, object, Drawable.class);
					} catch (Exception ex) {
						throw new SerializationException(
								"Error reading " + ClassReflection.getSimpleName(type) + ": " + valueEntry.name, ex);
					}
				}
			}
		});

		json.setSerializer(BitmapFont.class, new ReadOnlySerializer<BitmapFont>() {
			@Override
			public BitmapFont read(Json json, JsonValue jsonData, @SuppressWarnings("rawtypes") Class type) {
				String path = json.readValue("file", String.class, jsonData);
				int scaledSize = json.readValue("scaledSize", int.class, -1, jsonData);
				Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
				int size = json.readValue("size", int.class, -1, jsonData);

				FileHandle fontFile = skinFile.parent().child(path);
				if (!FileUtils.exists(fontFile))
					fontFile = Gdx.files.internal(path);

				if (!FileUtils.exists(fontFile))
					throw new SerializationException("Font file not found: " + fontFile);

				BitmapFont font;

				if (fontFile.extension().equalsIgnoreCase("ttf")) {

					if (size == -1)
						throw new SerializationException("'size' mandatory parameter for .ttf fonts");

					long initTime = System.currentTimeMillis();

					FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
					FreeTypeFontParameter parameter = new FreeTypeFontParameter();
					parameter.size = (int) (DPIUtils.dpToPixels(size) * DPIUtils.getSizeMultiplier());
					parameter.incremental = json.readValue("incremental", boolean.class, true, jsonData);
					parameter.borderWidth = json.readValue("borderWidth", int.class, 0, jsonData);
					parameter.borderColor = json.readValue("borderColor", Color.class, Color.BLACK, jsonData);
					parameter.borderStraight = json.readValue("borderStraight", boolean.class, false, jsonData);
					parameter.shadowOffsetX = json.readValue("shadowOffsetX", int.class, 0, jsonData);
					parameter.shadowOffsetY = json.readValue("shadowOffsetY", int.class, 0, jsonData);
					parameter.shadowColor = json.readValue("shadowColor", Color.class, Color.BLACK, jsonData);
					if (parameter.incremental)
						parameter.characters = "";

					// parameter.hinting = Hinting.Medium;

					// parameter.mono = false;

					font = generator.generateFont(parameter);

					EngineLogger.debug(path + " TIME (ms): " + (System.currentTimeMillis() - initTime));

					// TODO Dispose all generators.

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
							if (FileUtils.exists(imageFile))
								font = new BitmapFont(fontFile, imageFile, flip);
							else
								font = new BitmapFont(fontFile, flip);
						}
						// Scaled size is the desired cap height to scale the
						// font to.
						if (scaledSize != -1)
							font.getData().setScale(scaledSize / font.getCapHeight());
						else if (size != -1) // TODO set size in points (dpi
												// independent)
							font.getData().setScale(
									(DPIUtils.dpToPixels(size) * DPIUtils.getSizeMultiplier()) / font.getCapHeight());
					} catch (RuntimeException ex) {
						throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
					}
				}

				font.getData().markupEnabled = true;

				return font;
			}
		});

		json.setSerializer(AnimationDrawable.class, new ReadOnlySerializer<AnimationDrawable>() {
			@Override
			public AnimationDrawable read(Json json, JsonValue jsonData, @SuppressWarnings("rawtypes") Class type) {
				String name = json.readValue("name", String.class, jsonData);
				float frameDuration = json.readValue("frame_duration", Float.class, jsonData);

				Array<AtlasRegion> regions = getAtlas().findRegions(name);

				Animation<AtlasRegion> a = new Animation<AtlasRegion>(frameDuration, regions, PlayMode.LOOP);
				AnimationDrawable drawable = new AnimationDrawable(a);

				if (drawable instanceof BaseDrawable) {
					BaseDrawable named = drawable;
					named.setName(jsonData.name + " (" + name + ", " + frameDuration + ")");
				}

				return drawable;
			}
		});

		json.addClassTag("AnimationDrawable", AnimationDrawable.class);

		return json;
	}

	public void addStyleTag(Class<?> tag) {
		getJsonClassTags().put(tag.getSimpleName(), tag);
	}

	@Override
	public Drawable newDrawable(Drawable drawable) {
		if (drawable instanceof AnimationDrawable)
			return new AnimationDrawable((AnimationDrawable) drawable);
		return super.newDrawable(drawable);
	}

	@Override
	public Drawable newDrawable(Drawable drawable, Color tint) {
		Drawable newDrawable;
		if (drawable instanceof AnimationDrawable) {
			newDrawable = ((AnimationDrawable) drawable).tint(tint);

			if (newDrawable instanceof BaseDrawable) {
				BaseDrawable named = (BaseDrawable) newDrawable;
				if (drawable instanceof BaseDrawable)
					named.setName(((BaseDrawable) drawable).getName() + " (" + tint + ")");
				else
					named.setName(" (" + tint + ")");
			}

			return newDrawable;
		}

		return super.newDrawable(drawable, tint);
	}
}
