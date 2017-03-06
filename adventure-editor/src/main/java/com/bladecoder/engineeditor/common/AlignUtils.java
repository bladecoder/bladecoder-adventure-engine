package com.bladecoder.engineeditor.common;

import com.badlogic.gdx.utils.Align;

public class AlignUtils {
	
	public static String getAlign(int align) {
		switch(align) {
		case Align.bottomRight:
			return "botton-right";
		case Align.bottomLeft:
			return "botton-left";
		case Align.topRight:
			return "top-right";
		case Align.topLeft:
			return "top-left";
		case Align.right:
			return "right";
		case Align.left:
			return "left";
		case Align.bottom:
			return "botton";
		case Align.top:
			return "top";
		case Align.center:
			return "center";
		}
		
		return "";
	}
	
	public static int getAlign(String s) {
		if("botton-right".equals(s))
			return Align.bottomRight;
		else if("botton-left".equals(s))
			return Align.bottomLeft;
		else if("top-right".equals(s))
			return Align.topRight;
		else if("top-left".equals(s))
			return Align.topLeft;
		else if("right".equals(s))
			return Align.right;
		else if("left".equals(s))
			return Align.left;
		else if("botton".equals(s))
			return Align.bottom;
		else if("top".equals(s))
			return Align.top;
		else if("center".equals(s))
			return Align.center;
		
		return 0;
	}
}
