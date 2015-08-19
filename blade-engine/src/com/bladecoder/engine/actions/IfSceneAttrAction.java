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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Execute the actions inside the If/EndIf if the attribute has the specified value.")
public class IfSceneAttrAction implements ControlAction {
	public static final String ENDTYPE_VALUE = "else";

	public enum SceneAttr {
		STATE
	}

	public static final Param[] PARAMS = {
			new Param("scene", "The scene to check its attribute", Type.SCENE),
			new Param("attr", "The scene attribute", Type.STRING, true, "state", new String[] { "state" }),
			new Param("value", "The attribute value", Type.STRING),
			new Param("endType", "The type for the end action. All control actions must have this attr.", Type.STRING, false, ENDTYPE_VALUE)};

	@JsonProperty
	@JsonPropertyDescription("The scene to check its attribute")
	@ActionPropertyType(Type.SCENE)
	private String sceneId;

	@JsonProperty(required = true, defaultValue = "state")
	@JsonPropertyDescription("The scene attribute")
	@ActionPropertyType(Type.STRING)
	private SceneAttr attr;

	@JsonProperty
	@JsonPropertyDescription("The attribute value")
	@ActionPropertyType(Type.STRING)
	private String value;

	@Override
	public void setParams(HashMap<String, String> params) {
		attr = SceneAttr.valueOf(params.get("attr").trim().toUpperCase());
		value = params.get("value");
		sceneId = params.get("scene");
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s = (sceneId != null && !sceneId.isEmpty()) ? World.getInstance().getScene(sceneId) : World.getInstance()
				.getCurrentScene();

		if (attr == SceneAttr.STATE) {
			if (!((s.getState() == null && value == null) || (s.getState() != null && s.getState().equals(value)))) {
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
				|| !((EndAction) actions.get(ip)).getEndType().equals(ENDTYPE_VALUE))
			ip++;

		v.setIP(ip);		
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}

	@Override
	public String getEndType() {
		return ENDTYPE_VALUE;
	}
}
