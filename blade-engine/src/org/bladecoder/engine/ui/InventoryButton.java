package org.bladecoder.engine.ui;


import org.bladecoder.engine.util.DPIUtils;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

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
		setSize(DPIUtils.getPrefButtonSize(width, height), DPIUtils.getPrefButtonSize(width, height));
		
		getImageCell().minSize(DPIUtils.getPrefButtonSize(width, height),DPIUtils.getPrefButtonSize(width, height));
		getImageCell().maxSize(DPIUtils.getPrefButtonSize(width, height),DPIUtils.getPrefButtonSize(width, height));
		
		setPosition(DPIUtils.getMarginSize(width, height), DPIUtils.getMarginSize(width, height));
	}
}
