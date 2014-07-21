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
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class WaitAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Pause the action";
	public static final Param[] PARAMS = {
		new Param("time", "The time for the pause in seconds", Type.FLOAT, true, "1.0")
		};		
	
	private float time;
	

	@Override
	public void run() {
		World.getInstance().addTimer(time, this);
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		time = Float.parseFloat(params.get("time"));
	}
	
	@Override
	public void write(Json json) {		
		json.writeValue("time", time);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		time = json.readValue("time", Float.class, jsonData);
		super.read(json, jsonData);
	}	
	

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}
