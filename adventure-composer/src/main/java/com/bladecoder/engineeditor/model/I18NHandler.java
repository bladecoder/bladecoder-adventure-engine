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
package com.bladecoder.engineeditor.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.DisableActionAction;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class I18NHandler {
	public static final String WORLD_VERBS_PREFIX = "default.";

	private String modelPath;
	private String worldFilename;
	private String chapterFilename;

	private Properties i18nWorld;
	private Properties i18nChapter;

	public I18NHandler(String modelPath) {
		this.worldFilename = EngineAssetManager.WORLD_FILENAME_JSON;
		this.modelPath = modelPath;

		if (!modelPath.endsWith("/"))
			this.modelPath = modelPath + "/";

		i18nWorld = loadI18N(worldFilename);
	}

	private String getI18NFilename(String modelFilename) {
		return modelPath + modelFilename.substring(0, modelFilename.lastIndexOf('.')) + ".properties";
	}

	public void load(String chapterId) {
		this.chapterFilename = chapterId + ".chapter";
		i18nChapter = loadI18N(chapterFilename);
	}

	@SuppressWarnings("serial")
	private Properties loadI18N(String modelFilename) {
		String i18nFilename = getI18NFilename(modelFilename);

		// To save in alphabetical order we override the keys method
		Properties i18n = new Properties() {
			@Override
			public synchronized Enumeration<Object> keys() {
				return Collections.enumeration(new TreeSet<Object>(keySet()));
			}
		};

		try {
			i18n.load(new FileInputStream(i18nFilename));
		} catch (IOException e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nFilename);
		}

		return i18n;
	}

	public String getWorldTranslation(String key) {
		if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX || i18nWorld == null)
			return key;

		return i18nWorld.getProperty(key.substring(1), key);
	}

	/**
	 * Returns the translation from chapter property
	 * 
	 * @param key
	 * @return
	 */
	public String getTranslation(String key) {
		if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX || i18nChapter == null)
			return key;

		return i18nChapter.getProperty(key.substring(1), key);
	}

	public void setTranslation(String key, String value) {
		if (key.charAt(0) != I18N.PREFIX) {
			if (value == null || value.equals(""))
				i18nChapter.remove(key);
			else
				i18nChapter.setProperty(key, value);
		} else {
			if (value == null || value.equals(""))
				i18nChapter.remove(key.substring(1));
			else
				i18nChapter.setProperty(key.substring(1), value);
		}
	}

	public void setWorldTranslation(String key, String value) {
		if (key.charAt(0) != I18N.PREFIX)
			i18nWorld.setProperty(key, value);
		else
			i18nWorld.setProperty(key.substring(1), value);
	}

	private void save(String filename, Properties p) {
		String i18nFilename = getI18NFilename(filename);

		 deleteUnusedKeys();

		try {
			FileOutputStream os = new FileOutputStream(i18nFilename);
			Writer out = new OutputStreamWriter(os, "ISO-8859-1");
			p.store(out, filename);
		} catch (IOException e) {
			EditorLogger.error("ERROR WRITING BUNDLE: " + i18nFilename);
		}
	}

	public void save() throws FileNotFoundException {
		save(worldFilename, i18nWorld);
		save(chapterFilename, i18nChapter);
	}

	public void putTranslationsInElement(Scene scn) {

		HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();

		for (Verb v : verbs.values())
			putTranslationsInElement(v);

		for (BaseActor a : scn.getActors().values()) {
			putTranslationsInElement(a);
		}
	}

	public void putTranslationsInElement(BaseActor a) {
		if (a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) a;

			// 1. DESC attribute
			ia.setDesc(getTranslation(ia.getDesc()));

			// 2. ACTIONS
			HashMap<String, Verb> verbs = ia.getVerbManager().getVerbs();

			for (Verb v : verbs.values())
				putTranslationsInElement(v);

			// 3. DIALOGS
			if (a instanceof CharacterActor) {
				HashMap<String, Dialog> dialogs = ((CharacterActor) a).getDialogs();

				if (dialogs != null) {
					for (Dialog d : dialogs.values())
						putTranslationsInElement(d);
				}
			}
		}
	}

	public void putTranslationsInElement(Verb v) {
		ArrayList<Action> actions = v.getActions();

		for (Action a : actions) {
			putTranslationsInElement(a);
		}
	}

	public void putTranslationsInElement(Action a) {
		String[] names = ActionUtils.getFieldNames(a);

		for (String name : names) {
			if (name.toLowerCase().endsWith("text")) {
				
				try {
					String value = getTranslation(ActionUtils.getStringValue(a, name));
					ActionUtils.setParam(a, name, value);
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					EditorLogger.error(e.getMessage());
				}
			}
		}
	}

	public void putTranslationsInElement(Dialog d) {
		ArrayList<DialogOption> options = d.getOptions();

		for (DialogOption o : options) {
			putTranslationsInElement(o);
		}
	}

	public void putTranslationsInElement(DialogOption o) {
		o.setText(getTranslation(o.getText()));
		o.setResponseText(getTranslation(o.getResponseText()));
	}

	public void extractStrings(Scene scn) {
		HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();

		for (Verb v : verbs.values())
			extractStrings(I18N.PREFIX + scn.getId(), v);

		for (BaseActor a : scn.getActors().values()) {
			extractStrings(I18N.PREFIX + scn.getId(), a);
		}
	}

	public void extractStrings(String baseString, BaseActor a) {
		if (a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) a;

			// 1. DESC attribute
			if (ia.getDesc() != null && ia.getDesc().charAt(0) != I18N.PREFIX) {
				String key = baseString + "." + a.getId() + ".desc";
				String value = ia.getDesc();
				ia.setDesc(key);
				setTranslation(key, value);
			}

			// 2. ACTIONS
			HashMap<String, Verb> verbs = ia.getVerbManager().getVerbs();

			for (Verb v : verbs.values())
				extractStrings(baseString + "." + a.getId(), v);

			// 3. DIALOGS
			if (a instanceof CharacterActor) {
				HashMap<String, Dialog> dialogs = ((CharacterActor) a).getDialogs();

				if (dialogs != null)
					for (Dialog d : dialogs.values())
						extractStrings(baseString + "." + a.getId(), d);
			}
		}
	}

	public void extractStrings(String baseString, Verb v) {
		ArrayList<Action> actions = v.getActions();

		for (int i = 0; i < actions.size(); i++) {
			Action a = actions.get(i);

			extractStrings(baseString + "." + v.getHashKey() + "." + i, a);
		}
	}

	public void extractStrings(String baseString, Dialog d) {
		ArrayList<DialogOption> options = d.getOptions();

		for (int i = 0; i < options.size(); i++) {
			DialogOption o = options.get(i);

			extractStrings(baseString + "." + d.getId() + "." + i, o);
		}
	}

	public void extractStrings(String baseString, DialogOption o) {
		if (o.getText() != null && o.getText().charAt(0) != I18N.PREFIX) {
			String key = baseString + ".text";
			String value = o.getText();
			o.setText(key);
			setTranslation(key, value);
		}

		if (o.getResponseText() != null && o.getResponseText().charAt(0) != I18N.PREFIX) {
			String key = baseString + ".responseText";
			String value = o.getResponseText();
			o.setResponseText(key);
			setTranslation(key, value);
		}
	}

	public void extractStrings(String baseString, Action a) {
		String[] names = ActionUtils.getFieldNames(a);

		for (String name : names) {
			if (name.toLowerCase().endsWith("text")) {
				String key = baseString + "." + name;
				
				try {
					String value = ActionUtils.getStringValue(a, name);
					ActionUtils.setParam(a, name, key);
					setTranslation(key, value);
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					EditorLogger.error(e.getMessage());
				}
			}
		}
	}

	private void deleteUnusedKeys() {
		ArrayList<String> usedKeys = new ArrayList<String>();
		
		// SCENES
		for(Scene s: World.getInstance().getScenes().values())
			getUsedKeys(s, usedKeys);
		
		Enumeration<Object> keys = i18nChapter.keys();

		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();

			// Doesn't remove ui keys
			if (!usedKeys.contains(key) && !key.startsWith("ui.")) { 
				EditorLogger.debug("Removing translation key: " + key);
				i18nChapter.remove(key);
			}
		}
		
		usedKeys.clear();
		// WORLD VERBS
		for(Verb v: World.getInstance().getVerbManager().getVerbs().values())
			getUsedKeys(v, usedKeys);
		
		keys = i18nWorld.keys();

		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();

			// Doesn't remove ui keys
			if (!usedKeys.contains(key) && !key.startsWith("ui.")) {
				EditorLogger.debug("Removing translation key: " + key);
				i18nWorld.remove(key);
			}
		}

	}

	private void getUsedKeys(Scene s, ArrayList<String> usedKeys) {
		for(Verb v: s.getVerbManager().getVerbs().values())
			getUsedKeys(v, usedKeys);

		for(BaseActor a: s.getActors().values()) {
			if(a instanceof InteractiveActor) {
				InteractiveActor ia = (InteractiveActor) a;
				
				if(ia.getDesc() != null && !ia.getDesc().isEmpty() && ia.getDesc().charAt(0) == I18N.PREFIX)
					usedKeys.add(ia.getDesc().substring(1));
				
				for(Verb v: ia.getVerbManager().getVerbs().values())
					getUsedKeys(v, usedKeys);
				
				if(a instanceof CharacterActor) {
					CharacterActor ca = (CharacterActor) a;
					
					if(ca.getDialogs() != null) {
						for(Dialog d: ca.getDialogs().values()) {
							for(DialogOption o: d.getOptions()) {
								if(o.getText() != null && !o.getText().isEmpty() && o.getText().charAt(0) == I18N.PREFIX)
									usedKeys.add(o.getText().substring(1));
								
								if(o.getResponseText() != null && !o.getResponseText().isEmpty() && o.getResponseText().charAt(0) == I18N.PREFIX)
									usedKeys.add(o.getResponseText().substring(1));
							}
						}
					}
				}
			}
		}
	}
	
	private void getUsedKeys(Verb v, ArrayList<String> usedKeys) {
		for(Action a:v.getActions()) {
			
			if(a instanceof DisableActionAction)
				a = ((DisableActionAction) a).getAction();
			
			String[] fieldNames = ActionUtils.getFieldNames(a);
			
			for(String name: fieldNames) {
				if(name.toLowerCase().endsWith("text")) {
					String value = null;
					
					try {
						value = ActionUtils.getStringValue(a, name);
					} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
						EditorLogger.error(e.getMessage());
					}
					
					if (value != null && !value.isEmpty() && value.charAt(0) == I18N.PREFIX) {
						usedKeys.add(value.substring(1));
					}
				}
			}
		}
	}
}
