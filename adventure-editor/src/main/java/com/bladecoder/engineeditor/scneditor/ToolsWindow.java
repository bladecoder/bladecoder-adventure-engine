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

import java.util.ArrayList;
import java.util.HashMap;

import com.bladecoder.engine.util.Config;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.LookAtAction;
import com.bladecoder.engine.actions.SayAction;
import com.bladecoder.engine.actions.SetCutmodeAction;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.utils.Message;
import com.bladecoder.engineeditor.utils.RunProccess;

public class ToolsWindow extends Container<Table> {

	ScnWidget scnWidget;
	com.bladecoder.engine.model.BaseActor actor;

	public ToolsWindow(Skin skin, ScnWidget sw) {

		scnWidget = sw;

		Table table = new Table(skin);
		TextButton button1 = new TextButton("Add Intelligent Cutmode", skin);
		TextButton button2 = new TextButton("Test in Android device", skin);

		table.top();
		table.add(new Label("Tools", skin, "big")).center();
		setActor(table);

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);

		table.row();
		table.add(button2).expandX().fill();
		
//		table.row();
//		table.add(button1).expandX().fill();

		// ADD CUTMODE FOR VERBS THAT HAVE ONLY A LOOKAT OR SAY ACTION
		button1.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				HashMap<String, Scene> scenes = World.getInstance().getScenes();

				for (Scene scn : scenes.values()) {
					HashMap<String, BaseActor> actors = scn.getActors();

					for (BaseActor a : actors.values()) {
						if (a instanceof InteractiveActor) {
							InteractiveActor ia = (InteractiveActor) a;

							HashMap<String, Verb> verbs = ia.getVerbManager().getVerbs();

							for (Verb v : verbs.values()) {
								ArrayList<Action> actions = v.getActions();

								// Don't process verbs for inventory
								if (v.getState() != null && v.getState().equalsIgnoreCase("INVENTORY"))
									continue;

								if (actions.size() == 1) {
									Action act = actions.get(0);

									if (act instanceof LookAtAction || act instanceof SayAction) {
										actions.clear();

										SetCutmodeAction cma1 = new SetCutmodeAction();
										SetCutmodeAction cma2 = new SetCutmodeAction();
										try {
											ActionUtils.setParam(cma1, "value", "true");
											ActionUtils.setParam(cma2, "value", "false");

										} catch (NoSuchFieldException | IllegalArgumentException
												| IllegalAccessException e) {
											e.printStackTrace();
										}

										actions.add(cma1);
										actions.add(act);
										actions.add(cma2);
									}
								}
							}
						}
					}
				}

				Ctx.project.setModified();
				Message.showMsg(getStage(), "VERBS PROCESSED SUSCESSFULLY", 4);

				event.cancel();
			}

		});

		// TEST IN ANDROID DEVICE
		button2.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

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
					String msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName()
							+ " - " + ex.getMessage();
					Message.showMsgDialog(getStage(), "Error", msg);
					return;
				}

				new Thread(new Runnable() {
					Stage stage = getStage();

					@Override
					public void run() {
						Message.showMsg(stage, "Running scene on Android device...", 5);

						if (!RunProccess.runGradle(Ctx.project.getProjectDir(), "android:installDebug android:run"))
							Message.showMsg(stage, "There was a problem running the project", 4);

					}
				}).start();
				
				Ctx.project.getProjectConfig().remove(Config.CHAPTER_PROP);			
				Ctx.project.getProjectConfig().remove(Config.TEST_SCENE_PROP);
				Ctx.project.setModified();
				event.cancel();
			}

		});

		prefSize(200, 200);
		setSize(200, 200);
	}
}
