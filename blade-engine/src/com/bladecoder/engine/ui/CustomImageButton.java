package com.bladecoder.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.util.DPIUtils;

public class CustomImageButton extends ImageButton {

	public CustomImageButton(ImageButtonStyle menuButtonStyle) {
		super(menuButtonStyle);
		
		resize();
	}

	public CustomImageButton(Skin skin, String string) {
		super(skin, string);
		
		resize();
	}

	public void resize() {
		float size = DPIUtils.getPrefButtonSize();
		float iconSize = Math.max(size / 1.5f, DPIUtils.ICON_SIZE);
		setSize(size, size);
		getImageCell().maxSize(iconSize, iconSize);		
	}
}
