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

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Jump an Ink knot or stich.")
public class InkRunAction implements Action {
	@ActionPropertyDescription("The knot/stich/function path to jump. Ej: 'myKnotName' or 'myKnotName.theStitchWithin'")
	@ActionProperty(required = true)
	private String path;

	@ActionPropertyDescription("List of params (comma separated) for the path. Use the % prefix for numbers.")
	@ActionProperty(required = false)
	private String params;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Waits for the action to finish.")
	private boolean wait = true;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		try {
			Object[] p = null;

			if (params != null && !params.trim().isEmpty()) {
				String[] split = params.split(",");

				p = new Object[split.length];

				for (int i = 0; i < split.length; i++) {
					String v = split[i].trim();
					Object val = v;

					if (v.charAt(0) == '%') {
						try {
							val = Float.parseFloat(v.substring(1));
						} catch (NumberFormatException e) {
							// do nothing
						}
					}

					p[i] = val;
				}
			}

			w.getInkManager().runPath(path, p, wait ? cb : null);
		} catch (Exception e) {
			EngineLogger.error("Cannot jump to: " + path + " " + e.getMessage());
		}

		return wait;
	}
}
