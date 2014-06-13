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

import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class WalkZoneWindow extends Container {
	private static final String DELETE_WALK_ZONE_TEXT = "Delete Walk Zone";
	private static final String CREATE_WALK_ZONE_TEXT = "Create Walk Zone";
	private static final String SET_ACTOR_AS_OBSTACLE_TEXT = "Set Actor as Obstacle";
	private static final String REMOVE_ACTOR_AS_OBSTACLE_TEXT = "Remove Actor as Obstacle";
	private static final float OBSTACLE_WIDTH = 200;

	TextButton createZoneBtn;
	TextButton createObstacleBtn;
	TextButton deleteObstacleBtn;
	TextButton addObstacleActorBtn;
	Scene scn;
	org.bladecoder.engine.model.Actor actor;

	private final ScnWidgetInputListener scnIL;

	public WalkZoneWindow(Skin skin, ScnWidgetInputListener sIL) {
		this.scnIL = sIL;

		Table table = new Table(skin);
		createZoneBtn = new TextButton(CREATE_WALK_ZONE_TEXT, skin);
		createObstacleBtn = new TextButton("Create Obstacle", skin);
		deleteObstacleBtn = new TextButton("Delete Obstacle", skin);
		addObstacleActorBtn = new TextButton(SET_ACTOR_AS_OBSTACLE_TEXT, skin);

		createZoneBtn.setDisabled(true);
		createObstacleBtn.setDisabled(true);
		deleteObstacleBtn.setDisabled(true);
		addObstacleActorBtn.setDisabled(true);

		table.top();
		table.add(new Label("Walk Zone", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();
		table.add(createZoneBtn).expandX().fill();
		table.row();
		table.add(createObstacleBtn).expandX().fill();
		table.row();
		table.add(deleteObstacleBtn).expandX().fill();
		table.row();
		table.add(addObstacleActorBtn).expandX().fill();
		setWidget(table);

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
					createObstacleBtn.setDisabled(false);
					addObstacleActorBtn.setDisabled(false);
					Ctx.project.getSelectedChapter().createWalkZone(
							Ctx.project.getSelectedScene(), poly);
				} else {
					createZoneBtn.setText(CREATE_WALK_ZONE_TEXT);
					createObstacleBtn.setDisabled(true);
					deleteObstacleBtn.setDisabled(true);
					addObstacleActorBtn.setDisabled(true);
					scn.setPolygonalNavGraph(null);
					Ctx.project.getSelectedChapter().deleteWalkZone(
							Ctx.project.getSelectedScene());
				}

				event.cancel();
			}

		});

		createObstacleBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float[] verts = new float[8];

				verts[3] = OBSTACLE_WIDTH;
				verts[4] = OBSTACLE_WIDTH;
				verts[5] = OBSTACLE_WIDTH;
				verts[6] = OBSTACLE_WIDTH;

				Polygon poly = new Polygon(verts);
				PolygonalNavGraph pf = scn.getPolygonalNavGraph();
				pf.addObstacle(poly);
				Ctx.project.getSelectedChapter().createObstacle(
						Ctx.project.getSelectedScene(), poly);
				deleteObstacleBtn.setDisabled(false);

				event.cancel();
			}
		});

		deleteObstacleBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				PolygonalNavGraph pf = scn.getPolygonalNavGraph();

				if (pf.getObstacles().size() > 0)
					scnIL.setDeleteObstacle(true);
				else
					Ctx.msg.show(getStage(),
							"There are no obstacles to delete", 3);

				// if(pf.getObstacles().size() == 0)
				// deleteObstacleBtn.setDisabled(true);

				event.cancel();
			}
		});

		addObstacleActorBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor a) {
				if (actor.isWalkObstacle()) {
					actor.setWalkObstacle(false);
					Ctx.project.getSelectedChapter().setRootAttr(Ctx.project.getSelectedActor(), "obstacle", "false");
					addObstacleActorBtn.setText(SET_ACTOR_AS_OBSTACLE_TEXT);
				} else {
					actor.setWalkObstacle(true);
					Ctx.project.getSelectedChapter().setRootAttr(Ctx.project.getSelectedActor(), "obstacle", "true");
					addObstacleActorBtn.setText(REMOVE_ACTOR_AS_OBSTACLE_TEXT);
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
			createObstacleBtn.setDisabled(true);
			deleteObstacleBtn.setDisabled(true);
			return;
		}

		createZoneBtn.setDisabled(false);

		if (scn.getPolygonalNavGraph() == null) {
			createZoneBtn.setText(CREATE_WALK_ZONE_TEXT);
			createObstacleBtn.setDisabled(true);
		} else {
			createZoneBtn.setText(DELETE_WALK_ZONE_TEXT);
			createObstacleBtn.setDisabled(false);
		}

		if (scn.getPolygonalNavGraph() != null
				&& scn.getPolygonalNavGraph().getObstacles().size() > 0) {
			deleteObstacleBtn.setDisabled(false);
		} else {
			deleteObstacleBtn.setDisabled(true);
		}
	}

	public void setActor(org.bladecoder.engine.model.Actor a) {
		this.actor = a;
		
		if(a == null) {
			addObstacleActorBtn.setDisabled(true);			
		} else {
			addObstacleActorBtn.setDisabled(false);
			
			if(!a.isWalkObstacle()) {
				addObstacleActorBtn.setText(SET_ACTOR_AS_OBSTACLE_TEXT);
			} else {
				addObstacleActorBtn.setText(SET_ACTOR_AS_OBSTACLE_TEXT);
			}
		}
	}
}
