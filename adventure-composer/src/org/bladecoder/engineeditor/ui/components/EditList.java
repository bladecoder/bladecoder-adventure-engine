package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engineeditor.utils.EditorLogger;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;

public abstract class EditList<T> extends Table {
	
	protected EditToolbar toolbar;
    protected CustomList<T> list;
    protected Skin skin;
    protected Container container;
	
	public EditList(Skin skin) {
		super(skin);
		
		this.skin = skin;
		
		
		list = new CustomList<T>(skin);
		
		Array<T> items = new Array<T>();
		list.setItems(items);
			
		ScrollPane scrollPane = new ScrollPane(list, skin);
		container = new Container(scrollPane);
		container.fill();
		container.prefHeight(100);
		
		toolbar = new EditToolbar(skin);
//		debug();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();
		
		toolbar.addCreateListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				create();
			}
		});
		
		toolbar.addEditListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				edit();
			}
		});
		
		toolbar.addDeleteListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				delete();
			}
		});
		
		toolbar.addCopyListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				copy();
			}
		});
		
		toolbar.addPasteListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				paste();
			}
		});
		
		list.addListener(new InputListener() {
			@Override
			public void enter(InputEvent event, float x, float y, int pointer,  com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
				EditorLogger.debug("ENTER - X: " + x + " Y: " + y);
				getStage().setScrollFocus(list);
			}
		});
    }
	
	public void setCellRenderer(CellRenderer<T> r) {
		list.setCellRenderer(r);	
	}
	
	public Array<T> getItems () {
		return list.getItems();
	}
	
	public void addItem(T item) {		
		list.getItems().add(item);
	}
	
	public void clear() {
		list.getItems().clear();
	}
	
	abstract protected void create();
	abstract protected void edit();
	abstract protected void delete();
	abstract protected void copy();
	abstract protected void paste();
}
