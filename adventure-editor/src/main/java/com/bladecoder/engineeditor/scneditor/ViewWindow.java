/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.scneditor;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.util.DPIUtils;

public class ViewWindow extends Container<Table> {
	ScnWidget scnWidget;
	
	CheckBox walkZone;

	public ViewWindow(Skin skin, ScnWidget sw) {
		
		scnWidget = sw;

		Table table = new Table(skin);
		table.defaults().left().expandX();
		table.top().pad(DPIUtils.getSpacing()/2);
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
		walkZone = new CheckBox("Show Walkzone", skin);
		walkZone.setChecked(scnWidget.getShowWalkZone());
		table.add(walkZone);		
		
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
		
		walkZone.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setShowWalkZone(walkZone.isChecked());
			}
		});		


		table.pack();
		setActor(table);

		prefSize(table.getWidth(), Math.max(200, table.getHeight()));
		setSize(table.getWidth(), Math.max(200, table.getHeight()));
	}
	
	public void showWalkZone() {
		scnWidget.setShowWalkZone(true);
		walkZone.setChecked(true);
	}
}
