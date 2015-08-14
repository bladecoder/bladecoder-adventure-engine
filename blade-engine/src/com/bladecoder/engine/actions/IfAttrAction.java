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
package com.bladecoder.engine.actions;

import java.util.ArrayList;
import java.util.HashMap;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class IfAttrAction implements Action {

	public static final String INFO = "Execute the actions inside the If/EndIf if the attribute has the specified value.";
	public static final Param[] PARAMS = { 
			new Param("actor", "The actor to check its attribute", Type.SCENE_ACTOR),
			new Param("attr", "The actor attribute", Type.STRING, true, "state", new String[] { "state", "visible"}),
			new Param("value", "The attribute value", Type.STRING),
			new Param("endType", "The type for the end action. All control actions must have this attr.", Type.STRING, false, "else")};

	String attr;
	String value;
	String actorId;
	String sceneId;

	@Override
	public void setParams(HashMap<String, String> params) {
		attr = params.get("attr");
		value = params.get("value");
		
		String[] a = Param.parseString2(params.get("actor"));
		if(a==null) // Called inside a scene
			return;
		
		sceneId = a[0];
		actorId = a[1];
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = (sceneId != null && !sceneId.isEmpty()) ? World.getInstance().getScene(sceneId) : World.getInstance()
				.getCurrentScene();

		if (actorId == null) {
			// if called inside a scene verb and no actor is specified, return
			EngineLogger.error("IfAttrAction::No actor especified");
			return false;
		}

		BaseActor a = s.getActor(actorId, false);

		if (a == null) { // search in inventory
			a = World.getInstance().getInventory().getItem(actorId);
		}

		if (attr.equals("state") && a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor)a;
			if (!((ia.getState() == null && value == null) || (ia.getState() != null && ia.getState().equals(value)))) {
				gotoElse((VerbRunner) cb);
			}
		} if (attr.equals("visible")) {
			boolean val = Boolean.parseBoolean(value);
			if (val != a.isVisible()) {
				gotoElse((VerbRunner) cb);
			}
		}

		return false;
	}
	
	private void gotoElse(VerbRunner v) {
		int ip = v.getIP();
		ArrayList<Action> actions = v.getActions();
		
		// TODO: Handle If to allow nested Ifs
		while (!(actions.get(ip) instanceof EndAction)
				|| !((EndAction) actions.get(ip)).getType().equals("else"))
			ip++;

		v.setIP(ip);		
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
