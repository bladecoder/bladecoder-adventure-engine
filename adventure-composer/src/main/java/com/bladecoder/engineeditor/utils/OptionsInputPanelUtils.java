package com.bladecoder.engineeditor.utils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class OptionsInputPanelUtils {
	public static String[] getIdFromNodeList(final boolean mandatory, final NodeList nodeList) {
		int l = nodeList.getLength();
		if(!mandatory) l++;
		String values[] = new String[l];

		if(!mandatory) {
			values[0] = "";
		}

		for(int i = 0; i < nodeList.getLength(); i++) {
			if(mandatory)
				values[i] = ((Element)nodeList.item(i)).getAttribute("id");
			else
				values[i+1] = ((Element)nodeList.item(i)).getAttribute("id");
		}
		return values;
	}
}
