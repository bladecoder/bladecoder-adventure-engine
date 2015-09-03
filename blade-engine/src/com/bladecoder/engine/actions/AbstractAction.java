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

import com.bladecoder.engine.model.TrackPropertyChanges;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
public abstract class AbstractAction implements Cloneable {
	@JsonProperty
	private Boolean enabled;

	public boolean getEnabled() {
		return enabled != null && enabled;
	}

	@TrackPropertyChanges
	public void setEnabled(boolean enabled) {
		this.enabled = !enabled ? null : true;
	}

	/**
	 * Execute the action
	 * 
	 * @param cb
	 * @return If 'true', the verb must stops the execution and wait
	 * for the action to call the cb.resume()
	 */
	public abstract boolean run(ActionCallback cb);

	@Deprecated
	public abstract void setParams(HashMap<String, String> params);
}
