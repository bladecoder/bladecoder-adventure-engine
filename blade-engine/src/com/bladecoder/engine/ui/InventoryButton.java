package com.bladecoder.engine.ui;


import com.bladecoder.engine.ui.InventoryUI;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.util.DPIUtils;

public class InventoryButton extends ImageButton {
	
	private final InventoryUI inventory;

	public InventoryButton(Skin skin, InventoryUI inv) {
		super(skin, "inventory");
		this.inventory = inv;
		
		addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if(!inventory.isVisible())
					inventory.show();
				else
					inventory.hide();
			}
		});
	}
	
	public void resize(int width, int height) {
		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();
		
		setSize(size, size);
		
//		getImageCell().minSize(DPIUtils.getPrefButtonSize(width, height),DPIUtils.getPrefButtonSize(width, height));
		
		float iconSize = Math.max(size/2, DPIUtils.ICON_SIZE);
		getImageCell().maxSize(iconSize, iconSize);
		
		setPosition(margin, margin);
	}
}
