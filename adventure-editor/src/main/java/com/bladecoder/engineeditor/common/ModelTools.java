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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.LookAtAction;
import com.bladecoder.engine.actions.SayAction;
import com.bladecoder.engine.actions.SetCutmodeAction;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;

public class ModelTools {
	public static final void extractDialogs() {
		Map<String, Scene> scenes = World.getInstance().getScenes();

		BufferedWriter writer = null;
		try {
			// create a temporary file
			File dFile = new File(Ctx.project.getProjectPath() + "/" + "DIALOGS.md");

			writer = new BufferedWriter(new FileWriter(dFile));

			writer.write("# DIALOGS - " + (Ctx.project.getTitle() != null ? Ctx.project.getTitle().toUpperCase() : "")
					+ "\n\n");

			for (Scene scn : scenes.values()) {
				Map<String, BaseActor> actors = scn.getActors();

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
		Map<String, Scene> scenes = World.getInstance().getScenes();

		for (Scene scn : scenes.values()) {
			Map<String, BaseActor> actors = scn.getActors();

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
		Map<String, Scene> scenes = World.getInstance().getScenes();

		for (Scene scn : scenes.values()) {
			Map<String, BaseActor> actors = scn.getActors();

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
				}
			}
		}

		Ctx.project.setModified();
	}

	public static final void checkI18NMissingKeys()
			throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Map<String, Scene> scenes = World.getInstance().getScenes();

		for (Scene scn : scenes.values()) {
			Map<String, BaseActor> actors = scn.getActors();

			// SCENE VERBS
			HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();

			for (Verb v : verbs.values()) {
				ArrayList<Action> actions = v.getActions();

				for (Action act : actions) {

					String[] params = ActionUtils.getFieldNames(act);

					for (String p : params) {
						String val = ActionUtils.getStringValue(act, p);

						if (val != null && !val.isEmpty() && val.charAt(0) == I18N.PREFIX) {
							String trans = Ctx.project.translate(val);

							if (trans == val) {
								EditorLogger.error("Key not found: " + val);
							}
						}
					}
				}
			}

			for (BaseActor a : actors.values()) {
				if (a instanceof InteractiveActor) {
					InteractiveActor ia = (InteractiveActor) a;

					// DESC
					if (ia.getDesc() != null && !ia.getDesc().isEmpty() && ia.getDesc().charAt(0) == I18N.PREFIX) {
						String trans = Ctx.project.translate(ia.getDesc());

						if (trans == ia.getDesc()) {
							EditorLogger.error("Key not found: " + ia.getDesc());
						}
					}

					// ACTOR VERBS
					verbs = ia.getVerbManager().getVerbs();

					for (Verb v : verbs.values()) {
						ArrayList<Action> actions = v.getActions();

						for (Action act : actions) {

							String[] params = ActionUtils.getFieldNames(act);

							for (String p : params) {
								String val = ActionUtils.getStringValue(act, p);

								if (val != null && !val.isEmpty() && val.charAt(0) == I18N.PREFIX) {
									String trans = Ctx.project.translate(val);

									if (trans == val) {
										EditorLogger.error("Key not found: " + val);
									}
								}
							}
						}
					}
				}

				// DIALOGS
				if (a instanceof CharacterActor) {
					HashMap<String, Dialog> dialogs = ((CharacterActor) a).getDialogs();

					if (dialogs != null) {
						for (Dialog d : dialogs.values()) {
							ArrayList<DialogOption> options = d.getOptions();

							for (DialogOption o : options) {

								if (o.getText() != null && !o.getText().isEmpty()
										&& o.getText().charAt(0) == I18N.PREFIX) {
									String trans = Ctx.project.translate(o.getText());

									if (trans == o.getText()) {
										EditorLogger.error("Key not found: " + o.getText());
									}
								}

								if (o.getResponseText() != null && !o.getResponseText().isEmpty()
										&& o.getResponseText().charAt(0) == I18N.PREFIX) {
									String trans = Ctx.project.translate(o.getResponseText());

									if (trans == o.getResponseText()) {
										EditorLogger.error("Key not found: " + o.getResponseText());
									}
								}
							}
						}
					}
				}
			}
		}

	}

	public static void printUnusedSounds() {
		ArrayList<String> unusedSounds = new ArrayList<String>(Arrays.asList(getSoundList()));

		HashMap<String, SoundDesc> sounds = World.getInstance().getSounds();

		if (sounds != null) {
			for (SoundDesc s : sounds.values()) {
				unusedSounds.remove(s.getFilename());
			}
		}

		for (String s : unusedSounds)
			EditorLogger.error(s);
	}

	public static String[] getSoundList() {
		String path = Ctx.project.getProjectPath() + Project.SOUND_PATH;

		File f = new File(path);

		String soundFiles[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".ogg") || arg1.endsWith(".wav") || arg1.endsWith(".mp3"))
					return true;

				return false;
			}
		});

		if(soundFiles == null)
			soundFiles = new String[0];
		
		Arrays.sort(soundFiles);

		return soundFiles;
	}

	public static void extractInkTexts(String story) throws IOException {
		String file = Ctx.project.getModelPath() + "/" + story + EngineAssetManager.INK_EXT;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		StringBuilder sb = new StringBuilder();

		try {
			String line = br.readLine();

			// Replace the BOM mark
			if (line != null)
				line = line.replace('\uFEFF', ' ');

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}

		} finally {
			br.close();
		}

		JsonValue root = new JsonReader().parse(sb.toString());

		StringBuilder tsvString = new StringBuilder();

		extractInkTextsInternal("ink." + story + ".", root, tsvString);
		FileUtils.writeStringToFile(new File(file + ".tsv"), tsvString.toString());

		String json = root.toJson(OutputType.json);
		FileUtils.writeStringToFile(new File(file + ".new"), json);

		Ctx.project.setModified();
	}

	private static void extractInkTextsInternal(String prefix, JsonValue v, StringBuilder sb) {
		if (v.isArray() || v.isObject()) {
			for (int i = 0; i < v.size; i++) {
				JsonValue aValue = v.get(i);

				extractInkTextsInternal(prefix, aValue, sb);
			}
		} else if (v.isString() && v.asString().charAt(0) == '^') {
			String value = v.asString().substring(1).trim();
			// String key = "ink." + value.hashCode();

			String key = prefix;

			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				byte[] bytes = value.getBytes(("UTF-8"));
				md.update(bytes);
				byte[] digest = md.digest();
				key += Base64Coder.encodeLines(digest).substring(0, 10);
			} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {

			}

			Ctx.project.getI18N().setTranslation(key, value);
			sb.append(key + "\t" + value + "\n");
			v.set("^" + I18N.PREFIX + key);
		}
	}

}
