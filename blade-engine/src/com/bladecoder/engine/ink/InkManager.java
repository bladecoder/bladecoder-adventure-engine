package com.bladecoder.engine.ink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.Text.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Story;

public class InkManager implements VerbRunner, Serializable {
	public final static char NAME_VALUE_TAG_SEPARATOR = ':';
	public final static char NAME_VALUE_PARAM_SEPARATOR = '=';
	private final static String PARAM_SEPARATOR = ",";
	private final static char COMMAND_MARK = '>';

	private Story story = null;
	private ExternalFunctions externalFunctions;

	private ActionCallback cb;

	private ArrayList<Action> actions;

	private boolean wasInCutmode;

	private String storyName;

	private int ip = -1;

	public InkManager() {
		externalFunctions = new ExternalFunctions();
		actions = new ArrayList<Action>();
	}

	public void newStory(InputStream is) throws Exception {

		String json = getJsonString(is);
		story = new Story(json);

		externalFunctions.bindExternalFunctions(this);
	}

	public void newStory(String storyName) throws Exception {
		FileHandle asset = EngineAssetManager.getInstance()
				.getAsset(EngineAssetManager.MODEL_DIR + storyName + EngineAssetManager.INK_EXT);

		try {
			long initTime = System.currentTimeMillis();
			newStory(asset.read());
			EngineLogger.debug("INK STORY LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));

			this.storyName = storyName;
		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story: " + storyName + " " + e.getMessage());
		}
	}
	
	public String getVariable(String name) {
		return story.getVariablesState().get(name).toString();
	}
	
	public void setVariable(String name, String value) throws Exception {
		story.getVariablesState().set(name, value);
	}

	private void continueMaximally() {
		String line = null;
		actions.clear();

		HashMap<String, String> currentLineParams = new HashMap<String, String>();

		while (story.canContinue()) {
			try {
				line = story.Continue();
				currentLineParams.clear();

				if (!line.isEmpty()) {
					// Remove trailing '\n'
					line = line.substring(0, line.length() - 1);

					EngineLogger.debug("INK LINE: " + line);

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
			
			if(story.getCurrentErrors() != null && !story.getCurrentErrors().isEmpty()) {
				EngineLogger.error(story.getCurrentErrors().get(0));
			}

		}

		if (actions.size() > 0) {
			run();
		} else {

			if (hasChoices()) {
				wasInCutmode = World.getInstance().inCutMode();
				World.getInstance().setCutMode(false);
			} else if (cb != null) {
				ActionCallbackQueue.add(cb);
			}
		}
	}

	private void processParams(List<String> input, HashMap<String, String> output) {

		for (String t : input) {
			String key;
			String value;

			int i = t.indexOf(NAME_VALUE_TAG_SEPARATOR);
			
			// support ':' and '=' as param separator
			if(i == -1)
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
			World.getInstance().setCurrentScene(params.get("scene"));
		} else if ("set".equals(commandName)) {
			World.getInstance().setModelProp(params.get("prop"), params.get("value"));
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
			int idx = line.indexOf(NAME_VALUE_TAG_SEPARATOR);

			if (idx != -1) {
				params.put("actor", line.substring(0, idx).trim());
				line = line.substring(idx + 1).trim();
			}
		}

		if (!params.containsKey("actor") && World.getInstance().getCurrentScene().getPlayer() != null) {
			// params.put("actor", Scene.VAR_PLAYER);

			if (!params.containsKey("type")) {
				params.put("type", Type.SUBTITLE.toString());
			}
		} else if (params.containsKey("actor") && !params.containsKey("type")) {
			params.put("type", Type.TALK.toString());
		} else if (!params.containsKey("type")) {
			params.put("type", Type.SUBTITLE.toString());
		}

		params.put("text", line);

		try {
			if (!params.containsKey("actor")) {
				Action action = ActionFactory.createByClass("com.bladecoder.engine.actions.TextAction", params);
				actions.add(action);
			} else {
				Action action = ActionFactory.createByClass("com.bladecoder.engine.actions.SayAction", params);
				actions.add(action);
			}
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

	public void run(String path, ActionCallback cb) throws Exception {
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

	public List<Choice> getChoices() {
		return story.getCurrentChoices();
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
		World.getInstance().setCutMode(wasInCutmode);

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
	public void run() {
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
	public String getTarget() {
		return null;
	}

	@Override
	public void write(Json json) {
		json.writeValue("wasInCutmode", wasInCutmode);
		json.writeValue("cb", ActionCallbackSerialization.find(cb));

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
		cb = ActionCallbackSerialization.find(json.readValue("cb", String.class, jsonData));

		// READ ACTIONS
		actions.clear();
		JsonValue actionsValue = jsonData.get("actions");
		for (int i = 0; i < actionsValue.size; i++) {
			JsonValue aValue = actionsValue.get(i);

			Action a = ActionUtils.readJson(json, aValue);
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
			try {
				newStory(storyName);

				long initTime = System.currentTimeMillis();
				story.getState().loadJson(storyString);
				EngineLogger.debug("INK SAVED STATE LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
			} catch (Exception e) {
				EngineLogger.error(e.getMessage(), e);
			}
		}
	}
}
