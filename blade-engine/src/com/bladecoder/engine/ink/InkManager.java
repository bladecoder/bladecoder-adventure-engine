package com.bladecoder.engine.ink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Text.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.InkList;
import com.bladecoder.ink.runtime.ListDefinition;
import com.bladecoder.ink.runtime.Story;

public class InkManager implements VerbRunner, Serializable {
	public final static char NAME_VALUE_TAG_SEPARATOR = ':';
	public final static char NAME_VALUE_PARAM_SEPARATOR = '=';
	private final static String PARAM_SEPARATOR = ",";
	public final static char COMMAND_MARK = '>';

	private static ResourceBundle i18n;

	private Story story = null;
	private ExternalFunctions externalFunctions;

	private ActionCallback cb;

	// Depending on the reading order of Inventory, InkManager and Actor verbs,
	// the verbCallbacks may not exist. So, we search the Cb lazily when needed.
	private String sCb;

	private ArrayList<Action> actions;

	private boolean wasInCutmode;

	private String storyName;

	private int ip = -1;

	private final World w;

	public InkManager(World w) {
		this.w = w;
		externalFunctions = new ExternalFunctions();
		actions = new ArrayList<>();
	}

	public void newStory(InputStream is) throws Exception {

		String json = getJsonString(is);
		story = new Story(json);

		externalFunctions.bindExternalFunctions(w, this);
	}

	public void newStory(final String name) throws Exception {
		new Thread() {
			@Override
			public void run() {
				loadStory(name, null);
			}
		}.start();

		// Start the child thread now to avoid calling setVariable() before the thread
		// starts.
		Thread.yield();
	}

