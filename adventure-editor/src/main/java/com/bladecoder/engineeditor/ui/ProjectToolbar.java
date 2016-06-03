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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.utils.Message;
import com.bladecoder.engineeditor.utils.RunProccess;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

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

		newBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				newProject();
			}
		});

		loadBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				loadProject();
			}
		});

		exitBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				exit();
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
				packageBtn.setDisabled(Ctx.project.getProjectDir() == null);
				playBtn.setDisabled(Ctx.project.getProjectDir() == null);
				assetsBtn.setDisabled(Ctx.project.getProjectDir() == null);
				atlasBtn.setDisabled(Ctx.project.getProjectDir() == null);
			}
		});
	}

	private void addToolBarButton(Skin skin, ImageButton button, String icon, String text, String tooltip) {
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ButtonStyle.class));
		TextureRegion image = Ctx.assetManager.getIcon(icon);
		style.imageUp = new TextureRegionDrawable(image);

		try {
			TextureRegion imageDisabled = Ctx.assetManager.getIcon(icon + "_disabled");
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
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				final DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Select the project to load");
				chooser.setInitialDirectory(
						Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."));

				final File dir = chooser.showDialog(null);
				if (dir == null) {
					return;
				}

				Message.showMsg(getStage(), "Loading project...", true);
		
				Timer.post(new Task() {
					@Override
					public void run() {
						try {
							Ctx.project.saveProject();
							Ctx.project.loadProject(dir);
							playBtn.setDisabled(false);
							packageBtn.setDisabled(false);
							Message.showMsg(getStage(), null);

						} catch (Exception ex) {
							if (ex.getCause() != null && ex.getCause().getCause() != null
									&& ex.getCause().getCause() instanceof ClassNotFoundException) {
								String msg = "The game have custom actions that can not be loaded. Probably the game needs to be compiled. Trying 'gradlew compile'...";
								Message.showMsg(getStage(), msg, true);
								Timer.post(new Task() {
									@Override
									public void run() {
										if (RunProccess.runGradle(Ctx.project.getProjectDir(), "desktop:compileJava")) {
											try {
												Ctx.project.loadProject(dir);
												playBtn.setDisabled(false);
												packageBtn.setDisabled(false);
												Message.showMsg(getStage(), "Project loaded Successfully", 3);
											} catch (IOException e) {
												String msg = e.getClass().getSimpleName() + " - " + e.getMessage();
												Message.hideMsg();
												Message.showMsgDialog(getStage(), "Error loading project", msg);
											}
										} else {
											Message.hideMsg();
											Message.showMsgDialog(getStage(),  "Error loading project", "error running 'gradlew desktop:compileJava'");
										}
									}
								});
							} else {

								String msg = ex.getClass().getSimpleName() + " - " + ex.getMessage();
								Message.hideMsg();
								Message.showMsgDialog(getStage(), "Error loading project", msg);
							}

							ex.printStackTrace();
						}
					}
				});
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
					if (!RunProccess.runBladeEngine(Ctx.project.getProjectDir(), null, null))
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
				Desktop.getDesktop()
						.open(new File(Ctx.project.getProjectDir().getAbsoluteFile() + Project.ASSETS_PATH));
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

}
