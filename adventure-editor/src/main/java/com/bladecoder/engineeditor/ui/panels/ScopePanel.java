package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

abstract public class ScopePanel extends Table {
	public static String WORLD_SCOPE = "World";
	public static String SCENE_SCOPE = "Scene";
	public static String ACTOR_SCOPE = "Actor";
	
	private ButtonGroup<TextButton> buttonGroup;
	private HorizontalGroup hPanel;
	private Skin skin;
	
	public ScopePanel(Skin skin) {
		super(skin);
		this.skin = skin;
		buttonGroup = new ButtonGroup<TextButton>();
		hPanel = new HorizontalGroup();
		hPanel.wrap(true);
		hPanel.rowAlign(Align.left);
		
		buttonGroup.setMaxCheckCount(1);
		buttonGroup.setMinCheckCount(1);
		buttonGroup.setUncheckLast(true);
		
		hPanel.addActor(new Label("Scope: ", skin));
		
		addButton(WORLD_SCOPE);
		addButton(SCENE_SCOPE);
		addButton(ACTOR_SCOPE);
		
		add(hPanel).expandX().fillX().center();
		
		buttonGroup.getButtons().get(2).setChecked(true);
	}
	
	private void addButton(String name) {
		TextButton button = new TextButton(name, skin);
		buttonGroup.add(button);
		hPanel.addActor(button);
		
		button.addListener(new ClickListener() {
			
			@Override
			public void clicked (InputEvent event, float x, float y) {
				changeScope((TextButton)event.getListenerActor());
			}
		});
	}
	
	public int getSelectedIndex() {
		for(int i=0; i < buttonGroup.getButtons().size; i++) {
			if(buttonGroup.getButtons().get(i) == buttonGroup.getChecked())
				return i;
		}
		
		return -1;
	}
	
	public void changeScope(TextButton b) {		
		b.setChecked(true);
		
		scopeChanged(b.getText().toString());
	}
	
	abstract public void scopeChanged(String scope);
	
	public String getScope() {
		return buttonGroup.getChecked().getText().toString();
	}
	
	public void clear() {
		Array<TextButton> buttons = buttonGroup.getButtons();
		
		buttons.clear();		
		hPanel.clear();
	}
}
