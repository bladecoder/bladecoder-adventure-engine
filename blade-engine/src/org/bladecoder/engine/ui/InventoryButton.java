package org.bladecoder.engine.ui;


import org.bladecoder.engine.util.DPIUtils;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class InventoryButton extends ImageButton {
	
	private final static String  UP_ICON = "uncollapse";
	private final InventoryUI inventory;

	public InventoryButton(InventoryUI inv) {
		super(new ImageButtonStyle());
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
		
		setPosition(0, 0);
	}
	
	public void retrieveAssets(TextureAtlas atlas) {		
		ImageButtonStyle style = getStyle();
		
		TextureRegion upIcon = atlas.findRegion(UP_ICON);
		style.imageUp = new TextureRegionDrawable(upIcon);
		setStyle(style);		
	}
	
	public void resize(int width, int height) {
		setSize(DPIUtils.getButtonPrefSize(), DPIUtils.getButtonPrefSize());
	}
}
