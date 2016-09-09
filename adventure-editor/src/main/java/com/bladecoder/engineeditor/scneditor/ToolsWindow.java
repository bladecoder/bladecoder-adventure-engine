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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.I18NUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.ModelTools;
import com.bladecoder.engineeditor.common.RunProccess;

import javafx.application.Platform;
import javafx.stage.FileChooser;

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

		table.pack();
		setActor(table);
		prefSize(table.getWidth(), Math.max(200, table.getHeight()));
		setSize(table.getWidth(), Math.max(200, table.getHeight()));
	}

	@SuppressWarnings("restriction")
	private void exportTSV() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				try {
					final FileChooser chooser = new FileChooser();
					chooser.setTitle("Select the file to export the project texts");
					final File outFile = chooser.showSaveDialog(null);

					if (outFile == null) {
						return;
					}

					I18NUtils.exportTSV(Ctx.project.getProjectDir().getAbsolutePath(), outFile.getAbsolutePath(),
							Ctx.project.getChapter().getId(), "default");

					Message.showMsg(getStage(), outFile.getName() + " exported sucessfully.", 4);
				} catch (IOException e) {
					Message.showMsg(getStage(), "There was a problem generating the .tsv file.", 4);
					EditorLogger.printStackTrace(e);
				}
			}
		});
	}

	@SuppressWarnings("restriction")
	private void importTSV() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					final FileChooser chooser = new FileChooser();
					chooser.setTitle("Select the .tsv file to import");
					final File inFile = chooser.showOpenDialog(null);
					if (inFile == null) {
						return;
					}

					I18NUtils.importTSV(Ctx.project.getProjectDir().getAbsolutePath(), inFile.getAbsolutePath(),
							Ctx.project.getChapter().getId(), "default");

					// Reload texts
					Ctx.project.getI18N().load(Ctx.project.getChapter().getId());

					Message.showMsg(getStage(), inFile.getName() + " imported sucessfully.", 4);

				} catch (IOException e) {
					Message.showMsg(getStage(), "There was a problem importing the .tsv file.", 4);
					EditorLogger.printStackTrace(e);
				}
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
