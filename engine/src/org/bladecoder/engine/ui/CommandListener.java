package org.bladecoder.engine.ui;

public interface CommandListener {
	
	public static final String RUN_VERB_COMMAND = "RUN_VERB_COMMAND";
	public static final String MENU_COMMAND = "CONFIG_COMMAND";
	public static final String BACK_COMMAND = "BACK_COMMAND";
	public static final String QUIT_COMMAND = "QUIT_COMMAND";

	public void runCommand(String command, Object param);

}
