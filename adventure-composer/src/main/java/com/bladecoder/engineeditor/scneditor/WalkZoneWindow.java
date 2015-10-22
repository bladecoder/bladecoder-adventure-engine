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

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engineeditor.Ctx;

public class WalkZoneWindow extends Container<Table> {
	private static final String DELETE_WALK_ZONE_TEXT = "Delete Walk Zone";
	private static final String CREATE_WALK_ZONE_TEXT = "Create Walk Zone";

	TextButton createZoneBtn;

	Scene scn;
	com.bladecoder.engine.model.BaseActor actor;

	public WalkZoneWindow(Skin skin, ScnWidgetInputListener sIL) {

		Table table = new Table(skin);
		createZoneBtn = new TextButton(CREATE_WALK_ZONE_TEXT, skin);
		createZoneBtn.setDisabled(true);

		table.top();
		table.add(new Label("Walk Zone", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();
		table.add(createZoneBtn).expandX().fill();
		setActor(table);

		createZoneBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				if (scn.getPolygonalNavGraph() == null) {
					float[] verts = new float[8];

					float width = scn.getCamera().getScrollingWidth();
					float height = scn.getCamera().getScrollingHeight();

					verts[3] = height;
					verts[4] = width;
					verts[5] = height;
					verts[6] = width;

					Polygon poly = new Polygon(verts);
					PolygonalNavGraph pf = new PolygonalNavGraph();
					pf.setWalkZone(poly);
					scn.setPolygonalNavGraph(pf);
					createZoneBtn.setText(DELETE_WALK_ZONE_TEXT);
		
//					Ctx.project.getSelectedChapter().createWalkZone(
//							Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()), poly);
				} else {
					createZoneBtn.setText(CREATE_WALK_ZONE_TEXT);
			
					scn.setPolygonalNavGraph(null);
//					Ctx.project.getSelectedChapter().deleteWalkZone(
//							Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()));
				}

				event.cancel();
			}

		});


		prefSize(200, 200);
		setSize(200, 200);
	}

	public void setScene(Scene scn) {
		this.scn = scn;
		this.actor = null;

		if (scn == null) {
			createZoneBtn.setDisabled(true);
	
			return;
		}

		createZoneBtn.setDisabled(false);

		if (scn.getPolygonalNavGraph() == null) {
			createZoneBtn.setText(CREATE_WALK_ZONE_TEXT);
			
		} else {
			createZoneBtn.setText(DELETE_WALK_ZONE_TEXT);
			
		}

	}
}
