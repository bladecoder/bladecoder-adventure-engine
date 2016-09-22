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
package com.bladecoder.engineeditor;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.ui.BladeSkin;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.scneditor.ScnEditor;
import com.bladecoder.engineeditor.ui.ActorPanel;
import com.bladecoder.engineeditor.ui.ProjectPanel;
import com.bladecoder.engineeditor.ui.ProjectToolbar;
import com.bladecoder.engineeditor.ui.ScenePanel;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.strongjoshua.console.GUIConsole;

//TODO: Set TITLE in window bar. Set '*' in the title when modified

public class Editor implements ApplicationListener {
	public static final String SKIN = "skin/BladeSkin/BladeSkin-ldpi.json";

	Stage stage;
	ScnEditor scnEditor;
	Skin skin;

	@Override
	public void create() {

		Gdx.graphics.setWindowedMode(Math.max((int) (Gdx.graphics.getDisplayMode().width * 0.9), 1920 / 2),
				Math.max((int) (Gdx.graphics.getDisplayMode().height * 0.9), 1080 / 2));

		skin = new BladeSkin(Gdx.files.internal(SKIN));
		VisUI.load();
		FileChooser.setDefaultPrefsName("com.bladecoder.engineeditor.filechooser");

		/*** STAGE SETUP ***/
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		setCtx();

		Message.init(skin);

		scnEditor = new ScnEditor(skin);
		scnEditor.setBackground("background");
		skin.getFont("default-font").getData().markupEnabled = true;

		// RIGHT PANEL
		ScenePanel scenePanel = new ScenePanel(skin);
		ActorPanel actorPanel = new ActorPanel(skin);

		Table rightPanel = new Table(skin);
		rightPanel.top().left();
		rightPanel.add(actorPanel).expand().fill().left();
		rightPanel.setBackground("background");

		SplitPane splitPaneRight = new SplitPane(scnEditor, rightPanel, false, skin);

		splitPaneRight.setSplitAmount(0.75f);

		// LEFT PANEL
		ProjectPanel projectPanel = new ProjectPanel(skin);
		Image img = new Image(Ctx.assetManager.getIcon("title"));
		img.setScaling(Scaling.none);
		img.setAlign(Align.left);

		Table leftPanel = new Table(skin);
		leftPanel.top().left().padLeft(10);
		leftPanel.add(img).expand().fill().padBottom(20).padTop(20).padLeft(0).left();
		leftPanel.row();
		leftPanel.add(new ProjectToolbar(skin)).expandX().fill().left();
		leftPanel.row();
		leftPanel.add(projectPanel).expand().fill().left();
		leftPanel.row();
		leftPanel.add(scenePanel).expand().fill().left();
		leftPanel.setBackground("background");

		SplitPane splitPaneLeft = new SplitPane(leftPanel, splitPaneRight, false, skin);
		splitPaneLeft.setFillParent(true);
		splitPaneLeft.setSplitAmount(0.25f);
		stage.addActor(splitPaneLeft);

		// LOAD LAST OPEN PROJECT
		String lastProject = Ctx.project.getEditorConfig().getProperty(Project.LAST_PROJECT_PROP, "");

		if (!lastProject.isEmpty() && new File(lastProject).exists()) {
			try {
				EditorLogger.debug("Loading last project: " + lastProject);
				Ctx.project.loadProject(new File(lastProject));

				if (!Ctx.project.checkVersion()) {
					new Dialog("Update Engine", skin) {
						protected void result(Object object) {
							if (((Boolean) object).booleanValue()) {
								try {
									Ctx.project.updateEngineVersion();
									Message.showMsg(getStage(), "Project successfully updated.", 3);
								} catch (IOException e1) {
									String msg = "Something went wrong while updating the engine.\n\n"
											+ e1.getClass().getSimpleName() + " - " + e1.getMessage();
									Message.showMsgDialog(getStage(), "Error", msg);

									EditorLogger.error(msg, e1);
								}
							}
						}
					}.text("Your game uses an old (" + Ctx.project.getProjectBladeEngineVersion()
							+ ") Engine version. Do you want to update the engine?").button("Yes", true)
							.button("No", false).key(Keys.ENTER, true).key(Keys.ESCAPE, false).show(stage);
				}
			} catch (Exception e) {
				EditorLogger.error("Error loading last project.", e);
				Ctx.project.closeProject();
			}
		}

		stage.setScrollFocus(scnEditor.getScnWidget());
		stage.setKeyboardFocus(scnEditor.getScnWidget());

		TooltipManager.getInstance().instant();
	}

	private void setCtx() {
		Ctx.project = new Project();
		Ctx.assetManager = new EditorAssetManager();

		EditorLogger.setConsole(new GUIConsole());
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));

		stage.draw();

		EditorLogger.drawConsole();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		EditorLogger.console.refresh();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		scnEditor.dispose();
		stage.dispose();
		EditorLogger.console.dispose();
		VisUI.dispose();

		Ctx.project.saveConfig();
	}

	public void exit() {
		if (Ctx.project.getProjectDir() != null && Ctx.project.isModified()) {
			new Dialog("Save Project", skin) {
				protected void result(Object object) {
					if (((Boolean) object).booleanValue()) {
						try {
							Ctx.project.saveProject();
						} catch (IOException e1) {
							String msg = "Something went wrong while saving the actor.\n\n"
									+ e1.getClass().getSimpleName() + " - " + e1.getMessage();
							Message.showMsgDialog(getStage(), "Error", msg);

							EditorLogger.printStackTrace(e1);
						}
					}

					((Main) Gdx.app).exitSaved();
				}
			}.text("Save changes to project?").button("Yes", true).button("No", false).key(Keys.ENTER, true)
					.key(Keys.ESCAPE, false).show(stage);

		} else {
			((Main) Gdx.app).exitSaved();
		}

	}
}
