package com.bladecoder.engine.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.bladecoder.engine.model.World;

public class ScriptManager {
	private static ScriptManager instance = null;
	
	private final World world;
	private Globals globals = JsePlatform.standardGlobals();

	
	protected ScriptManager(World world) {
		this.world = world;
	}
	
	public static final ScriptManager getInstance() {
		if(instance == null) {
			instance = new ScriptManager(World.getInstance());
		}
		
		return instance;
	}
	
	public void eval(String script) {
		LuaValue chunk = globals.load(script);
		
		chunk.call();
	}
}
