package com.bladecoder.engineeditor.scneditor;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;

public class ViewWindow extends Container<Table> {
	ScnWidget scnWidget;

	CheckBox spriteBounds;

	public ViewWindow(Skin skin, ScnWidget sw) {

		scnWidget = sw;

		Table table = new Table(skin);
		table.defaults().left().expandX();
		table.top().pad(DPIUtils.getSpacing() / 2);
		table.top();
		table.add(new Label("View", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();

		final CheckBox inSceneCb = new CheckBox("Animations in scene", skin);
		inSceneCb.setChecked(scnWidget.getInSceneSprites());
		table.add(inSceneCb);

		table.row();
		final CheckBox animCb = new CheckBox("Show Animations", skin);
		animCb.setChecked(scnWidget.getAnimation());
		table.add(animCb);

		table.row();
		spriteBounds = new CheckBox("Show Sprite Bounds", skin);
		spriteBounds.setChecked(scnWidget.getShowSpriteBounds());
		table.add(spriteBounds);

		table.row();
		final CheckBox console = new CheckBox("Show Debug Console", skin);
		console.setChecked(EditorLogger.console.isVisible());
		table.add(console);

		table.row();
		final CheckBox fullscreen = new CheckBox("Play in fullscreen", skin);
		fullscreen.setChecked(
				Boolean.parseBoolean(Ctx.project.getEditorConfig().getProperty("view.fullscreenPlay", "false")));
		table.add(fullscreen);

		inSceneCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setInSceneSprites(inSceneCb.isChecked());
			}
		});

		animCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setAnimation(animCb.isChecked());
			}
		});

		spriteBounds.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setShowSpriteBounds(spriteBounds.isChecked());
			}
		});

		console.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				EditorLogger.console.setVisible(console.isChecked());
			}
		});

		fullscreen.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				Ctx.project.getEditorConfig().setProperty("view.fullscreenPlay",
						Boolean.toString(fullscreen.isChecked()));
			}
		});

		table.pack();
		setActor(table);

		prefSize(table.getWidth(), Math.max(200, table.getHeight()));
		setSize(table.getWidth(), Math.max(200, table.getHeight()));
	}

	public void showWalkZone() {
		scnWidget.setShowSpriteBounds(true);
		spriteBounds.setChecked(true);
	}
}
