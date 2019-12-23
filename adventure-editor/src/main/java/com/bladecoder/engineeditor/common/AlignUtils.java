package com.bladecoder.engineeditor.common;

import com.badlogic.gdx.utils.Align;

public class AlignUtils {

	public static String getAlign(int align) {
		switch (align) {
		case Align.bottomRight:
			return "bottom-right";
		case Align.bottomLeft:
			return "bottom-left";
		case Align.topRight:
			return "top-right";
		case Align.topLeft:
			return "top-left";
		case Align.right:
			return "right";
		case Align.left:
			return "left";
		case Align.bottom:
			return "bottom";
		case Align.top:
			return "top";
		case Align.center:
			return "center";
		}

		return "";
	}

	public static int getAlign(String s) {
		if ("bottom-right".equals(s))
			return Align.bottomRight;
		else if ("bottom-left".equals(s))
			return Align.bottomLeft;
		else if ("top-right".equals(s))
			return Align.topRight;
		else if ("top-left".equals(s))
			return Align.topLeft;
		else if ("right".equals(s))
			return Align.right;
		else if ("left".equals(s))
			return Align.left;
		else if ("bottom".equals(s))
			return Align.bottom;
		else if ("top".equals(s))
			return Align.top;
		else if ("center".equals(s))
			return Align.center;

		return 0;
	}
}
