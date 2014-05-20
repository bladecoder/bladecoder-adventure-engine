package org.bladecoder.engineeditor.ui.components;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.Array;

public class TabPanel extends Table {
	private ButtonGroup buttonGroup;
	private HorizontalGroup header;
	private Container body;
	private List<Tab> tabs;
	private Skin skin;
	
	public class Tab {
		public Button button;
		public Actor content;
		
		public Tab(Button b, Actor c) {
			button = b;
			content = c;
		}
	}
	
	public TabPanel(Skin skin) {
		super(skin);
		this.skin = skin;
		buttonGroup = new ButtonGroup();
		header = new HorizontalGroup();
		body = new Container();
		tabs = new ArrayList<Tab>();
		
		buttonGroup.setMaxCheckCount(1);
		buttonGroup.setMinCheckCount(1);
		buttonGroup.setUncheckLast(true);
//		top().left();
//		
		add(header).expandX().fillX().left();
		row();
		add(body).expand().fill();

		body.size(0);
		body.fill();
	}
	
	public void addTab(String name, Actor panel) {
		Button button = new TextButton(name, skin);
		buttonGroup.add(button);
		header.addActor(button);
		tabs.add(new Tab(button, panel));
		
		button.addListener(new ClickListener() {
			
			@Override
			public void clicked (InputEvent event, float x, float y) {
				setTab((Button)event.getListenerActor());
			}
		});
		
		if(tabs.size() == 1)
			setTab(0);
	}
	
	private void setTab(Button b) {		
		for(int i = 0; i < tabs.size(); i++) {
			if(tabs.get(i).button == b) {
				setTab(i);
				break;
			}
		}
	}
	
	public int getSelectedIndex() {
		for(int i=0; i < buttonGroup.getButtons().size; i++) {
			if(buttonGroup.getButtons().get(i) == buttonGroup.getChecked())
				return i;
		}
		
		return -1;
	}
	
	public int getTabCount() {
		return tabs.size();
	}
	
	public String getTitleAt(int i) {
		return ((TextButton)buttonGroup.getButtons().get(i)).getText().toString();
	}
	
	public void setTab(int i) {		
		Actor panel = tabs.get(i).content;
		tabs.get(i).button.setChecked(true);
		body.clear();
		body.setWidget(panel);

		if(panel instanceof Layout)
			body.prefHeight(((Layout)panel).getPrefHeight());
		else
			body.prefHeight(panel.getHeight());
		
		invalidateHierarchy();
	}
	
	public void clear() {
		Array<Button> buttons = buttonGroup.getButtons();
		
		for(Button b:buttons)
			buttonGroup.remove(b);
		
		header.clear();
		tabs.clear();
		body.clear();
	}
}
