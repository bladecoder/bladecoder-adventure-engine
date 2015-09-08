package com.bladecoder.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.util.DPIUtils;

public class CustomImageButton extends ImageButton {

	public CustomImageButton(ImageButtonStyle menuButtonStyle) {
		super(menuButtonStyle);
		
		setSize();
	}

	public CustomImageButton(Skin skin, String string) {
		super(skin, string);
		
		setSize();
	}
	
	private void setSize() {
		float size = DPIUtils.getPrefButtonSize();
		setSize(size, size);		
	}

	@Override
	public void sizeChanged() {

		if(getImageCell() == null)
			return;
		
		float iconSize = Math.max(getWidth() / 1.5f, DPIUtils.ICON_SIZE);

		getImageCell().maxSize(iconSize, iconSize);		
	}
}
