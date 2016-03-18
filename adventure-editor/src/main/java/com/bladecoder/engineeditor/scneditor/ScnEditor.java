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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.bladecoder.engineeditor.utils.Message;
import com.bladecoder.engineeditor.utils.RunProccess;

public class ScnEditor extends Table {
	private static final String DELETE_WALK_ZONE_TEXT = "Delete Walk Zone";
	private static final String CREATE_WALK_ZONE_TEXT = "Create Walk Zone";

	private ScnWidget scnWidget;
	private TextButton testButton;

	private ButtonGroup<Button> buttonGroup;

	private TextButton walkZoneButton;
	private TextButton toolsButton;
	private TextButton viewButton;

	private ToolsWindow toolsWindow;
	private ViewWindow viewWindow;

	public ScnEditor(Skin skin) {
		super(skin);

		scnWidget = new ScnWidget(skin);
		testButton = new TextButton("Test", skin);

		buttonGroup = new ButtonGroup<Button>();
		buttonGroup.setMaxCheckCount(1);
		buttonGroup.setMinCheckCount(0);
		buttonGroup.setUncheckLast(true);

		walkZoneButton = new TextButton(CREATE_WALK_ZONE_TEXT, skin);
		toolsButton = new TextButton("Tools", skin);
		viewButton = new TextButton("View", skin);

		buttonGroup.add(toolsButton);
		buttonGroup.add(viewButton);

		viewWindow = new ViewWindow(skin, scnWidget);
		toolsWindow = new ToolsWindow(skin, scnWidget);

		add(scnWidget).expand().fill();
		row();

		Table bottomTable = new Table(skin);
		bottomTable.left();
		add(bottomTable).fill();

		bottomTable.add(viewButton);
		bottomTable.add(toolsButton);
		bottomTable.add(testButton);
		bottomTable.add(walkZoneButton).right().expandX();

		walkZoneButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				if (scnWidget.getScene().getPolygonalNavGraph() == null) {

					float[] verts = new float[8];

					float width = scnWidget.getScene().getCamera().getScrollingWidth();
					float height = scnWidget.getScene().getCamera().getScrollingHeight();

					verts[3] = height;
					verts[4] = width;
					verts[5] = height;
					verts[6] = width;

					Polygon poly = new Polygon(verts);
					PolygonalNavGraph pf = new PolygonalNavGraph();
					pf.setWalkZone(poly);
					scnWidget.getScene().setPolygonalNavGraph(pf);
					Ctx.project.setModified();
					walkZoneButton.setText(DELETE_WALK_ZONE_TEXT);
					viewWindow.showWalkZone();

				} else {
					new Dialog("Delete Walk Zone", getSkin()) {
						protected void result(Object object) {
							if (((Boolean) object).booleanValue()) {
								walkZoneButton.setText(CREATE_WALK_ZONE_TEXT);

								scnWidget.getScene().setPolygonalNavGraph(null);
								Ctx.project.setModified();
							}
						}
					}.text("Are you sure you want to delete the Walk Zone?").button("Yes", true).button("No", false).key(Keys.ENTER, true)
							.key(Keys.ESCAPE, false).show(getStage());
				}

				event.cancel();
			}
		});

		toolsButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if (toolsButton.isChecked()) {
					scnWidget.getParent().addActor(toolsWindow);
					toolsWindow.setPosition(getScnWidget().getX() + 5, getScnWidget().getY() + 5);
					toolsWindow.invalidate();
				} else {
					scnWidget.getParent().removeActor(toolsWindow);
				}
			}
		});

		viewButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if (viewButton.isChecked()) {
					scnWidget.getParent().addActor(viewWindow);
					viewWindow.setPosition(getScnWidget().getX() + 5, getScnWidget().getY() + 5);
					viewWindow.invalidate();
				} else {
					scnWidget.getParent().removeActor(viewWindow);
				}
			}
		});

		testButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				test();
				event.cancel();
			}
		});

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				EditorLogger.debug("ScnWidget Listener: " + e.getPropertyName());

				if (e.getPropertyName().equals(Project.NOTIFY_SCENE_SELECTED)) {
					if (Ctx.project.getSelectedScene() == null)
						return;

					if (Ctx.project.getSelectedScene().getPolygonalNavGraph() != null) {
						walkZoneButton.setText(DELETE_WALK_ZONE_TEXT);
					} else {
						walkZoneButton.setText(CREATE_WALK_ZONE_TEXT);
					}
				}
			}
		});
	}

	public ScnWidget getScnWidget() {
		return scnWidget;
	}

	private void test() {

		if (Ctx.project.getSelectedScene() == null) {
			String msg = "There are no scenes in this chapter.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error", msg);
		}

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {
				Message.showMsg(stage, "Running scene...", 5);

				try {
					if (!RunProccess.runBladeEngine(Ctx.project.getProjectDir(), Ctx.project.getChapter().getId(),
							Ctx.project.getSelectedScene().getId()))
						Message.showMsg(stage, "There was a problem running the scene", 4);
				} catch (IOException e) {
					Message.showMsgDialog(stage, "Error", "There was a problem running the scene: " + e.getMessage());
				}

			}
		}).start();

	}

	public void dispose() {
		scnWidget.dispose();
	}
}
