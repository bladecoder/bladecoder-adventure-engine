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

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.ArrayList;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engineeditor.common.EditorLogger;

public class EditableSelectBox<T> extends Table {
	static final Vector2 temp = new Vector2();

	private TextField input;
	private TextButton showListButton;
	private SelectList<T> selectList;

	private T[] items;

	private boolean disabled;

	public EditableSelectBox(Skin skin) {
		super(skin);

		input = new TextField("", skin);
		showListButton = new TextButton(">", skin);
		selectList = new SelectList<>(skin, input);

		add(input);
		add(showListButton);

		addListener(new ClickListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (pointer == 0 && button != 0)
					return false;

				if (disabled)
					return false;

//				if (selectList.getStage() == null)
					showList();

				return true;
			}

			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Keys.ENTER) {
					setSelectedIndex(selectList.list.getSelectedIndex());
					hideList();
					input.setCursorPosition(input.getText().length());
				} else if (keycode == Keys.UP) {
					int idx = selectList.list.getSelectedIndex();

					if (idx > 0)
						selectList.list.setSelectedIndex(idx - 1);

					return true;
				} else if (keycode == Keys.DOWN) {
					int idx = selectList.list.getSelectedIndex();

					if (idx < selectList.list.getItems().size - 1)
						selectList.list.setSelectedIndex(idx + 1);

					return true;
				} else {
					if (selectList.getStage() == null && selectList.list.getItems().size > 0) {
						showList();
					}
					
					filterItems(input.getText());
				}

