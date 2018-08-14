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
package com.bladecoder.engineeditor.ui;

import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.EditorUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.model.Project;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooser.SelectionMode;
import com.kotcrab.vis.ui.widget.file.FileChooser.ViewMode;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;

public class ProjectToolbar extends Table {
	private ImageButton newBtn;
	private ImageButton loadBtn;
	private ImageButton saveBtn;
	private ImageButton packageBtn;
	private ImageButton exitBtn;
	private ImageButton playBtn;
	private ImageButton assetsBtn;
	private ImageButton atlasBtn;

	private Skin skin;

	private FileChooser fileChooser;

	public ProjectToolbar(Skin skin) {
		super(skin);
		this.skin = skin;
		left();
		newBtn = new ImageButton(skin);
		saveBtn = new ImageButton(skin);
		loadBtn = new ImageButton(skin);
		packageBtn = new ImageButton(skin);
		exitBtn = new ImageButton(skin);
		playBtn = new ImageButton(skin);
		assetsBtn = new ImageButton(skin);
		atlasBtn = new ImageButton(skin);

		fileChooser = new FileChooser(Mode.OPEN);
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);

		addToolBarButton(skin, newBtn, "ic_new", "New", "Create a new project");
		addToolBarButton(skin, loadBtn, "ic_load", "Load", "Load an existing project");
		addToolBarButton(skin, saveBtn, "ic_save", "Save", "Save the current project");
		addToolBarButton(skin, exitBtn, "ic_quit", "Exit", "Save changes and exits");
		row();

		addToolBarButton(skin, playBtn, "ic_play", "Play", "Play Adventure");
		addToolBarButton(skin, packageBtn, "ic_package", "Package", "Package the game for distribution");
		addToolBarButton(skin, assetsBtn, "ic_assets", "Assets", "Open assets folder");
		addToolBarButton(skin, atlasBtn, "ic_atlases", "Atlas", "Create Atlas");

		newBtn.setDisabled(false);
		loadBtn.setDisabled(false);
		exitBtn.setDisabled(false);

		playBtn.setDisabled(true);
		packageBtn.setDisabled(true);
		atlasBtn.setDisabled(true);
		assetsBtn.setDisabled(true);

		newBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveProjectAndExecute(new Runnable() {

					@Override
					public void run() {
						newProject();
					}
				});
			}
		});

		loadBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveProjectAndExecute(new Runnable() {
					@Override
					public void run() {
						loadProject();
					}
				});
			}
		});

		exitBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveProjectAndExecute(new Runnable() {
					@Override
					public void run() {
						exit();
					}
				});
			}
		});

		saveBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveProject();
			}
		});

		playBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				play();
			}
		});

		packageBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				packageProject();
			}
		});

		assetsBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				openAssetFolder();
			}
		});

		atlasBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				createAtlas();
			}
		});

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				saveBtn.setDisabled(!Ctx.project.isModified());
			}
		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				packageBtn.setDisabled(!Ctx.project.isLoaded());
				playBtn.setDisabled(!Ctx.project.isLoaded());
				assetsBtn.setDisabled(!Ctx.project.isLoaded());
				atlasBtn.setDisabled(!Ctx.project.isLoaded());
			}
		});
	}

	private void addToolBarButton(Skin skin, ImageButton button, String icon, String text, String tooltip) {
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ButtonStyle.class));
		TextureRegion image = Ctx.assetManager.getIcon(icon);
		style.imageUp = new TextureRegionDrawable(image);

		try {
			TextureRegion imageDisabled = Ctx.assetManager.getIcon(icon + "_disabled");

			if (imageDisabled != null)
				style.imageDisabled = new TextureRegionDrawable(imageDisabled);
		} catch (Exception e) {

		}

		button.setStyle(style);
		// button.row();
		// button.add(new Label(text, skin));

		add(button);
		button.setDisabled(true);
		TextTooltip t = new TextTooltip(tooltip, skin);
		button.addListener(t);
	}

	private void newProject() {
		CreateProjectDialog dialog = new CreateProjectDialog(skin);
		dialog.show(getStage());
	}

	@Override
	public void act(float delta) {
		super.act(delta);
	}

	private void loadProject() {
		fileChooser.setSelectionMode(SelectionMode.DIRECTORIES);
		getStage().addActor(fileChooser);

		fileChooser.setListener(new FileChooserListener() {

			@Override
			public void selected(Array<FileHandle> files) {

				final File f = files.get(0).file();

				try {
					EditorUtils.checkVersionAndLoadProject(f, getStage(), skin);
				} catch (Exception e) {
					EditorLogger.error("Error loading last project.", e);
					Ctx.project.closeProject();
				}

			}

			@Override
			public void canceled() {
			}
		});
	}

	public void exit() {
		Gdx.app.exit();
	}

	private void saveProject() {
		File file = Ctx.project.getProjectDir();

		if (file == null) {
			String msg = "Please create a new project first.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error saving project", msg);
		}
	}

	private void packageProject() {
		saveProject();

		new PackageDialog(skin).show(getStage());
	}

	private void play() {
		saveProject();

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {

				if (Ctx.project.getSelectedScene() == null) {
					String msg = "There are no scenes in this chapter.";
					Message.showMsg(getStage(), msg, 3);
					return;
				}

				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);

				try {
					Ctx.project.saveProject();
				} catch (Exception ex) {
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					Message.showMsgDialog(getStage(), "Error", msg);
					return;
				}

				Message.showMsg(stage, "Running scene...", 3);

				try {
					if (!RunProccess.runBladeEngine(Ctx.project.getProjectDir(), null, null,
							Boolean.parseBoolean(
									Ctx.project.getEditorConfig().getProperty("view.fullscreenPlay", "false"))))
						Message.showMsg(getStage(), "There was a problem running the project", 3);
				} catch (IOException e) {
					Message.showMsgDialog(stage, "Error", "There was a problem running the project: " + e.getMessage());
				}

			}
		}).start();

	}

	private void openAssetFolder() {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(new File(
						Ctx.project.getAssetPath(Ctx.project.getProjectDir().getAbsoluteFile().getAbsolutePath())));
			} catch (IOException e1) {
				String msg = "Something went wrong while opening assets folder.\n\n" + e1.getClass().getSimpleName()
						+ " - " + e1.getMessage();
				Message.showMsgDialog(getStage(), "Error", msg);
			}
		}
	}

	private void createAtlas() {
		new CreateAtlasDialog(skin).show(getStage());
	}

	private void saveProjectAndExecute(final Runnable task) {
		if (Ctx.project.isLoaded() && Ctx.project.isModified()) {
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

					task.run();
				}
			}.text("Save current project changes?").button("Yes", true).button("No", false).key(Keys.ENTER, true)
					.key(Keys.ESCAPE, false).show(getStage());
		} else {
			task.run();
		}
	}

}
