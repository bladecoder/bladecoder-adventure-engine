package com.bladecoder.engineeditor.utils;

import java.io.StringWriter;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;
import com.bladecoder.engine.util.ActionUtils;

public class ElementUtils {
	public static String getCheckedId(String id, String[] values) {
		boolean checked = false;
		int i = 1;

		String idChecked = id;

		String[] nl = values;

		while (!checked) {
			checked = true;

			for (int j = 0; j < nl.length; j++) {
				String id2 = nl[j];

				if (id2.equals(idChecked)) {
					i++;
					idChecked = id + i;
					checked = false;
					break;
				}
			}
		}

		return idChecked;
	}

	public static Object cloneElement(Object e) {
		Json json = new Json();

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		if (e instanceof Action) {
			StringWriter buffer = new StringWriter();
			json.setWriter(buffer);
			ActionUtils.writeJson((Action) e, json);
			String str = buffer.toString();
			EditorLogger.debug(str);
			JsonValue root = new JsonReader().parse(str);
			return ActionUtils.readJson(json, root);
		} else {

			String str = json.toJson(e, (Class<?>) null);
			return json.fromJson(e.getClass(), str);
		}
	}
}
