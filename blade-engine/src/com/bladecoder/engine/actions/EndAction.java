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

import java.util.List;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Marks the end of a block for a control action")
public class EndAction extends AbstractControlAction {
	
	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {
		// FIXME: This is now more generic than before, but also less optimized (we always get our "parent")
		final VerbRunner v = (VerbRunner) cb;
		final List<Action> actions = v.getActions();
		final int ip = v.getIP();

		final int parentIp = getParentControlAction(caID, actions, ip);
		final AbstractControlAction parent = (AbstractControlAction) actions.get(parentIp);

		if (parent instanceof RepeatAction) {
			v.setIP(parentIp - 1);
		} else if (parent instanceof AbstractIfAction) {
			int newIp = skipControlIdBlock(actions, parentIp); // goto Else
			newIp = skipControlIdBlock(actions, newIp); // goto EndIf

			v.setIP(newIp);
		}

		return false;
	}

	private int getParentControlAction(String caID, List<Action> actions, int ip) {
		do {
			ip--;
		} while (!(actions.get(ip) instanceof AbstractControlAction) || !((AbstractControlAction) actions.get(ip)).getControlActionID().equals(caID));
		
		return ip;
	}
}