	synchronized private void loadStory(String name, String stateString) {
		FileHandle asset = EngineAssetManager.getInstance()
				.getAsset(EngineAssetManager.MODEL_DIR + name + EngineAssetManager.INK_EXT);

		try {
			long initTime = System.currentTimeMillis();
			newStory(asset.read());

			storyName = name;

			loadI18NBundle();

			EngineLogger.debug("INK STORY LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));

			if (stateString != null) {
				initTime = System.currentTimeMillis();
				story.getState().loadJson(stateString);
				EngineLogger.debug("INK SAVED STATE LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
			}

		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story: " + name + " " + e.getMessage());
			story = null;
			storyName = null;
		}
	}

	public void loadI18NBundle() {
		if (storyName != null && EngineAssetManager.getInstance().getModelFile(storyName + "-ink.properties").exists())
			i18n = I18N.getBundle(EngineAssetManager.MODEL_DIR + storyName + "-ink", true);
	}

	public String translateLine(String line) {
		if (line.charAt(0) == I18N.PREFIX) {
			String key = line.substring(1);

			// In ink, several keys can be included in the same line.
			String[] keys = key.split("@");

			String translated = "";

			for (String k : keys) {
				try {
					translated += i18n.getString(k);
				} catch (Exception e) {
					EngineLogger.error("MISSING TRANSLATION KEY: " + key);
					return key;
				}
			}

			// In translated lines, spaces can be escaped with '_'.
			translated = translated.replace('_', ' ');

			return translated;
		}

		return line;
	}

	public synchronized String getVariable(String name) {
		return story.getVariablesState().get(name).toString();
	}

	public synchronized boolean compareVariable(String name, String value) {
		if (story.getVariablesState().get(name) instanceof InkList) {
			return ((InkList) story.getVariablesState().get(name)).ContainsItemNamed(value);
		} else {
			return story.getVariablesState().get(name).toString().equals(value);
		}
	}

	public synchronized void setVariable(String name, String value) throws Exception {
		if (story.getVariablesState().get(name) instanceof InkList) {

			InkList rawList = (InkList) story.getVariablesState().get(name);

			if (rawList.getOrigins() == null) {
				List<String> names = rawList.getOriginNames();
				if (names != null) {
					ArrayList<ListDefinition> origins = new ArrayList<>();
					for (String n : names) {
						ListDefinition def = story.getListDefinitions().getListDefinition(n);
						if (!origins.contains(def))
							origins.add(def);
					}
					rawList.setOrigins(origins);
				}
			}

			rawList.addItem(value);
		} else
			story.getVariablesState().set(name, value);
	}

	private void continueMaximally() {
		String line = null;
		actions.clear();

		HashMap<String, String> currentLineParams = new HashMap<>();

		while (story.canContinue()) {
			try {
				line = story.Continue();
				currentLineParams.clear();

				// Remove trailing '\n'
				if (!line.isEmpty())
					line = line.substring(0, line.length() - 1);

				if (!line.isEmpty()) {
					if (EngineLogger.debugMode())
						EngineLogger.debug("INK LINE: " + translateLine(line));

					processParams(story.getCurrentTags(), currentLineParams);

					// PROCESS COMMANDS
					if (line.charAt(0) == COMMAND_MARK) {
						processCommand(currentLineParams, line);
					} else {
						processTextLine(currentLineParams, line);
					}
				} else {
					EngineLogger.debug("INK EMPTY LINE!");
				}
			} catch (Exception e) {
				EngineLogger.error(e.getMessage(), e);
			}

			if (story.getCurrentErrors() != null && !story.getCurrentErrors().isEmpty()) {
				EngineLogger.error(story.getCurrentErrors().get(0));
			}

		}

		if (actions.size() > 0) {
			run(null, null);
		} else {

			if (hasChoices()) {
				wasInCutmode = w.inCutMode();
				w.setCutMode(false);
				w.getListener().dialogOptions();
			} else if (cb != null || sCb != null) {
				if (cb == null) {
					cb = ActionCallbackSerializer.find(w, sCb);
				}

				ActionCallback tmpcb = cb;
				cb = null;
				tmpcb.resume();
			}
		}
	}

	private void processParams(List<String> input, HashMap<String, String> output) {

		for (String t : input) {
			String key;
			String value;

			int i = t.indexOf(NAME_VALUE_TAG_SEPARATOR);

			// support ':' and '=' as param separator
			if (i == -1)
				i = t.indexOf(NAME_VALUE_PARAM_SEPARATOR);

			if (i != -1) {
				key = t.substring(0, i).trim();
				value = t.substring(i + 1, t.length()).trim();
			} else {
				key = t.trim();
				value = null;
			}

			EngineLogger.debug("PARAM: " + key + " value: " + value);

			output.put(key, value);
		}
	}

	private void processCommand(HashMap<String, String> params, String line) {
		String commandName = null;
		String commandParams[] = null;

		int i = line.indexOf(NAME_VALUE_TAG_SEPARATOR);

		if (i == -1) {
			commandName = line.substring(1).trim();
		} else {
			commandName = line.substring(1, i).trim();
			commandParams = line.substring(i + 1).split(PARAM_SEPARATOR);

			processParams(Arrays.asList(commandParams), params);
		}

		if ("leave".equals(commandName)) {
			boolean init = true;

			if (params.get("init") != null)
				init = Boolean.parseBoolean(params.get("init"));

			w.setCurrentScene(params.get("scene"), init);
		} else if ("set".equals(commandName)) {
			w.setModelProp(params.get("prop"), params.get("value"));
		} else {

			// for backward compatibility
			if ("action".equals(commandName)) {
				commandName = commandParams[0].trim();
				params.remove(commandName);
			}

			// Some preliminar validation to see if it's an action
			if (commandName.length() > 0 && Character.isUpperCase(commandName.charAt(0))) {
				// Try to create action by default
				Action action;

				try {
					action = ActionFactory.createByClass("com.bladecoder.engine.actions." + commandName + "Action",
							params);
					action.init(w);
					actions.add(action);
				} catch (ClassNotFoundException | ReflectionException e) {
					EngineLogger.error(e.getMessage(), e);
				}

			} else {
				EngineLogger.error("Ink command not found: " + commandName);
			}
		}
	}

	private void processTextLine(HashMap<String, String> params, String line) {

		// Get actor name from Line. Actor is separated by ':'.
		// ej. "Johnny: Hello punks!"
		if (!params.containsKey("actor")) {
			int idx = line.indexOf(COMMAND_MARK);

			if (idx != -1) {
				params.put("actor", line.substring(0, idx).trim());
				line = line.substring(idx + 1).trim();
			}
		}

		if (!params.containsKey("actor") && w.getCurrentScene().getPlayer() != null) {
			// params.put("actor", Scene.VAR_PLAYER);

			if (!params.containsKey("type")) {
				params.put("type", Type.SUBTITLE.toString());
			}
		} else if (params.containsKey("actor") && !params.containsKey("type")) {
			params.put("type", Type.TALK.toString());
		} else if (!params.containsKey("type")) {
			params.put("type", Type.SUBTITLE.toString());
		}

		params.put("text", translateLine(line));

		try {
			Action action = null;

			if (!params.containsKey("actor")) {
				action = ActionFactory.createByClass("com.bladecoder.engine.actions.TextAction", params);
			} else {
				action = ActionFactory.createByClass("com.bladecoder.engine.actions.SayAction", params);
			}

			action.init(w);
			actions.add(action);
		} catch (ClassNotFoundException | ReflectionException e) {
			EngineLogger.error(e.getMessage(), e);
		}
	}

	private void nextStep() {
		if (ip < 0) {
			continueMaximally();
		} else {
			boolean stop = false;

			while (ip < actions.size() && !stop) {
				Action a = actions.get(ip);

				try {
					if (a.run(this))
						stop = true;
					else
						ip++;
				} catch (Exception e) {
					EngineLogger.error("EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName(), e);
					ip++;
				}
			}

			if (ip >= actions.size() && !stop)
				continueMaximally();
		}
	}

	public Story getStory() {
		return story;
	}

	synchronized public void runPath(String path, ActionCallback cb) throws Exception {
		if (story == null) {
			EngineLogger.error("Ink Story not loaded!");
			return;
		}

		this.cb = cb;

		story.choosePathString(path);
		continueMaximally();
	}

	public boolean hasChoices() {
		return (story != null && actions.size() == 0 && story.getCurrentChoices().size() > 0);
	}

	public List<String> getChoices() {

		List<Choice> options = story.getCurrentChoices();
		List<String> choices = new ArrayList<>(options.size());

		for (Choice o : options) {
			String line = o.getText();

			// the line maybe empty in default choices.
			if (line.isEmpty())
				continue;

			int idx = line.indexOf(InkManager.COMMAND_MARK);

			if (idx != -1) {
				line = line.substring(idx + 1).trim();
			}

			choices.add(translateLine(line));
		}

		return choices;
	}

	private String getJsonString(InputStream is) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			// Replace the BOM mark
			if (line != null)
				line = line.replace('\uFEFF', ' ');

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	@Override
	public void resume() {
		ip++;
		nextStep();
	}

	public void selectChoice(int i) {
		w.setCutMode(wasInCutmode);

		try {
			story.chooseChoiceIndex(i);
			continueMaximally();
		} catch (Exception e) {
			EngineLogger.error(e.getMessage(), e);
		}
	}

	@Override
	public ArrayList<Action> getActions() {
		return actions;
	}

	@Override
	public void run(String currentTarget, ActionCallback cb) {
		ip = 0;
		nextStep();
	}

	@Override
	public int getIP() {
		return ip;
	}

	@Override
	public void setIP(int ip) {
		this.ip = ip;
	}

	@Override
	public void cancel() {
		ArrayList<Action> actions = getActions();

		for (Action c : actions) {
			if (c instanceof VerbRunner)
				((VerbRunner) c).cancel();
		}

		ip = actions.size();
	}

	@Override
	public String getCurrentTarget() {
		return null;
	}

	@Override
	public void write(Json json) {
		World w = ((BladeJson) json).getWorld();

		json.writeValue("wasInCutmode", wasInCutmode);

		if (cb == null && sCb != null)
			cb = ActionCallbackSerializer.find(w, sCb);

		if (cb != null)
			json.writeValue("cb", ActionCallbackSerializer.find(w, cb));

		// SAVE ACTIONS
		json.writeArrayStart("actions");
		for (Action a : actions) {
			ActionUtils.writeJson(a, json);
		}
		json.writeArrayEnd();

		json.writeValue("ip", ip);

		json.writeArrayStart("actionsSer");
		for (Action a : actions) {
			if (a instanceof Serializable) {
				json.writeObjectStart();
				((Serializable) a).write(json);
				json.writeObjectEnd();
			}
		}
		json.writeArrayEnd();

		// SAVE STORY
		json.writeValue("storyName", storyName);

		if (story != null) {
			try {
				json.writeValue("story", story.getState().toJson());
			} catch (Exception e) {
				EngineLogger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		wasInCutmode = json.readValue("wasInCutmode", Boolean.class, jsonData);
		sCb = json.readValue("cb", String.class, jsonData);

		// READ ACTIONS
		actions.clear();
		JsonValue actionsValue = jsonData.get("actions");
		for (int i = 0; i < actionsValue.size; i++) {
			JsonValue aValue = actionsValue.get(i);

			Action a = ActionUtils.readJson(w, json, aValue);
			actions.add(a);
		}

		ip = json.readValue("ip", Integer.class, jsonData);

		actionsValue = jsonData.get("actionsSer");

		int i = 0;

		for (Action a : actions) {
			if (a instanceof Serializable && i < actionsValue.size) {
				if (actionsValue.get(i) == null)
					break;

				((Serializable) a).read(json, actionsValue.get(i));
				i++;
			}
		}

		// READ STORY
		String storyName = json.readValue("storyName", String.class, jsonData);
		String storyString = json.readValue("story", String.class, jsonData);
		if (storyString != null) {
			loadStory(storyName, storyString);
		}
	}
}
