package com.bladecoder.engine.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.bladecoder.engine.model.World;

public class ScriptManager {
	private static ScriptManager instance = null;

	private Globals globals = JsePlatform.standardGlobals();

	protected ScriptManager(World world) {	
		LuaValue w = CoerceJavaToLua.coerce(world);
        globals.set("world", w);
	}

	public static final ScriptManager getInstance() {
		if (instance == null) {
			instance = new ScriptManager(World.getInstance());
		}

		return instance;
	}

	public void eval(String script) {
		LuaValue chunk = globals.load(script);

		chunk.call();
	}
	
	public boolean evalIf(String expr) {
		LuaValue chunk = globals.load("return " + expr);

		LuaValue ret = chunk.call();
				
		return ret.toboolean();
	}
	
    /**
     * Call a function in the Lua script with the given parameters passed 
     */
    public void executeFunction(String functionName, Object... objects) {
    	executeFunctionParamsAsArray(functionName, objects);
    }

    /**
     * Now this function takes the parameters as an array instead, mostly meant
	 * so we can call other Lua script functions from Lua itself
	 */
	public void executeFunctionParamsAsArray(String functionName, Object[] objects) {

		LuaValue luaFunction = globals.get(functionName);

		// Check if a function with that name exists
		if (luaFunction.isfunction()) {
			LuaValue[] parameters = new LuaValue[objects.length];

			int i = 0;
			for (Object object : objects) {
				// Convert each parameter to a form that's usable by Lua
				parameters[i] = CoerceJavaToLua.coerce(object);
				i++;
			}

			// Run the function with the converted parameters
			luaFunction.invoke(parameters);
		}
	}
}
