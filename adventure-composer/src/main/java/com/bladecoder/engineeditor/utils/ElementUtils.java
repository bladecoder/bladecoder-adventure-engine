package com.bladecoder.engineeditor.utils;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;

public class ElementUtils {
	public static String getCheckedId(String id, String[] values) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;
		
		String [] nl = values;

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
		json.setOutputType(OutputType.javascript);

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		String str = json.toJson(e, (Class<?>)null);

		return json.fromJson(e.getClass(), str);
	}
}
