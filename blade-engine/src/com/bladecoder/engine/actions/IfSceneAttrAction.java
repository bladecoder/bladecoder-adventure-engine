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

import java.util.HashMap;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("IfSceneAttr")
@ModelDescription("Execute the actions inside the If/EndIf if the attribute has the specified value.")
public class IfSceneAttrAction extends AbstractIfAction {
	private String caID;

	public enum SceneAttr {
		STATE
	}

	@JsonProperty("scene")
	@JsonPropertyDescription("The scene to check its attribute")
	@ModelPropertyType(Type.SCENE)
	private String sceneId;

	@JsonProperty(required = true, defaultValue = "state")
	@JsonPropertyDescription("The scene attribute")
	@ModelPropertyType(Type.STRING)
	private SceneAttr attr;

	@JsonProperty
	@JsonPropertyDescription("The attribute value")
	@ModelPropertyType(Type.STRING)
	private String value;

	@Override
	public void setParams(HashMap<String, String> params) {
		attr = SceneAttr.valueOf(params.get("attr").trim().toUpperCase());
		value = params.get("value");
		sceneId = params.get("scene");

		caID = params.get(XMLConstants.CONTROL_ACTION_ID_ATTR);
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

	@Override
	public String getControlActionID() {
		return caID;
	}
}
