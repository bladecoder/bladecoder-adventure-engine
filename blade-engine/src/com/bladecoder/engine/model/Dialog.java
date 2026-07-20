package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;

public class Dialog implements Serializable {

	public final static String DEFAULT_DIALOG_VERB = "dialog";

	private ArrayList<DialogOption> options = new ArrayList<>();

	private int currentOption = -1;

	private String id;
	private CharacterActor actor;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CharacterActor getActor() {
		return actor;
	}

	public void setActor(CharacterActor actor) {
		this.actor = actor;
	}

	public Dialog selectOption(int i) {
		return selectOption(getVisibleOptions().get(i));
	}

	/**
	 * @return The current visible options.
	 */
	public List<String> getChoices() {
		ArrayList<DialogOption> options = getVisibleOptions();
		List<String> choices = new ArrayList<>(options.size());

		for (DialogOption o : options) {
			choices.add(o.getText());
		}

		return choices;
	}

	public Dialog selectOption(DialogOption o) {

		currentOption = options.indexOf(o);

		String v = o.getVerbId();

		if (v == null)
			v = DEFAULT_DIALOG_VERB;

		actor.runVerb(v);

		if (o.isOnce())
			o.setVisible(false);

		currentOption = -1;

		if (o.getNext() != null) {
			String next = o.getNext();

			if (next.equals("this"))
				return this;
			else
				return actor.getDialog(next);
		}

		return null;
	}

	public void addOption(DialogOption o) {
		options.add(o);
	}

	public ArrayList<DialogOption> getOptions() {
		return options;
	}

	private ArrayList<DialogOption> getVisibleOptions() {
		ArrayList<DialogOption> visible = new ArrayList<>();

		for (DialogOption o : options) {
			if (o.isVisible())
				visible.add(o);
		}

		return visible;
	}

	public void reset() {
		currentOption = -1;
	}

	public int getNumVisibleOptions() {
		int num = 0;

		for (DialogOption o : getOptions()) {
			if (o.isVisible())
				num++;
		}

		return num;
	}

	public DialogOption getCurrentOption() {
		return currentOption == -1 ? null : options.get(currentOption);
	}

	@Override
	public void write(Json json) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			json.writeValue("id", id);
			// json.writeValue("actor", actor);
		} else {
			json.writeValue("currentOption", currentOption);
		}

		json.writeValue("options", options, DialogOption.class, DialogOption.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			id = json.readValue("id", String.class, jsonData);
			// actor = json.readValue("actor", String.class, jsonData);
			options = json.readValue("options", ArrayList.class, DialogOption.class, jsonData);
		} else {
			JsonValue optionsValue = jsonData.get("options");

			int i = 0;

			for (DialogOption o : options) {
				JsonValue jsonValue = optionsValue.get(i);

				if (jsonValue == null)
					break;

				o.read(json, jsonValue);
				i++;
			}

			currentOption = json.readValue("currentOption", int.class, jsonData);
		}
	}
}
