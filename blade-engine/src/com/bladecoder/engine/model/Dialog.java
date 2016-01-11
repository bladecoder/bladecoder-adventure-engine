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
package com.bladecoder.engine.model;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class Dialog implements Serializable {

	public final static String DEFAULT_DIALOG_VERB = "dialog";

	private ArrayList<DialogOption> options = new ArrayList<DialogOption>();

	private int currentOption = -1;

	private String id;
	private String actor;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public Dialog selectOption(DialogOption o) {

		currentOption = options.indexOf(o);

		String v = o.getVerbId();

		if (v == null)
			v = DEFAULT_DIALOG_VERB;

		// TODO: DELETE REFERENCE TO WORLD FROM DIALOG
		CharacterActor a = (CharacterActor) World.getInstance().getCurrentScene().getActor(actor, false);
		a.runVerb(v);

		if (o.isOnce())
			o.setVisible(false);

		currentOption = -1;

		if (o.getNext() != null) {
			String next = o.getNext();

			if (next.equals("this"))
				return this;
			else
				return a.getDialog(next);
		}

		return null;
	}

	public void addOption(DialogOption o) {
		options.add(o);
	}

	public ArrayList<DialogOption> getOptions() {
		return options;
	}

	public ArrayList<DialogOption> getVisibleOptions() {
		ArrayList<DialogOption> visible = new ArrayList<DialogOption>();

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

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			json.writeValue("id", id);
			json.writeValue("actor", actor);
		} else {
			json.writeValue("currentOption", currentOption);
		}

		json.writeValue("options", options, DialogOption.class, DialogOption.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			id = json.readValue("id", String.class, jsonData);
			actor = json.readValue("actor", String.class, jsonData);
			options = json.readValue("options", ArrayList.class, DialogOption.class, jsonData);
		} else {
			JsonValue optionsValue = jsonData.get("options");

			int i = 0;

			for (DialogOption o : options) {
				o.read(json, optionsValue.get(i));
				i++;
			}
		}
	}
}
