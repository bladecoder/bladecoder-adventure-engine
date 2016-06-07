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
package com.bladecoder.engine.actions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Param {
	public enum Type {
		STRING, BOOLEAN, FLOAT, INTEGER, VECTOR2, VECTOR3, DIMENSION, ACTOR, CHARACTER_ACTOR, INTERACTIVE_ACTOR, SPRITE_ACTOR, SCENE, CHAPTER, FILE, OPTION, SCENE_ACTOR, ACTOR_ANIMATION, LAYER, EDITABLE_OPTION,
		SCENE_CHARACTER_ACTOR, SCENE_INTERACTIVE_ACTOR, SCENE_SPRITE_ACTOR, TEXT, SMALL_TEXT, BIG_TEXT, COLOR, SOUND, TEXT_STYLE, NOT_SET
	}
	
	public static final String NUMBER_PARAM_SEPARATOR = ",";
	public static final String STRING_PARAM_SEPARATOR = "#";

	public final String name;
	public final String desc;
	public final Type type;
	public final boolean mandatory;
	public final String defaultValue;
	public final Object[] options; // availables values for combos

	public Param(String name, String desc, Type type, boolean mandatory, String defaultValue, Object[] options) {
		this.name = name;
		this.desc = desc;
		this.type = type;
		this.mandatory = mandatory;
		this.defaultValue = defaultValue;
		this.options = options;
	}
	
	public Param(String name, String desc, Type type, boolean mandatory, String defaultValue) {
		this(name, desc, type, mandatory, defaultValue, null);
	}
	
	public Param(String name, String desc, Type type, boolean mandatory) {
		this(name, desc, type, mandatory, null, null);
	}
	
	public Param(String name, String desc, Type type) {
		this(name, desc, type, false, null, null);
	}

	public static Vector2 parseVector2(String s) {
		
		if(s==null || s.isEmpty())
			return null;
		
		Vector2 v = null;

		int idx = s.indexOf(NUMBER_PARAM_SEPARATOR.charAt(0));

		if (idx != -1) {
			try {
				float x = Float.parseFloat(s.substring(0,idx));
				float y = Float.parseFloat(s.substring(idx + 1));

				v = new Vector2(x, y);
			} catch (Exception e) {

			}
		}

		return v;
	}

	public static Vector3 parseVector3(String s) {
		Vector3 v = null;

		int idx = s.indexOf(NUMBER_PARAM_SEPARATOR.charAt(0));
		int idx2 = s.lastIndexOf(NUMBER_PARAM_SEPARATOR.charAt(0));

		if (idx != -1 && idx2 != -1 && idx != idx2) {
			try {
				float x = Float.parseFloat(s.substring(0,idx));
				float y = Float.parseFloat(s.substring(idx + 1, idx2));
				float z = Float.parseFloat(s.substring(idx2 + 1));

				v = new Vector3(x, y, z);
			} catch (Exception e) {

			}
		}

		return v;
	}
	
	public static void parsePolygon(Polygon p, String s) {
		
		String[] vs = s.split(NUMBER_PARAM_SEPARATOR);
		
		if(vs.length < 6)
			return;
		
		float verts[] = new float[vs.length];
		
		for(int i = 0; i < vs.length; i++) {
			verts[i] = Float.parseFloat(vs[i]);
		}
		
		p.setVertices(verts);

	}
	
	public static void parsePolygon(Polygon p, String v, String pos) {
		parsePolygon(p, v);
		Vector2 v2 = parseVector2(pos);
		p.setPosition(v2.x, v2.y);
	}
	
	public static String toStringParam(Polygon p) {
		StringBuilder sb = new StringBuilder();
		float[]verts = p.getVertices();
		
		sb.append(verts[0]);
		
		for(int i = 1; i < verts.length; i++) {
			sb.append(NUMBER_PARAM_SEPARATOR);
			sb.append(verts[i]);	
		}
		
		return sb.toString();
	}

	public static Color parseColor(String color) {
		if (color == null || color.trim().isEmpty()) {
			return null; // the default color in the style will be used
		}

		switch (color.trim()) {
			case "black":
				return Color.BLACK;
			case "white":
				return Color.WHITE;
			default:
				return Color.valueOf(color);
		}
	}

	public static String toStringParam(Vector2 v) {
		if(v == null)
			return null;
		
		return v.x + NUMBER_PARAM_SEPARATOR + v.y;
	}

	public static String toStringParam(Vector3 v) {
		return v.x + NUMBER_PARAM_SEPARATOR + v.y + NUMBER_PARAM_SEPARATOR + v.z;
	}
}
