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

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.I18NUtils;
import com.bladecoder.engineeditor.common.ImageUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.ModelTools;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.model.Project;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooser.SelectionMode;
import com.kotcrab.vis.ui.widget.file.FileChooser.ViewMode;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;

public class ToolsWindow extends Container<Table> {

	ScnWidget scnWidget;
	com.bladecoder.engine.model.BaseActor actor;

	public ToolsWindow(Skin skin, ScnWidget sw) {

		scnWidget = sw;

		Table table = new Table(skin);
		TextButton tmpButton = new TextButton("Temporal tool", skin, "no-toggled");
		TextButton testOnAndroidButton = new TextButton("Test on Android device", skin, "no-toggled");
		TextButton testOnIphoneEmulatorButton = new TextButton("Test on Iphone emulator", skin, "no-toggled");
		TextButton testOnIpadEmulatorButton = new TextButton("Test on Ipad emulator", skin, "no-toggled");
		TextButton testOnIOSDeviceButton = new TextButton("Test on IOS device", skin, "no-toggled");
		TextButton exportTSVButton = new TextButton("I18N - Export texts as .tsv", skin, "no-toggled");
		TextButton importTSVButton = new TextButton("I18N - Import.tsv file", skin, "no-toggled");
		
		TextButton exportUIImages = new TextButton("Export UI Images", skin, "no-toggled");
		TextButton createUIAtlas = new TextButton("Create UI Atlas", skin, "no-toggled");

		table.defaults().left().expandX();
		table.top().pad(DPIUtils.getSpacing() / 2);
		table.add(new Label("Tools", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);

		table.row();
		table.add(testOnAndroidButton).expandX().fill();

		table.row();
		table.add(testOnIphoneEmulatorButton).expandX().fill();

		table.row();
		table.add(testOnIpadEmulatorButton).expandX().fill();

		table.row();
		table.add(testOnIOSDeviceButton).expandX().fill();

		// disable if not mac
		if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
			testOnIphoneEmulatorButton.setDisabled(true);
			testOnIpadEmulatorButton.setDisabled(true);
			testOnIOSDeviceButton.setDisabled(true);
		}

		table.row();
		table.add(exportTSVButton).expandX().fill();

		table.row();
		table.add(importTSVButton).expandX().fill();
		
		table.row();
		table.add(exportUIImages).expandX().fill();
		
		table.row();
		table.add(createUIAtlas).expandX().fill();

		// table.row();
		// table.add(tmpButton).expandX().fill();

		// ADD CUTMODE FOR VERBS THAT ONLY HAVE A LOOKAT OR SAY ACTION
		tmpButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ModelTools.fixSaySubtitleActor();

				Message.showMsg(getStage(), "TOOL PROCESSED", 4);
			}

		});

		// TEST ON ANDROID DEVICE
		testOnAndroidButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				testOnAndroid();
			}

		});

		testOnIphoneEmulatorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				testOnIphoneEmulator();
			}

		});

		testOnIpadEmulatorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				testOnIpadEmulator();
			}

		});

		testOnIOSDeviceButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				testOnIOSDevice();
			}

		});

		exportTSVButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				exportTSV();
			}
		});

		importTSVButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				importTSV();
			}
		});
		
		exportUIImages.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				exportUIImages();
			}
		});
		
		createUIAtlas.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				createUIAtlas();
			}
		});

		table.pack();
		setActor(table);
		prefSize(table.getWidth(), Math.max(200, table.getHeight()));
		setSize(table.getWidth(), Math.max(200, table.getHeight()));
	}

	protected void createUIAtlas() {
		FileChooser fileChooser = new FileChooser(Mode.OPEN);
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);
		
		fileChooser.setSelectionMode(SelectionMode.DIRECTORIES);
		getStage().addActor(fileChooser);

		fileChooser.setListener(new FileChooserListener() {

			@Override
			public void selected(Array<FileHandle> files) {
				try {
//					fileChooser.setTitle("Select the file to export the project texts");
				
					ImageUtils.createAtlas(files.get(0).file().getAbsolutePath(), 
							Ctx.project.getProjectPath() + "/" + Project.UI_PATH + "/1/", "ui", 1, TextureFilter.Linear, 
								TextureFilter.Nearest);

					Message.showMsg(getStage(), "UI Atlas created sucessfully.", 4);
				} catch (IOException e) {
					Message.showMsg(getStage(), "There was a problem creating the UI Atlas.", 4);
					EditorLogger.printStackTrace(e);
				}				
			}

			@Override
			public void canceled() {
				
			}
		});
		
	}

	protected void exportUIImages() {
		FileChooser fileChooser = new FileChooser(Mode.OPEN);
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);
		
		fileChooser.setSelectionMode(SelectionMode.DIRECTORIES);
		getStage().addActor(fileChooser);

		fileChooser.setListener(new FileChooserListener() {

			@Override
			public void selected(Array<FileHandle> files) {
				try {
//					fileChooser.setTitle("Select the file to export the project texts");
					
					ImageUtils.unpackAtlas(new File(Ctx.project.getProjectPath() + "/" + Project.UI_PATH + "/1/ui.atlas"), 
							files.get(0).file());

					Message.showMsg(getStage(), "UI Atlas images exported sucessfully.", 4);
				} catch (Exception e) {
					Message.showMsg(getStage(), "There was a problem exporting images from UI Atlas.", 4);
					EditorLogger.printStackTrace(e);
				}				
			}

			@Override
			public void canceled() {
				
			}
		});
		
	}

	private void exportTSV() {
		
		FileChooser fileChooser = new FileChooser(Mode.SAVE);
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);
		
		fileChooser.setSelectionMode(SelectionMode.FILES);
		getStage().addActor(fileChooser);

		fileChooser.setListener(new FileChooserListener() {

			@Override
			public void selected(Array<FileHandle> files) {
				try {
//					fileChooser.setTitle("Select the file to export the project texts");

					I18NUtils.exportTSV(Ctx.project.getProjectDir().getAbsolutePath(), files.get(0).file().getAbsolutePath(),
							Ctx.project.getChapter().getId(), "default");

					Message.showMsg(getStage(), files.get(0).file().getName() + " exported sucessfully.", 4);
				} catch (IOException e) {
					Message.showMsg(getStage(), "There was a problem generating the .tsv file.", 4);
					EditorLogger.printStackTrace(e);
				}				
			}

			@Override
			public void canceled() {
				
			}
		});
	}

	private void importTSV() {
		
		FileChooser fileChooser = new FileChooser(Mode.OPEN);
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);
		
		fileChooser.setSelectionMode(SelectionMode.FILES);
		getStage().addActor(fileChooser);

		fileChooser.setListener(new FileChooserListener() {

			@Override
			public void selected(Array<FileHandle> files) {
				try {
//					chooser.setTitle("Select the .tsv file to import");

					I18NUtils.importTSV(Ctx.project.getProjectDir().getAbsolutePath(), files.get(0).file().getAbsolutePath(),
							Ctx.project.getChapter().getId(), "default");

					// Reload texts
					Ctx.project.getI18N().load(Ctx.project.getChapter().getId());

					Message.showMsg(getStage(), files.get(0).file().getName() + " imported sucessfully.", 4);

				} catch (IOException e) {
					Message.showMsg(getStage(), "There was a problem importing the .tsv file.", 4);
					EditorLogger.printStackTrace(e);
				}		
			}

			@Override
			public void canceled() {
				
			}
		});
	}

	private void testOnAndroid() {
		if (Ctx.project.getSelectedScene() == null) {
			String msg = "There are no scenes in this chapter.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		Ctx.project.getProjectConfig().setProperty(Config.CHAPTER_PROP, Ctx.project.getChapter().getId());
		Ctx.project.getProjectConfig().setProperty(Config.TEST_SCENE_PROP, Ctx.project.getSelectedScene().getId());
		Ctx.project.setModified();

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error", msg);
			return;
		}

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {
				Message.showMsg(stage, "Running scene on Android device...", 5);

				if (!RunProccess.runGradle(Ctx.project.getProjectDir(),
						"android:uninstallDebug android:installDebug android:run")) {
					Message.showMsg(stage, "There was a problem running the project", 4);
				}

				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);
				Ctx.project.setModified();

				try {
					Ctx.project.saveProject();
				} catch (Exception ex) {
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					EditorLogger.error(msg);
					return;
				}

			}
		}).start();

	}

	private void testOnIphoneEmulator() {
		if (Ctx.project.getSelectedScene() == null) {
			String msg = "There are no scenes in this chapter.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		Ctx.project.getProjectConfig().setProperty(Config.CHAPTER_PROP, Ctx.project.getChapter().getId());
		Ctx.project.getProjectConfig().setProperty(Config.TEST_SCENE_PROP, Ctx.project.getSelectedScene().getId());
		Ctx.project.setModified();

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error", msg);
			return;
		}

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {
				Message.showMsg(stage, "Running scene on Iphone emulator...", 5);

				if (!RunProccess.runGradle(Ctx.project.getProjectDir(), "ios:launchIPhoneSimulator")) {
					Message.showMsg(stage, "There was a problem running the project", 4);
				}

				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);
				Ctx.project.setModified();

				try {
					Ctx.project.saveProject();
				} catch (Exception ex) {
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					EditorLogger.error(msg);
					return;
				}

			}
		}).start();

	}

	private void testOnIpadEmulator() {
		if (Ctx.project.getSelectedScene() == null) {
			String msg = "There are no scenes in this chapter.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		Ctx.project.getProjectConfig().setProperty(Config.CHAPTER_PROP, Ctx.project.getChapter().getId());
		Ctx.project.getProjectConfig().setProperty(Config.TEST_SCENE_PROP, Ctx.project.getSelectedScene().getId());
		Ctx.project.setModified();

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error", msg);
			return;
		}

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {
				Message.showMsg(stage, "Running scene on Ipad simulator...", 5);

				if (!RunProccess.runGradle(Ctx.project.getProjectDir(), "ios:launchIPadSimulator")) {
					Message.showMsg(stage, "There was a problem running the project", 4);
				}

				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);
				Ctx.project.setModified();

				try {
					Ctx.project.saveProject();
				} catch (Exception ex) {
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					EditorLogger.error(msg);
					return;
				}

			}
		}).start();

	}

	private void testOnIOSDevice() {
		if (Ctx.project.getSelectedScene() == null) {
			String msg = "There are no scenes in this chapter.";
			Message.showMsg(getStage(), msg, 3);
			return;
		}

		Ctx.project.getProjectConfig().setProperty(Config.CHAPTER_PROP, Ctx.project.getChapter().getId());
		Ctx.project.getProjectConfig().setProperty(Config.TEST_SCENE_PROP, Ctx.project.getSelectedScene().getId());
		Ctx.project.setModified();

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
					+ ex.getMessage();
			Message.showMsgDialog(getStage(), "Error", msg);
			return;
		}

		new Thread(new Runnable() {
			Stage stage = getStage();

			@Override
			public void run() {
				Message.showMsg(stage, "Running scene on IOS device...", 5);

				if (!RunProccess.runGradle(Ctx.project.getProjectDir(), "ios:launchIOSDevice")) {
					Message.showMsg(stage, "There was a problem running the project", 4);
				}

				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);
				Ctx.project.setModified();

				try {
					Ctx.project.saveProject();
				} catch (Exception ex) {
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					EditorLogger.error(msg);
					return;
				}

			}
		}).start();

	}
}
