package com.bladecoder.engine.ink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Text.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Story;

public class InkManager implements VerbRunner, Serializable {

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
			newStory(asset.read());
			this.storyName = storyName;
		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story: " + storyName + " " + e.getMessage());
		}
	}

	private void nextLine() {
		if (story.canContinue()) {

			String line = null;

			try {
				do {
					actions.clear();
					line = story.Continue();

					if (!line.isEmpty()) {
						// Remove trailing '\n'
						line = line.substring(0, line.length() - 1);

						EngineLogger.debug("INK LINE: " + line);

						List<String> tags = story.getCurrentTags();
						processLine(tags, line);
					} else {
						EngineLogger.debug("INK EMPTY LINE!");
					}

				} while (line.isEmpty());
			} catch (Exception e) {
				EngineLogger.error(e.getMessage(), e);
			}

		} else if (hasChoices()) {
			wasInCutmode = World.getInstance().inCutMode();
			World.getInstance().setCutMode(false);
		} else if(cb != null){
			ActionCallbackQueue.add(cb);
		}
	}

	private HashMap<String, String> processTags(List<String> tags) {

		HashMap<String, String> tagsMap = new HashMap<String, String>();

		for (String t : tags) {
			String key;
			String value;

			int i = t.indexOf(':');
			if (i != -1) {
				key = t.substring(0, i).trim();
				value = t.substring(i + 1, t.length()).trim();
			} else {
				key = t.trim();
				value = null;
			}

			EngineLogger.debug("TAG: " + key + " value: " + value);

			tagsMap.put(key, value);
		}

		return tagsMap;
	}

	private void processLine(List<String> tags, String line) {

		HashMap<String, String> params = processTags(tags);

		if (!params.containsKey("actor") && World.getInstance().getCurrentScene().getPlayer() != null) {
			params.put("actor", Scene.VAR_PLAYER);

			if (!params.containsKey("type")) {
				params.put("type", Type.TALK.toString());
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

		run();
	}

	private void nextStep() {
		if (ip >= actions.size() || ip < 0) {
			nextLine();
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
		nextLine();
	}

	public boolean hasChoices() {
		return story.getCurrentChoices().size() > 0;
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
			nextLine();
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

				story.getState().loadJson(storyString);
			} catch (Exception e) {
				EngineLogger.error(e.getMessage(), e);
			}
		}
	}
}
