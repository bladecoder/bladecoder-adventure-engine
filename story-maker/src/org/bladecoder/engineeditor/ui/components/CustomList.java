package org.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public abstract class CustomList<T> extends List<T> {
	
	private Rectangle cullingArea;

	public CustomList(Skin skin) {
		super(skin);
	}
	
	protected abstract String getCellTitle(T item);
	
	protected abstract String getCellSubTitle(T item);
	
	protected abstract Texture getCellTexture(T item);
	
	
	@Override
	public void draw (Batch batch, float parentAlpha) {
		validate();

		BitmapFont font = getStyle().font;
		Drawable selectedDrawable = getStyle().selection;
		Color fontColorSelected = getStyle().fontColorSelected;
		Color fontColorUnselected = getStyle().fontColorUnselected;

		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

		float x = getX(), y = getY(), width = getWidth(), height = getHeight();
		float itemY = height;

		Drawable background = getStyle().background;
		if (background != null) {
			background.draw(batch, x, y, width, height);
			float leftWidth = background.getLeftWidth();
			x += leftWidth;
			itemY -= background.getTopHeight();
			width -= leftWidth + background.getRightWidth();
		}

		font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha);
		for (int i = 0; i < getItems().size; i++) {
			if (cullingArea == null || (itemY - getItemHeight() <= cullingArea.y + cullingArea.height && itemY >= cullingArea.y)) {
				T item = getItems().get(i);
				boolean selected = getSelection().contains(item);
				if (selected) {
					selectedDrawable.draw(batch, x, y + itemY - getItemHeight(), width, getItemHeight());
					font.setColor(fontColorSelected.r, fontColorSelected.g, fontColorSelected.b, fontColorSelected.a * parentAlpha);
				}
//				font.draw(batch, item.toString(), x + textOffsetX, y + itemY - textOffsetY);
				font.draw(batch, item.toString(), x, y + itemY);
				if (selected) {
					font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a
						* parentAlpha);
				}
			} else if (itemY < cullingArea.y) {
				break;
			}
			itemY -= getItemHeight();
		}
	}
	
	@Override
	public void setCullingArea (Rectangle cullingArea) {
		this.cullingArea = cullingArea;
	}
}

