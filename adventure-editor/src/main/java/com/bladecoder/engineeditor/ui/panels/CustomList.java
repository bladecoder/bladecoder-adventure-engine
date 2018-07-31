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
package com.bladecoder.engineeditor.ui.panels;

import java.util.Comparator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

/**
 * List witch items has and icon, a title and a subtitle
 * 
 * @author rgarcia
 *
 * @param <T>
 */
public class CustomList<T> extends Widget implements Cullable {

	private CellRenderer<T> cellRenderer;

	private CustomListStyle style;
	private final Array<T> items = new Array<T>();
	private Rectangle cullingArea;
	private float prefWidth, prefHeight;
	final ArraySelection<T> selection;

	public CustomList(Skin skin, CellRenderer<T> r) {
		this(skin.get(CustomListStyle.class), r);
	}

	public CustomList(Skin skin) {
		this(skin.get(CustomListStyle.class), new CellRenderer<T>());
	}

	public CustomList(Skin skin, String styleName) {
		this(skin.get(styleName, CustomListStyle.class), new CellRenderer<T>());
	}

	public CustomList(CustomListStyle style, CellRenderer<T> r) {
		selection = new ArraySelection<T>(items);
		selection.setActor(this);
		selection.setRequired(true);

		cellRenderer = r;

		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());

		addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (pointer == 0 && button != 0)
					return false;
				if (selection.isDisabled())
					return false;
				CustomList.this.touchDown(y);
				return true;
			}
		});
	}

	public void setCellRenderer(CellRenderer<T> r) {
		cellRenderer = r;
	}

	void touchDown(float y) {
		if (items.size == 0)
			return;
		float height = getHeight();
		if (style.background != null) {
			height -= style.background.getTopHeight() + style.background.getBottomHeight();
			y -= style.background.getBottomHeight();
		}
		int index = (int) ((height - y) / cellRenderer.getItemHeight());
		index = Math.max(0, index);
		index = Math.min(items.size - 1, index);
		selection.choose(items.get(index));
	}

	public void setStyle(CustomListStyle style) {
		if (style == null)
			throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		invalidateHierarchy();
	}

	/**
	 * Returns the list's style. Modifying the returned style may not have an
	 * effect until {@link #setStyle(CustomListStyle)} is called.
	 */
	public CustomListStyle getStyle() {
		return style;
	}

	public void layout() {
		final BitmapFont font = style.font;
		final BitmapFont subfont = style.subtitleFont;
		final Drawable selectedDrawable = style.selection;

		cellRenderer.layout(style);

		GlyphLayout textLayout = new GlyphLayout();

		prefWidth = 0;
		for (int i = 0; i < items.size; i++) {

			textLayout.setText(font, cellRenderer.getCellTitle(items.get(i)));

			prefWidth = Math.max(textLayout.width, prefWidth);
			
			if (cellRenderer.hasImage()) {
				TextureRegion r = cellRenderer.getCellImage(items.get(i));

				float ih = r.getRegionHeight();
				float iw = r.getRegionWidth();

				if (ih > getItemHeight() - 10) {
					ih = getItemHeight() - 10;
					iw *= ih / r.getRegionHeight();
				}

				prefWidth = Math.max(iw + textLayout.width, prefWidth);
			}

			if (cellRenderer.hasSubtitle()) {
				String subtitle = cellRenderer.getCellSubTitle(items.get(i));

				if (subtitle != null) {
					textLayout.setText(subfont, subtitle);
					prefWidth = Math.max(textLayout.width, prefWidth);
				}
			}
		}
		
		prefWidth += selectedDrawable.getLeftWidth() + selectedDrawable.getRightWidth();

		prefHeight = items.size * cellRenderer.getItemHeight();

		Drawable background = style.background;
		if (background != null) {
			prefWidth += background.getLeftWidth() + background.getRightWidth();
			prefHeight += background.getTopHeight() + background.getBottomHeight();
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate();

		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

		float x = getX(), y = getY(), width = getWidth(), height = getHeight();
		float itemY = height;

		Drawable background = style.background;
		if (background != null) {
			background.draw(batch, x, y, width, height);
			float leftWidth = background.getLeftWidth();
			x += leftWidth;
			itemY -= background.getTopHeight();
			width -= leftWidth + background.getRightWidth();
		}

		for (int i = 0; i < items.size; i++) {
			if (cullingArea == null || (itemY - cellRenderer.getItemHeight() <= cullingArea.y + cullingArea.height
					&& itemY >= cullingArea.y)) {
				T item = items.get(i);
				boolean selected = selection.contains(item);

				cellRenderer.draw(batch, parentAlpha, item, selected, x, y + itemY, width,
						cellRenderer.getItemHeight());

			} else if (itemY < cullingArea.y) {
				break;
			}
			itemY -= cellRenderer.getItemHeight();
		}
	}
	
	public void sortByTitle() {
		getItems().sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				String s1 = cellRenderer.getCellTitle(o1);
				String s2 = cellRenderer.getCellTitle(o2);
				
				if(s1 == null) {
					System.out.println("nul");
				}
				
				return s1.compareTo(s2);
			}
		});
	}

	public ArraySelection<T> getSelection() {
		return selection;
	}

	/** Returns the first selected item, or null. */
	public T getSelected() {
		return selection.first();
	}

	/**
	 * @return The index of the first selected item. The top item has an index
	 *         of 0. Nothing selected has an index of -1.
	 */
	public int getSelectedIndex() {
		ObjectSet<T> selected = selection.items();
		return selected.size == 0 ? -1 : items.indexOf(selected.first(), false);
	}

	/** Sets the selection to only the selected index. */
	public void setSelectedIndex(int index) {
		if (index < -1 || index >= items.size)
			throw new IllegalArgumentException("index must be >= -1 and < " + items.size + ": " + index);
		selection.set(items.get(index));
	}

	/**
	 * Sets the current items, clearing the selection if it is no longer valid.
	 * If a selection is {@link ArraySelection#getRequired()}, the first item is
	 * selected.
	 */
	public void setItems(Array<T> newItems) {
		if (newItems == null)
			throw new IllegalArgumentException("newItems cannot be null.");

		items.clear();
		items.addAll(newItems);

		T selected = getSelected();
		if (!items.contains(selected, false)) {
			if (selection.getRequired() && items.size > 0)
				selection.set(items.first());
			else
				selection.clear();
		}

		invalidateHierarchy();
	}

	public Array<T> getItems() {
		return items;
	}

	public float getItemHeight() {
		return cellRenderer.getItemHeight();
	}

	public float getPrefWidth() {
		validate();
		return prefWidth;
	}

	public float getPrefHeight() {
		validate();
		return prefHeight;
	}

	public void setCullingArea(Rectangle cullingArea) {
		this.cullingArea = cullingArea;
	}

	/**
	 * The style for a list, see {@link List}.
	 * 
	 * @author mzechner
	 * @author Nathan Sweet
	 */
	static public class CustomListStyle {
		public BitmapFont font;
		public BitmapFont subtitleFont;
		public Color fontColorSelected = new Color(1, 1, 1, 1);
		public Color fontColorUnselected = new Color(1, 1, 1, 1);
		public Drawable selection;
		/** Optional. */
		public Drawable background;

		public CustomListStyle() {
		}

		public CustomListStyle(BitmapFont font, Color fontColorSelected, Color fontColorUnselected,
				Drawable selection) {
			this.font = font;
			this.fontColorSelected.set(fontColorSelected);
			this.fontColorUnselected.set(fontColorUnselected);
			this.selection = selection;
		}

		public CustomListStyle(CustomListStyle style) {
			this.font = style.font;
			this.fontColorSelected.set(style.fontColorSelected);
			this.fontColorUnselected.set(style.fontColorUnselected);
			this.selection = style.selection;
		}
	}
}
