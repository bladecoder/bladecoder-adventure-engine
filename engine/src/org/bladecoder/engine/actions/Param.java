package org.bladecoder.engine.actions;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Param {
	public enum Type {
		STRING, BOOLEAN, FLOAT, INTEGER, VECTOR2, VECTOR3, DIMENSION
	};

	public String name;
	public String desc;
	public Type type;
	public boolean mandatory;
	public String defaultValue;
	public String[] options; // availables values for combos

	public Param(String name, String desc, Type type, boolean mandatory, String defaultValue, String[] options) {
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
		
		if(s==null)
			return null;
		
		Vector2 v = null;

		int idx = s.indexOf(',');

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

		int idx = s.indexOf(',');
		int idx2 = s.lastIndexOf(',');

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
	
	public static Polygon parsePolygon(String s) {
		Polygon p = null;
		
		String[] vs = s.split(",");
		
		float verts[] = new float[vs.length];
		
		for(int i = 0; i < vs.length; i++) {
			verts[i] = Float.parseFloat(vs[i]);
		}
		
		p = new Polygon(verts);

		return p;
	}
	
	public static String toStringParam(Polygon p) {
		StringBuilder sb = new StringBuilder();
		float[]verts = p.getVertices();
		
		sb.append(verts[0]);
		
		for(int i = 1; i < verts.length; i++) {
			sb.append(',');
			sb.append(verts[i]);	
		}
		
		return sb.toString();
	}

	public static String toStringParam(Vector2 v) {
		return v.x + "," + v.y;
	}

	public static String toStringParam(Vector3 v) {
		return v.x + "," + v.y + "," + v.z;
	}
}
