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
package org.bladecoder.engineeditor.scneditor;

import java.io.IOException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.utils.RunProccess;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ScnEditor extends Table {
	ScnWidget scnWidget;
	CheckBox inSceneCb;
	CheckBox animCb;
	TextButton testButton;
	TextButton walkZoneButton;
	
	public ScnEditor(Skin skin) {
		super(skin);
		
		scnWidget = new ScnWidget(skin);
		
		inSceneCb = new CheckBox("In Scene Sprites", skin);
		inSceneCb.setChecked(false);
		animCb = new CheckBox("Animation", skin);
		animCb.setChecked(true);
		testButton = new TextButton("Test", skin);
		walkZoneButton = new TextButton("Walk Zone", skin);
		
		add(scnWidget).expand().fill();
		row();

		Table bottomTable = new Table(skin);
		bottomTable.left();
		// bottomTable.setBackground("background");
		add(bottomTable).fill();

		bottomTable.add(walkZoneButton);
		bottomTable.add(inSceneCb);
		bottomTable.add(animCb);
		bottomTable.add(testButton);
		
		walkZoneButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if(walkZoneButton.isChecked())
					getScnWidget().showEditWalkZoneWindow();
				else
					getScnWidget().hideEditWalkZoneWindow();
			}
		});


		inSceneCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setInSceneSprites(inSceneCb.isChecked());
			}
		});

		animCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setAnimation(animCb.isChecked());
			}
		});

		testButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				test();
				event.cancel();
			}
		});
	}
	
	public ScnWidget getScnWidget() {
		return scnWidget;
	}	
	
	private void test() {
		try {
			try {
				Ctx.project.saveProject();
			} catch (Exception ex) {
				String msg = "Something went wrong while saving the project.\n\n"
						+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
				Ctx.msg.show(getStage(),msg, 2);
			}
			
			RunProccess.runBladeEngine(
					Ctx.project.getProjectDir().getAbsolutePath(), 
					Ctx.project.getSelectedChapter().getId(Ctx.project.getSelectedScene()));
		} catch (IOException e) {
			String msg = "Something went wrong while testing the scene.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(),msg, 2);
		}
	}

	public void dispose() {
		scnWidget.dispose();
	}
}
