package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engineeditor.ui.components.CustomList.CustomListStyle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class CellRenderer<T> {
	private static final float MARGIN = 10;
	
	private CustomListStyle style;
	private float itemHeight;
	private float textOffsetX, textOffsetY;

	public CellRenderer() {		
	}

	public void draw(Batch batch, float parentAlpha, T item, boolean selected, float x, float y, float width, float height) {
		BitmapFont font = style.font;
		Drawable selectedDrawable = style.selection;
		Color fontColorSelected = style.fontColorSelected;
		Color fontColorUnselected = style.fontColorUnselected;
		
		
		
		if (selected) {
			selectedDrawable.draw(batch, x,
					y - height, width,
					height);
			font.setColor(fontColorSelected.r, fontColorSelected.g,
					fontColorSelected.b, fontColorSelected.a * parentAlpha);
		} else {
			font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha);			
		}
		
		if(hasImage()) {
			TextureRegion r = getCellImage(item);
			
			float ih = r.getRegionHeight();
			float iw = r.getRegionWidth();
					
			if(ih > getItemHeight() - MARGIN) {
				ih = getItemHeight() - MARGIN;
				iw *= ih / r.getRegionHeight();
			}
			
			batch.draw(r, x, y - ih - MARGIN/2, iw, ih);
			x += iw;
		}
		
		
		font.draw(batch, getCellTitle(item), x + textOffsetX,
				y - textOffsetY);
		
		if(hasSubtitle()) {
			if (selected) {
				style.subtitleFont.setColor(fontColorSelected.r, fontColorSelected.g,
						fontColorSelected.b, fontColorSelected.a * parentAlpha * 0.5f);
			} else {
				style.subtitleFont.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha * 0.5f);			
			}			
			
			style.subtitleFont.draw(batch, getCellSubTitle(item), x + textOffsetX,
					y - textOffsetY - (font.getCapHeight() - font.getDescent() * 2));
		}
	}
	
	protected boolean hasSubtitle() {
		return false;
	}
	
	protected boolean hasImage() {
		return false;
	}
	
	public void layout(CustomListStyle style) {
		this.style = style;
		
		BitmapFont font = style.font;
		Drawable selectedDrawable = style.selection;

		textOffsetX = selectedDrawable.getLeftWidth();
		textOffsetY = selectedDrawable.getTopHeight() - font.getDescent();
		
		itemHeight = font.getCapHeight() - font.getDescent() * 2;
		
		if(hasSubtitle()) {
			itemHeight += style.subtitleFont.getCapHeight() - style.subtitleFont.getDescent() * 2;;
		}
		
		itemHeight += selectedDrawable.getTopHeight()
				+ selectedDrawable.getBottomHeight();
	}		

	public float getItemHeight() {
		return itemHeight;
	}

	protected String getCellTitle(T item) {
		return item.toString();
	}

	protected String getCellSubTitle(T item) {
		return null;
	}

	protected TextureRegion getCellImage(T item) {
		return null;
	}
}
