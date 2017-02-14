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
package com.bladecoder.engine.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class TextRenderer implements ActorRenderer {

	private final static float DEFAULT_DIM = 200;

	private Polygon bbox;

	private BitmapFont font;
	private final GlyphLayout layout = new GlyphLayout();

	private int fontSize = 10;
	private String text;
	private String fontName;
	private int borderWidth = 0;
	private Color borderColor = Color.BLACK;
	private boolean borderStraight = false;
	private int shadowOffsetX = 0;
	private int shadowOffsetY = 0;
	private Color shadowColor = Color.BLACK;

	public TextRenderer() {

	}

	@Override
	public void update(float delta) {
	}

	private static final Matrix4 tmp = new Matrix4();

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint) {

		if (font != null && text != null) {

			Matrix4 tm = batch.getTransformMatrix();
			tmp.set(tm);

			float originX = -getWidth() / 2;
			float originY = layout.height;

			tm.translate(x, y, 0).rotate(0, 0, 1, rotation).scale(scale, scale, 1).translate(originX, originY, 0);

			batch.setTransformMatrix(tm);

			if (tint != null)
				batch.setColor(tint);

			font.draw(batch, layout, 0, 0);

			if (tint != null)
				batch.setColor(Color.WHITE);

			batch.setTransformMatrix(tmp);
		} else {
			x = x - getWidth() / 2 * scale;
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight() * scale, Color.RED);
		}
	}

	@Override
	public float getWidth() {
		if (font == null)
			return DEFAULT_DIM;

		return layout.width;
	}

	@Override
	public float getHeight() {
		if (font == null)
			return DEFAULT_DIM;

		return layout.height;
	}

	@Override
	public void updateBboxFromRenderer(Polygon bbox) {
		this.bbox = bbox;

		computeBbox();
	}

	private void computeBbox() {
		if (bbox == null)
			return;

		if (bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}

		float[] verts = bbox.getVertices();

		verts[0] = -getWidth() / 2;
		verts[1] = 0f;
		verts[2] = -getWidth() / 2;
		verts[3] = getHeight();
		verts[4] = getWidth() / 2;
		verts[5] = getHeight();
		verts[6] = getWidth() / 2;
		verts[7] = 0f;
		bbox.dirty();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public boolean isBorderStraight() {
		return borderStraight;
	}

	public void setBorderStraight(boolean borderStraight) {
		this.borderStraight = borderStraight;
	}

	public int getShadowOffsetX() {
		return shadowOffsetX;
	}

	public void setShadowOffsetX(int shadowOffsetX) {
		this.shadowOffsetX = shadowOffsetX;
	}

	public int getShadowOffsetY() {
		return shadowOffsetY;
	}

	public void setShadowOffsetY(int shadowOffsetY) {
		this.shadowOffsetY = shadowOffsetY;
	}

	public Color getShadowColor() {
		return shadowColor;
	}

	public void setShadowColor(Color shadowColor) {
		this.shadowColor = shadowColor;
	}

	@Override
	public void loadAssets() {
		FreeTypeFontLoaderParameter params = new FreeTypeFontLoaderParameter();

		params.fontFileName = EngineAssetManager.FONT_DIR + fontName + EngineAssetManager.FONT_EXT;
		params.fontParameters.size = fontSize;
		params.fontParameters.borderWidth = borderWidth;
		params.fontParameters.borderColor = borderColor;
		params.fontParameters.borderStraight = borderStraight;
		params.fontParameters.shadowOffsetX = shadowOffsetX;
		params.fontParameters.shadowOffsetY = shadowOffsetY;
		params.fontParameters.shadowColor = shadowColor;
		params.fontParameters.characters = "";
		params.fontParameters.incremental = true;
		params.fontParameters.magFilter = TextureFilter.Linear;
		params.fontParameters.minFilter = TextureFilter.Linear;

		EngineAssetManager.getInstance().load(fontName + getFontSize() + ".ttf", BitmapFont.class, params);
	}

	@Override
	public void retrieveAssets() {

		if (!EngineAssetManager.getInstance().isLoaded(fontName + getFontSize() + ".ttf")) {
			loadAssets();
			EngineAssetManager.getInstance().finishLoading();
		}

		font = EngineAssetManager.getInstance().get(fontName + getFontSize() + ".ttf", BitmapFont.class);

		layout.setText(font, text);

		computeBbox();
	}

	@Override
	public void dispose() {
		EngineAssetManager.getInstance().unload(fontName + getFontSize() + ".ttf");
		font = null;
	}

	@Override
	public void write(Json json) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			json.writeValue("text", text);
			json.writeValue("fontName", fontName);
			json.writeValue("fontSize", fontSize);
			json.writeValue("borderWidth", borderWidth);
			json.writeValue("borderColor", borderColor);
			json.writeValue("borderStraight", borderStraight);
			json.writeValue("shadowOffsetX", shadowOffsetX);
			json.writeValue("shadowOffsetY", shadowOffsetY);
			json.writeValue("shadowColor", shadowColor);
		} else {

		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			text = json.readValue("text", String.class, jsonData);
			fontName = json.readValue("fontName", String.class, jsonData);
			fontSize = json.readValue("fontSize", int.class, jsonData);
			borderWidth = json.readValue("borderWidth", int.class, jsonData);
			borderColor = json.readValue("borderColor", Color.class, jsonData);
			borderStraight = json.readValue("borderStraight", boolean.class, jsonData);
			shadowOffsetX = json.readValue("shadowOffsetX", int.class, jsonData);
			shadowOffsetY = json.readValue("shadowOffsetY", int.class, jsonData);
			shadowColor = json.readValue("shadowColor", Color.class, jsonData);
		} else {

		}
	}
}