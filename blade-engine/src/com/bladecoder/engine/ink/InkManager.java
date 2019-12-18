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
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.InkList;
import com.bladecoder.ink.runtime.ListDefinition;
import com.bladecoder.ink.runtime.Story;

public class InkManager implements Serializable {
	public final static int KEY_SIZE = 10;
	public final static char NAME_VALUE_TAG_SEPARATOR = ':';
	public final static char NAME_VALUE_PARAM_SEPARATOR = '=';
	private final static String PARAM_SEPARATOR = ",";
	public final static char COMMAND_MARK = '>';

	private ResourceBundle i18n;

	private Story story = null;
	private ExternalFunctions externalFunctions;

	private ActionCallback cb;

	// Depending on the reading order of Inventory, InkManager and Actor verbs,
	// the verbCallbacks may not exist. So, we search the Cb lazily when needed.
	private String sCb;

	private boolean wasInCutmode;

	private String storyName;

	private final World w;

	private InkVerbRunner inkVerbRunner = new InkVerbRunner();

	private Thread loaderThread;

	public InkManager(World w) {
		this.w = w;
		externalFunctions = new ExternalFunctions();
	}

	public void newStory(final String name) throws Exception {
		loadThreaded(name, null);
	}

	private void loadStory(String name) {
		try {
			FileHandle asset = EngineAssetManager.getInstance()
					.getAsset(EngineAssetManager.MODEL_DIR + name + EngineAssetManager.INK_EXT);

			long initTime = System.currentTimeMillis();

			String json = getJsonString(asset.read());
			story = new Story(json);

			externalFunctions.bindExternalFunctions(w, this);

			storyName = name;

			loadI18NBundle();

			EngineLogger.debug("INK STORY LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));

		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story: " + name + " " + e.getMessage());
			story = null;
			storyName = null;
		}
	}

	private void loadStoryState(String stateString) {
		try {
			long initTime = System.currentTimeMillis();
			story.getState().loadJson(stateString);
			EngineLogger.debug("INK *SAVED STATE* LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story State for: " + storyName + " " + e.getMessage());
		}
	}

	public void loadI18NBundle() {
		if (getStoryName() != null
				&& EngineAssetManager.getInstance().getModelFile(storyName + "-ink.properties").exists())
			i18n = w.getI18N().getBundle(EngineAssetManager.MODEL_DIR + storyName + "-ink", true);
	}

	public String translateLine(String line) {
		if (line.charAt(0) == I18N.PREFIX) {
			String key = line.substring(1);

			// In ink, several keys can be included in the same line.
			String[] keys = key.split("@");

			String translated = "";

			for (String k : keys) {
				try {
					// some untranslated words may follow the key
					String k2 = k.substring(0, 10);
					translated += i18n.getString(k2);
					if (k.length() > 10) {
						String trailing = k.substring(10);
						translated += trailing;
					}
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

	public String getVariable(String name) {
		return story.getVariablesState().get(name).toString();
	}

	public boolean compareVariable(String name, String value) {
		waitIfNotLoaded();

		if (story.getVariablesState().get(name) instanceof InkList) {
			return ((InkList) story.getVariablesState().get(name)).ContainsItemNamed(value);
		} else {
			return story.getVariablesState().get(name).toString().equals(value == null ? "" : value);
		}
	}

	public void setVariable(String name, String value) throws Exception {
		waitIfNotLoaded();

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
		waitIfNotLoaded();

		String line = null;

		// We create a new InkVerbRunner every ink loop to avoid pending cb.resume() to
		// execute in the new actions
		inkVerbRunner.cancel();
		inkVerbRunner = new InkVerbRunner();

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

		if (inkVerbRunner.getActions().size() > 0) {
			inkVerbRunner.run(null, null);
		} else {

			if (hasChoices()) {
				wasInCutmode = w.inCutMode();
				w.setCutMode(false);
				w.getListener().dialogOptions();
			} else if (cb != null || sCb != null) {
				if (cb == null) {
					cb = ActionCallbackSerializer.find(w, w.getCurrentScene(), sCb);
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
		} else {

			// for backward compatibility
			if ("action".equals(commandName)) {
				commandName = commandParams[0].trim();
				params.remove(commandName);
			}

			// Some preliminar validation to see if it's an action
			if (commandName.length() > 0) {
				// Try to create action by default
				Action action;

				try {

					Class<?> c = ActionFactory.getClassTags().get(commandName);

					if (c == null && commandName.indexOf('.') == -1) {
						commandName = "com.bladecoder.engine.actions." + commandName + "Action";
					}

					action = ActionFactory.create(commandName, params);
					action.init(w);
					inkVerbRunner.getActions().add(action);
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
				action = ActionFactory.create("Text", params);
			} else {
				action = ActionFactory.create("Say", params);
			}

			action.init(w);
			inkVerbRunner.getActions().add(action);
		} catch (ClassNotFoundException | ReflectionException e) {
			EngineLogger.error(e.getMessage(), e);
		}
	}

	public Story getStory() {
		return story;
	}

	public void runPath(String path, ActionCallback cb) throws Exception {
		waitIfNotLoaded();

		if (story == null) {
			EngineLogger.error("Ink Story not loaded!");
			return;
		}

		this.cb = cb;

		story.choosePathString(path);
		continueMaximally();
	}

	public boolean hasChoices() {
		waitIfNotLoaded();

		return (story != null && inkVerbRunner.getActions().size() == 0 && story.getCurrentChoices().size() > 0);
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

	public void selectChoice(int i) {
		w.setCutMode(wasInCutmode);

		try {
			story.chooseChoiceIndex(i);
			continueMaximally();
		} catch (Exception e) {
			EngineLogger.error(e.getMessage(), e);
		}
	}

	public String getStoryName() {
		return storyName;
	}

	public void setStoryName(String storyName) {
		this.storyName = storyName;
	}

	private void waitIfNotLoaded() {
		if (loaderThread != null && loaderThread.isAlive()) {
			EngineLogger.debug(">>> Loader thread not finished. Waiting for it!!!");
			try {
				loaderThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private void loadThreaded(final String name, final String state) {
		EngineLogger.debug("LOADING INK STORY: " + name + (state == null ? "" : " WITH SAVED STATE."));
		loaderThread = new Thread() {
			@Override
			public void run() {
				if (name != null)
					loadStory(name);

				if (state != null)
					loadStoryState(state);
			}
		};

		loaderThread.start();
	}

	public InkVerbRunner getVerbRunner() {
		return inkVerbRunner;
	}

	@Override
	public void write(Json json) {
		BladeJson bjson = (BladeJson) json;
		World w = bjson.getWorld();

		json.writeValue("storyName", storyName);

		if (bjson.getMode() == Mode.STATE) {
			json.writeValue("wasInCutmode", wasInCutmode);

			if (cb == null && sCb != null)
				cb = ActionCallbackSerializer.find(w, w.getCurrentScene(), sCb);

			if (cb != null)
				json.writeValue("cb", ActionCallbackSerializer.find(w, w.getCurrentScene(), cb));

			// SAVE ACTIONS
			json.writeArrayStart("actions");
			for (Action a : inkVerbRunner.getActions()) {
				ActionUtils.writeJson(a, json);
			}
			json.writeArrayEnd();

			json.writeValue("ip", inkVerbRunner.getIP());

			json.writeArrayStart("actionsSer");
			for (Action a : inkVerbRunner.getActions()) {
				if (a instanceof Serializable) {
					json.writeObjectStart();
					((Serializable) a).write(json);
					json.writeObjectEnd();
				}
			}
			json.writeArrayEnd();

			// SAVE STORY
			if (story != null) {
				try {
					json.writeValue("story", story.getState().toJson());
				} catch (Exception e) {
					EngineLogger.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		BladeJson bjson = (BladeJson) json;
		World w = bjson.getWorld();

		final String name = json.readValue("storyName", String.class, jsonData);

		if (bjson.getMode() == Mode.MODEL) {
			story = null;
			storyName = name;

			// Only load in new game.
			// If the SAVED_GAME_VERSION property exists we are loading a saved
			// game and we will load the story in the STATE mode.
			if (bjson.getInit()) {
				loadThreaded(name, null);
			}
		} else {
			wasInCutmode = json.readValue("wasInCutmode", Boolean.class, jsonData);
			sCb = json.readValue("cb", String.class, jsonData);

			// READ ACTIONS
			JsonValue actionsValue = jsonData.get("actions");

			inkVerbRunner = new InkVerbRunner();

			for (int i = 0; i < actionsValue.size; i++) {
				JsonValue aValue = actionsValue.get(i);

				Action a = ActionUtils.readJson(w, json, aValue);
				inkVerbRunner.getActions().add(a);
			}

			inkVerbRunner.setIP(json.readValue("ip", Integer.class, jsonData));

			actionsValue = jsonData.get("actionsSer");

			int i = 0;

			for (Action a : inkVerbRunner.getActions()) {
				if (a instanceof Serializable && i < actionsValue.size) {
					if (actionsValue.get(i) == null)
						break;

					((Serializable) a).read(json, actionsValue.get(i));
					i++;
				}
			}

			// READ STORY
			final String storyString = json.readValue("story", String.class, jsonData);
			if (storyString != null) {
				loadThreaded(name, storyString);
			}
		}
	}

	public final class InkVerbRunner implements VerbRunner {

		private ArrayList<Action> actions;
		private int ip = -1;
		private boolean cancelled = false;

		public InkVerbRunner() {
			actions = new ArrayList<>();
		}

		@Override
		public void resume() {
			ip++;

			nextStep();
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
			cancelled = true;
			ip = actions.size();
		}

		@Override
		public String getCurrentTarget() {
			return null;
		}

		private void nextStep() {
			if (cancelled)
				return;

			if (ip < 0) {
				continueMaximally();
			} else {
				boolean stop = false;

				while (ip < actions.size() && !stop && !cancelled) {
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

	}
}
