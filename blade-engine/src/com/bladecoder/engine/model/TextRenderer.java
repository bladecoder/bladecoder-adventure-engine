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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
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
	private int textAlign = Align.left;
	private int orgAlign = Align.bottom;
	
	private final Color color = new Color(Color.WHITE);
	
	// Translated Text shown in the editor. When editing the text, the .properties with the translation is not ready.
	private transient String editorTranslatedText;

	public TextRenderer() {

	}
		
	@Override
	public int getOrgAlign() {
		return orgAlign;
	}

	@Override
	public void setOrgAlign(int align) {
		orgAlign = align;
	}

	@Override
	public void update(float delta) {
	}

	private static final Matrix4 tmp = new Matrix4();

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint) {
		
		float dx = getAlignDx(getWidth(), orgAlign);
		float dy = getAlignDy(getHeight(), orgAlign);

		if (font != null && text != null) {
			
			if(tint != null && !tint.equals(color)) {
				color.set(tint);
				
				String tt = text;
				
				if (tt.charAt(0) == I18N.PREFIX)
					tt = I18N.getString(tt.substring(1));
				
				if(editorTranslatedText != null)
					tt = editorTranslatedText;
				
				layout.setText(font, tt, color, 0, textAlign, false);
			}

			Matrix4 tm = batch.getTransformMatrix();
			tmp.set(tm);

			float originX = dx;
			float originY = layout.height + dy;
			
			if(textAlign == Align.right) 
				originX += getWidth();
			else if(textAlign == Align.center)
				originX += getWidth() / 2;

			tm.translate(x, y, 0).rotate(0, 0, 1, rotation).scale(scale, scale, 1).translate(originX, originY, 0);

			batch.setTransformMatrix(tm);

			font.draw(batch, layout, 0, 0);

			batch.setTransformMatrix(tmp);
		} else {
			RectangleRenderer.draw(batch, x + dx * scale , y + dy * scale, getWidth() * scale, getHeight() * scale, Color.RED);
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
		
		float dx =  getAlignDx(getWidth(), orgAlign);
		float dy =  getAlignDy(getHeight(), orgAlign);

		float[] verts = bbox.getVertices();

		verts[0] = dx;
		verts[1] = dy;
		
		verts[2] = dx;
		verts[3] = getHeight() + dy;
		
		verts[4] = getWidth() + dx;
		verts[5] = getHeight() + dy;
		
		verts[6] = getWidth() + dx;
		verts[7] = dy;
		bbox.dirty();
	}

	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
		this.editorTranslatedText = text;
	}

	public void setText(String text, String translatedText) {
		this.text = text;
		this.editorTranslatedText = translatedText;
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
	
	public int getAlign() {
		return textAlign;
	}

	public void setAlign(int align) {
		this.textAlign = align;
	}
	
	public static float getAlignDx(float width, int align) {
		if((align & Align.left) != 0)
			return 0;
		else if((align & Align.right) != 0)
			return -width;
		else if((align & Align.center) != 0)
			return -width / 2.0f;
		
		return -width / 2.0f;
	}
	
	public static float getAlignDy(float height, int align) {
		if((align & Align.bottom) != 0)
			return 0;
		else if((align & Align.top) != 0)
			return -height;
		else if((align & Align.center) != 0)
			return -height / 2.0f;
		
		return 0;
	}

	@Override
	public void loadAssets() {
		FreeTypeFontLoaderParameter params = new FreeTypeFontLoaderParameter();
		
		float scale = EngineAssetManager.getInstance().getScale();

		params.fontFileName = EngineAssetManager.FONT_DIR + fontName + EngineAssetManager.FONT_EXT;
		params.fontParameters.size = (int)(fontSize * scale);
		params.fontParameters.borderWidth = (int)(borderWidth * scale);
		params.fontParameters.borderColor = borderColor;
		params.fontParameters.borderStraight = borderStraight;
		params.fontParameters.shadowOffsetX = (int)(shadowOffsetX * scale);
		params.fontParameters.shadowOffsetY = (int)(shadowOffsetY * scale);
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

		String tt = text;
		
		if (tt.charAt(0) == I18N.PREFIX)
			tt = I18N.getString(tt.substring(1));
		
		if(editorTranslatedText != null)
			tt = editorTranslatedText;
		
		layout.setText(font, tt, color, 0, textAlign, false);

		computeBbox();
	}

	@Override
	public void dispose() {
		if (EngineAssetManager.getInstance().isLoaded(fontName + getFontSize() + ".ttf"))
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
			json.writeValue("align", textAlign);
			json.writeValue("orgAlign", orgAlign);
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
			textAlign = json.readValue("align", int.class, Align.left, jsonData);
			orgAlign = json.readValue("orgAlign", int.class, Align.bottom, jsonData);
		} else {

		}
	}
}