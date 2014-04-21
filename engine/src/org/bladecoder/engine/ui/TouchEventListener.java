package org.bladecoder.engine.ui;

interface TouchEventListener {
	
	public static final int TOUCH_DOWN = 0;
	public static final int TOUCH_UP = 1;
	public static final int DRAG = 2;

	public void touchEvent(int type, float x, float y, int pointer, int button);

}
