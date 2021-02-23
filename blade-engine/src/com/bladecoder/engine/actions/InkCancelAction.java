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

import com.bladecoder.engine.ink.InkVerbRunner;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Stops the selected Ink flow.")
public class InkCancelAction implements Action {
	@ActionPropertyDescription("The conversation flow. Empty for the default flow.")
	@ActionProperty(required = false)
	private String flow;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		InkVerbRunner inkVerbRunner;

		try {
			if (flow == null) {
				inkVerbRunner = w.getInkManager().getDefaultVerbRunner();
				w.getInkManager().getStory().switchToDefaultFlow();
			} else {
				inkVerbRunner = w.getInkManager().getVerbRunners().get(flow);
				w.getInkManager().getStory().switchFlow(flow);
			}

			inkVerbRunner.cancel();
			w.getInkManager().getStory().resetCallstack();

		} catch (Exception e) {
			EngineLogger.error("Error cancelling flow: " + flow == null ? "DEFAULT" : flow);
		}

		return false;
	}
}
