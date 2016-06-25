package com.bladecoder.engine.ui;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.common.DPIUtils;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.InventoryUI.InventoryPos;

public class InventoryButton extends Button {
	
	private final InventoryUI inventory;
	
	private int numItems = Integer.MAX_VALUE;

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
	
	@Override
	public void act(float delta) {
		super.act(delta);
		
		if(numItems < World.getInstance().getInventory().getNumItems()) {
//			addAction(Actions.repeat(30, Actions.sequence(Actions.rotateBy(40, 1f), Actions.rotateBy(-40, 1f))));
			addAction(Actions.repeat(4, Actions.sequence(Actions.moveBy(20, 0, .06f), Actions.moveBy(-20, 0, .06f))));
//			addAction(Actions.repeat(3, Actions.sequence(Actions.alpha(0, .4f), Actions.alpha(1, .4f))));
		}
		
		numItems = World.getInstance().getInventory().getNumItems();
	}
	
	public void resize(int width, int height) {
		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();
		
		setSize(size, size);
		
		InventoryPos inventoryPos = inventory.getInventoryPos();
		
		switch (inventoryPos) {
		case TOP:
			setPosition(margin, height - margin - getHeight());	
			break;
		case LEFT:
			setPosition(margin, margin);	
			break;
		case RIGHT:
			setPosition(width - margin - getWidth(), margin);	
			break;
		case DOWN:
		case CENTER:
			setPosition(margin, margin);	
			break;
		default:
			break;
		}
	}
}
