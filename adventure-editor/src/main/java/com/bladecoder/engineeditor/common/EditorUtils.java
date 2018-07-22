package com.bladecoder.engineeditor.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engineeditor.Ctx;

public final class EditorUtils {
	public static void checkVersionAndLoadProject(final File projectToLoad, final Stage stage, Skin skin)
			throws FileNotFoundException, IOException {

		if (!Ctx.project.checkVersion(projectToLoad)) {
			new Dialog("Update Engine", skin) {
				protected void result(Object object) {
					if (((Boolean) object).booleanValue()) {
						try {
							Ctx.project.updateEngineVersion(projectToLoad);
						} catch (IOException e) {
							String msg = "Something went wrong while updating the engine.\n\n"
									+ e.getClass().getSimpleName() + " - " + e.getMessage();
							Message.showMsgDialog(getStage(), "Error", msg);

							EditorLogger.error(msg, e);
						}
					}

					loadProjectWithCustomClasses(projectToLoad, stage);
				}
			}.text("Your game uses an old (" + Ctx.project.getProjectBladeEngineVersion(projectToLoad)
					+ ") Engine version. Do you want to update the engine?").button("Yes", true).button("No", false)
					.key(Keys.ENTER, true).key(Keys.ESCAPE, false).show(stage);
		} else {
			loadProjectWithCustomClasses(projectToLoad, stage);
		}
	}

	public static void loadProjectWithCustomClasses(final File projectToLoad, final Stage stage) {
		Message.showMsg(stage, "Loading project...", true);

		Timer.schedule(new Task() {

			@Override
			public void run() {
				try {
					Ctx.project.loadProject(projectToLoad);
					Message.hideMsg();
				} catch (Exception ex) {
					if (ex.getCause() != null && ex.getCause().getCause() != null
							&& ex.getCause().getCause() instanceof ClassNotFoundException) {
						String msg = "The game have custom actions that can not be loaded. Probably the game needs to be compiled. Trying 'gradlew compile'...";
						Message.showMsg(stage, msg, true);
						Timer.post(new Task() {
							@Override
							public void run() {
								if (RunProccess.runGradle(Ctx.project.getProjectDir(), "desktop:compileJava")) {
									try {
										Ctx.project.loadProject(projectToLoad);
										Message.showMsg(stage, "Project loaded Successfully", 3);
									} catch (IOException e) {
										String msg = e.getClass().getSimpleName() + " - " + e.getMessage();
										Message.hideMsg();
										Message.showMsgDialog(stage, "Error loading project", msg);
									}
								} else {
									Message.hideMsg();
									Message.showMsgDialog(stage, "Error loading project",
											"error running 'gradlew desktop:compileJava'");
								}
							}
						});
					} else {

						String msg = ex.getClass().getSimpleName() + " - " + ex.getMessage();
						Message.hideMsg();
						Message.showMsgDialog(stage, "Error loading project", msg);
					}

					EditorLogger.printStackTrace(ex);
				}
			}
		}, 0.05f);

	}
}
