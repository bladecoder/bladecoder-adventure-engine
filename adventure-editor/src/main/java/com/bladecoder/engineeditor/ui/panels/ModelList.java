package com.bladecoder.engineeditor.ui.panels;

import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public abstract class ModelList<PARENT, T> extends EditList<T> {

	protected T clipboard;
	protected PARENT parent;

	private boolean sorted;

	public ModelList(Skin skin, boolean sorted) {
		super(skin);

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
			}
		});

		this.sorted = sorted;
	}

	public void addElements(PARENT parent, List<T> elements) {

		this.parent = parent;
		list.getItems().clear();
		list.getSelection().clear();

		if (elements != null) {

			for (T e : elements) {
				addItem(e);
			}
		}

		toolbar.disableEdit(list.getSelectedIndex() < 0);

		if (sorted) {
			list.sortByTitle();
		}

		if (getItems().size > 0)
			list.getSelection().choose(list.getItems().get(0));

		toolbar.disableCreate(parent == null);
		// container.prefHeight(list.getItemHeight() * (list.getItems().size >
		// 3?list.getItems().size:3));
		list.invalidateHierarchy();
	}

	@Override
	protected void create() {
		EditModelDialog<PARENT, T> dialog = getEditElementDialogInstance(null);

		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				T e = ((EditModelDialog<PARENT, T>) actor).getElement();

				if (getItems().indexOf(e, true) == -1) {
					addItem(e);
				}

				if (sorted) {
					list.sortByTitle();
				}

				int i = getItems().indexOf(e, true);
				list.setSelectedIndex(i);
				list.invalidateHierarchy();
			}
		});
	}

	@Override
	protected void edit() {
		T e = list.getSelected();

		if (e == null)
			return;

		EditModelDialog<PARENT, T> dialog = getEditElementDialogInstance(e);
		dialog.show(getStage());
	}

	protected abstract EditModelDialog<PARENT, T> getEditElementDialogInstance(T e);

	protected T removeSelected() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return null;

		T e = list.getItems().removeIndex(pos);

		clipboard = e;

		if (pos > 0)
			list.setSelectedIndex(pos - 1);
		else if (pos == 0 && list.getItems().size > 0)
			list.setSelectedIndex(0);
		else
			list.getSelection().clear();

		toolbar.disablePaste(false);

		return e;
	}
}
