package com.bladecoder.engine.ui;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.util.DPIUtils;

public class MenuButton extends ImageButton {
	
	private final UI ui;

	public MenuButton(Skin skin, UI u) {
		super(skin, "menu");
		this.ui = u;
		
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
		
//		getImageCell().minSize(DPIUtils.getPrefButtonSize(width, height),DPIUtils.getPrefButtonSize(width, height));
		
		float iconSize = Math.max(size/2, DPIUtils.ICON_SIZE);
		getImageCell().maxSize(iconSize, iconSize);
		
		setPosition(
				getStage().getViewport().getScreenWidth() - getWidth()
						- margin, getStage().getViewport()
						.getScreenHeight()
						- getHeight()
						- margin);
	}
}
