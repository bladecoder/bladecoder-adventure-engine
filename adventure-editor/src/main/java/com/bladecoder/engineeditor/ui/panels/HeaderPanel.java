package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.bladecoder.engineeditor.Ctx;

public class HeaderPanel extends Table {
	private Actor content;
	private HorizontalGroup north;
	private Label titleLbl;
	private boolean collapsable = true;
	private Image collapseImg;
	private Cell<Actor> contentCell;

	@SuppressWarnings("unchecked")
	public HeaderPanel(Skin skin, String title) {
		titleLbl = new Label(title, skin);
		north = new HorizontalGroup();
		collapseImg = new Image();
		collapseImg.setScaling(Scaling.none);
		
		if(collapsable) {
			north.addActor(collapseImg);
			collapseImg.setDrawable(new TextureRegionDrawable(Ctx.assetManager.getIcon("ic_open")));
		}
		
		top().left();
		
		north.addActor(titleLbl);
		north.align(Align.left);
		north.fill();
		north.wrap(true);
		north.rowAlign(Align.left);
		add(north).expandX().fillX();
		row();
		Image image = new Image(skin.getDrawable("white_pixel"));
		image.setColor(skin.getColor("separator-color"));
		add(image).height(2).padBottom(4f).expandX().fill();
		row().top().left();
		contentCell = add().expand().fill();
		

		if (collapsable) {
			titleLbl.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					toggleCollapse();
					invalidateHierarchy();
				}
			});
			
			collapseImg.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					toggleCollapse();
					invalidateHierarchy();
				}
			});
		}
		
//		setDebug(true);
	}

	public void setTile(String title) {
		titleLbl.setText(title);
	}

	public void setContent(Actor center) {
		this.contentCell.clearActor();
		this.content = center;
		this.contentCell.setActor(center);
		
		invalidateHierarchy();
	}
	

	public void setCollapsable(boolean c) {
		collapsable = c;

		if (c) {
			collapseImg.setDrawable(new TextureRegionDrawable(Ctx.assetManager.getIcon("ic_open")));
		} else
			collapseImg.setDrawable(null);
	}

	public void toggleCollapse() {
		if (collapsable) {
			if (contentCell.getActor() != null) {
//			if (this.content.isVisible()) {
//				this.content.setVisible(false);
				this.contentCell.clearActor();
				invalidateHierarchy();

				collapseImg.setDrawable(new TextureRegionDrawable(Ctx.assetManager.getIcon("ic_closed")));
			} else {
				this.contentCell.setActor(content);	
//				this.content.setVisible(true);				
				invalidateHierarchy();

				collapseImg.setDrawable(new TextureRegionDrawable(Ctx.assetManager.getIcon("ic_open")));
			}
		}
	}

}
