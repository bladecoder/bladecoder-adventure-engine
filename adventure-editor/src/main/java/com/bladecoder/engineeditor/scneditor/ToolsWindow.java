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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class ToolsWindow extends Container<Table> {

	TextButton button1;

	ScnWidget scnWidget;
	com.bladecoder.engine.model.BaseActor actor;

	public ToolsWindow(Skin skin, ScnWidget sw) {
		
		scnWidget = sw;

		Table table = new Table(skin);
		button1 = new TextButton("PUSHME", skin);
		button1.setDisabled(true);

		table.top();
		table.add(new Label("Tools", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();
		table.add(button1).expandX().fill();
		setActor(table);

		button1.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

//				event.cancel();
			}

		});


		prefSize(200, 200);
		setSize(200, 200);
	}
}
