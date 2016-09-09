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
package com.bladecoder.engineeditor.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.LookAtAction;
import com.bladecoder.engine.actions.SayAction;
import com.bladecoder.engine.actions.SetCutmodeAction;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.Ctx;

public class ModelTools {
	public static final void extractDialogs() {
		HashMap<String, Scene> scenes = World.getInstance().getScenes();

		BufferedWriter writer = null;
		try {
			// create a temporary file
			File dFile = new File("BONASERA-DIALOGS.md");

			writer = new BufferedWriter(new FileWriter(dFile));

			writer.write("# DIALOGS - " + (Ctx.project.getTitle() != null ? Ctx.project.getTitle().toUpperCase() : "")
					+ "\n\n");

			for (Scene scn : scenes.values()) {
				HashMap<String, BaseActor> actors = scn.getActors();

				writer.write("\n## SCENE: " + scn.getId() + "\n\n");

				HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();

				// Process SayAction of TALK type
				for (Verb v : verbs.values()) {
					ArrayList<Action> actions = v.getActions();

					for (Action act : actions) {

						if (act instanceof SayAction) {
							String type = ActionUtils.getStringValue(act, "type");

							if ("TALK".equals(type)) {
								String actor = ActionUtils.getStringValue(act, "actor").toUpperCase();
								String rawText = ActionUtils.getStringValue(act, "text");
								String text = Ctx.project.translate(rawText).replace("\\n\\n", "\n").replace("\\n",
										"\n");

								writer.write(actor + ": " + text + "\n");
							}
						}
					}
				}

				for (BaseActor a : actors.values()) {
					if (a instanceof InteractiveActor) {
						InteractiveActor ia = (InteractiveActor) a;

						verbs = ia.getVerbManager().getVerbs();

						// Process SayAction of TALK type
						for (Verb v : verbs.values()) {
							ArrayList<Action> actions = v.getActions();

							for (Action act : actions) {

								if (act instanceof SayAction) {
									String type = ActionUtils.getStringValue(act, "type");

									if ("TALK".equals(type)) {
										String actor = ActionUtils.getStringValue(act, "actor").toUpperCase();
										String rawText = ActionUtils.getStringValue(act, "text");
										String text = Ctx.project.translate(rawText).replace("\\n\\n", "\n")
												.replace("\\n", "\n");

										writer.write(actor + ": " + text + "\n");
									}
								}
							}
						}
					}

					if (a instanceof CharacterActor) {
						CharacterActor ca = (CharacterActor) a;

						HashMap<String, Dialog> dialogs = ca.getDialogs();

						if (dialogs == null)
							continue;

						// Process SayAction of TALK type
						for (Dialog d : dialogs.values()) {
							ArrayList<DialogOption> options = d.getOptions();

							if (options.size() > 0)
								writer.write("\n**" + ca.getId().toUpperCase() + " - " + d.getId() + "**\n\n");

							for (DialogOption o : options) {
								String text = o.getText();
								String response = o.getResponseText();

								writer.write(scn.getPlayer().getId().toUpperCase() + ": "
										+ Ctx.project.translate(text).replace("\\n\\n", "\n").replace("\\n", "\n")
										+ "\n");

								if (response != null)
									writer.write(ca.getId().toUpperCase() + ": " + Ctx.project.translate(response)
											.replace("\\n\\n", "\n").replace("\\n", "\n") + "\n\n");
							}
						}
					}
				}
			}

		} catch (Exception e) {
			EditorLogger.printStackTrace(e);
		} finally {
			try {
				// Close the writer regardless of what happens...
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	public static final void addCutMode() {
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

								} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
									EditorLogger.printStackTrace(e);
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
	}

	public static final void fixSaySubtitleActor() {
		HashMap<String, Scene> scenes = World.getInstance().getScenes();

		for (Scene scn : scenes.values()) {
			HashMap<String, BaseActor> actors = scn.getActors();
			
			HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();
			
			for (Verb v : verbs.values()) {
				ArrayList<Action> actions = v.getActions();

				for (Action act : actions) {

					if (act instanceof SayAction) {
						try {

							String stringValue = ActionUtils.getStringValue(act, "type");

							if (stringValue.equals("SUBTITLE"))
								ActionUtils.setParam(act, "actor", "$PLAYER");
						} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
							EditorLogger.printStackTrace(e);
							return;
						}
					}
				}
			}

			for (BaseActor a : actors.values()) {
				if (a instanceof InteractiveActor) {
					InteractiveActor ia = (InteractiveActor) a;

					verbs = ia.getVerbManager().getVerbs();

					for (Verb v : verbs.values()) {
						ArrayList<Action> actions = v.getActions();

						for(Action act: actions) {

							if (act instanceof SayAction) {
								try {

									String stringValue = ActionUtils.getStringValue(act, "type");

									if (stringValue.equals("SUBTITLE"))
										ActionUtils.setParam(act, "actor", "$PLAYER");
								} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
									EditorLogger.printStackTrace(e);
									return;
								}
							}
						}
					}
				}
			}
		}

		Ctx.project.setModified();
	}

}
