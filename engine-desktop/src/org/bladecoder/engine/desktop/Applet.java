package org.bladecoder.engine.desktop;

import org.bladecoder.engine.BladeEngine;

import com.badlogic.gdx.backends.lwjgl.LwjglApplet;

public class Applet extends LwjglApplet {
	private static final long serialVersionUID = 1L;

	public Applet() {
		super(new BladeEngine());
	}
}
