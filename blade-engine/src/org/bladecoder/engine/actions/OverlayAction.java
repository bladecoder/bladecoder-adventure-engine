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
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.OverlayImage;
import org.bladecoder.engine.model.World;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class OverlayAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Shows an overlay image in the scene";
	public static final Param[] PARAMS = {
		new Param("file", "Filename of the overlay image. Relative to the overlay asset directory", Type.STRING, true),
		new Param("pos", "The overlay position", Type.VECTOR2, true),
		new Param("time", "Time to show the image in screen", Type.FLOAT),
		new Param("closeOnClick", "The overlay is closed when the user clicks", Type.BOOLEAN),
		new Param("wait", "If this param is 'false' the overlay is showed and the action continues inmediatly", Type.BOOLEAN, true),
		};		
	
	private boolean wait = true;
	private boolean closeOnClick = true;
	
	private Vector2 pos;
	private float time = Float.MAX_VALUE;
	
	private String file;

	@Override
	public void run() {
		float scale = EngineAssetManager.getInstance().getScale();
		
		OverlayImage o = new OverlayImage();
		o.create(file, new Vector2(pos.x * scale, pos.y *scale), time, closeOnClick, wait?this:null);
		
		World.getInstance().getCurrentScene().setOverlay(o);
		
		if(!wait)
			onEvent();
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		
		if(params.get("time") != null) {
			time = Float.parseFloat(params.get("time"));
		}
		
		if(params.get("pos") != null) {
			pos = Param.parseVector2(params.get("pos"));
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
		
		if(params.get("closeOnClick") != null) {
			closeOnClick = Boolean.parseBoolean(params.get("closeOnClick"));
		}
		
		file = params.get("file");
	}
	
	@Override
	public void write(Json json) {		
		json.writeValue("file", file);
		json.writeValue("pos", pos);
		json.writeValue("time", time);
		json.writeValue("wait", wait);
		json.writeValue("closeOnClick", closeOnClick);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		file = json.readValue("file", String.class, jsonData);
		pos = json.readValue("pos", Vector2.class, jsonData);
		time = json.readValue("time", Float.class, jsonData);
		wait = json.readValue("wait", Boolean.class, jsonData);
		closeOnClick = json.readValue("closeOnClick", Boolean.class, jsonData);
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