				return false;
			}
		});

		// selectList.getList().addListener(new ChangeListener() {
		// @Override
		// public void changed(ChangeEvent event, Actor actor) {
		// fire(event);
		// }
		// });

		input.setProgrammaticChangeEvents(true);
		input.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				fire(event);
			}
		});
	}

	public String getSelected() {
		return input.getText();
	}

	public void setSelected(String str) {
		input.setText(str);
	}

	public int getSelectedIndex() {
		return selectList.list.getSelectedIndex();
	}

	public void setSelectedIndex(int idx) {
		if (selectList.list.getItems().size == 0)
			return;

		input.setText(selectList.list.getItems().get(idx).toString());
	}

	public TextField getInput() {
		return input;
	}

	public final void setItems(T[] newItems) {

		items = newItems;

		setListItems(items);
	}

	private final void setListItems(T[] newItems) {
		if (newItems == null)
			throw new IllegalArgumentException("newItems cannot be null.");

		float oldPrefWidth = getPrefWidth();

		selectList.list.setItems(newItems);
		if (newItems.length > 0)
			selectList.list.setSelectedIndex(0);
		else
			selectList.list.setSelectedIndex(-1);

		invalidate();
		if (oldPrefWidth != getPrefWidth())
			invalidateHierarchy();
	}

	public void showList() {
		if (selectList.list.getItems().size == 0)
			return;
		
		if(selectList.list.getSelectedIndex() >= selectList.list.getItems().size)
			selectList.list.setSelectedIndex(selectList.list.getItems().size - 1);
		
		selectList.show(getStage());
	}

	public void hideList() {
		selectList.hide();
		setListItems(items);
	}

	@SuppressWarnings("unchecked")
	private void filterItems(String s) {

		if (s == null || s.isEmpty()) {
			setListItems(items);
		} else {

			ArrayList<T> filtered = new ArrayList<T>();

			String sl = s.toLowerCase();

			for (T item : items) {
				if (item.toString().toLowerCase().contains(sl))
					filtered.add(item);
			}

			setListItems((T[]) filtered.toArray(new String[filtered.size()]));
		}

		selectList.hide();
		selectList.invalidate();
		showList();
	}

	protected void onShow(Actor selectBoxList, boolean below) {
		selectBoxList.getColor().a = 0;
		selectBoxList.addAction(fadeIn(0.3f, Interpolation.fade));
	}

	static class SelectList<T> extends ScrollPane {
		private final TextField selectBox;
		private int selectedIndex;
		int maxListCount;
		private final Vector2 screenPosition = new Vector2();
		final List<T> list;
		private InputListener hideListener;
		private Actor previousScrollFocus;

		public SelectList(Skin skin, final TextField inputBox) {
			super(null, skin.get(SelectBoxStyle.class).scrollStyle);
			this.selectBox = inputBox;

			setOverscroll(false, false);
			setFadeScrollBars(false);

			ListStyle listStyle = skin.get(SelectBoxStyle.class).listStyle;

			list = new List<>(listStyle);
			list.setTouchable(Touchable.disabled);
			setActor(list);

			list.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					selectBox.setText(list.getSelected().toString());
					selectedIndex = list.getSelectedIndex();
					hide();
				}

				public boolean mouseMoved(InputEvent event, float x, float y) {
					list.setSelectedIndex(
							Math.min(list.getItems().size - 1, (int) ((list.getHeight() - y) / list.getItemHeight())));
					return true;
				}
			});

			addListener(new InputListener() {
				public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
					if (toActor == null || !isAscendantOf(toActor))
						if(selectedIndex < list.getItems().size)
							list.setSelectedIndex(selectedIndex);
						else
							EditorLogger.error("EditableSelectBox:exit selectedIndex outOfBounds: " + selectedIndex);
				}
			});

			hideListener = new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					Actor target = event.getTarget();
					if (isAscendantOf(target))
						return false;
					list.setSelectedIndex(selectedIndex);
					hide();
					return false;
				}

				public boolean keyDown(InputEvent event, int keycode) {
					if (keycode == Keys.ESCAPE)
						hide();
					return false;
				}
			};
		}

		public void show(Stage stage) {
			if (list.isTouchable())
				return;

			stage.removeCaptureListener(hideListener);
			stage.addCaptureListener(hideListener);
			stage.addActor(this);

			selectBox.localToStageCoordinates(screenPosition.set(0, 0));

			// Show the list above or below the select box, limited to a number
			// of items and the available height in the stage.
			float itemHeight = list.getItemHeight();
			float height = itemHeight
					* (maxListCount <= 0 ? list.getItems().size : Math.min(maxListCount, list.getItems().size));
			Drawable scrollPaneBackground = getStyle().background;
			if (scrollPaneBackground != null)
				height += scrollPaneBackground.getTopHeight() + scrollPaneBackground.getBottomHeight();
			Drawable listBackground = list.getStyle().background;
			if (listBackground != null)
				height += listBackground.getTopHeight() + listBackground.getBottomHeight();

			float heightBelow = screenPosition.y;
			float heightAbove = stage.getCamera().viewportHeight - screenPosition.y - selectBox.getHeight();
			boolean below = true;
			if (height > heightBelow) {
				if (heightAbove > heightBelow) {
					below = false;
					height = Math.min(height, heightAbove);
				} else
					height = heightBelow;
			}

			if (below)
				setY(screenPosition.y - height);
			else
				setY(screenPosition.y + selectBox.getHeight());
			setX(screenPosition.x);
			setSize(Math.max(getPrefWidth(), selectBox.getWidth()), height);

			validate();
			scrollTo(0, list.getHeight() - selectedIndex * itemHeight - itemHeight / 2, 0, 0, true, true);
			updateVisualScroll();

			previousScrollFocus = null;
			Actor actor = stage.getScrollFocus();
			if (actor != null && !actor.isDescendantOf(this))
				previousScrollFocus = actor;
			stage.setScrollFocus(this);


			list.setTouchable(Touchable.enabled);
			clearActions();
			// getColor().a = 0;
			// addAction(fadeIn(0.3f, Interpolation.fade));
		}

		public void hide() {
			if (!list.isTouchable() || !hasParent())
				return;
			list.setTouchable(Touchable.disabled);

			Stage stage = getStage();
			if (stage != null) {
				stage.removeCaptureListener(hideListener);
				if (previousScrollFocus != null && previousScrollFocus.getStage() == null)
					previousScrollFocus = null;
				Actor actor = stage.getScrollFocus();
				if (actor == null || isAscendantOf(actor))
					stage.setScrollFocus(previousScrollFocus);
			}

			clearActions();
			getColor().a = 1;
			addAction(sequence(fadeOut(0.15f, Interpolation.fade), Actions.removeActor()));
		}

		public void draw(Batch batch, float parentAlpha) {
			selectBox.localToStageCoordinates(temp.set(0, 0));
			if (!temp.equals(screenPosition))
				hide();
			super.draw(batch, parentAlpha);
		}

		public void act(float delta) {
			super.act(delta);
			toFront();
		}

		public List<T> getList() {
			return list;
		}

		public int getSelectedIndex() {
			return list.getSelectedIndex();
		}
	}
}
