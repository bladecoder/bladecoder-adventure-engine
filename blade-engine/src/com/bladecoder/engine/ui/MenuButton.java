package com.bladecoder.engine.ui;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.util.DPIUtils;

public class MenuButton extends Button {
	public MenuButton(final UI ui) {
		super(ui.getSkin(), "menu");

		addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ui.setCurrentScreen(UI.Screens.MENU_SCREEN);
			}
		});
		
	}
	
	public void resize() {
		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();
		
		setSize(size, size);
		
		setPosition(
				getStage().getViewport().getScreenWidth() - getWidth()
						- margin, getStage().getViewport()
						.getScreenHeight()
						- getHeight()
						- margin);
	}
}
