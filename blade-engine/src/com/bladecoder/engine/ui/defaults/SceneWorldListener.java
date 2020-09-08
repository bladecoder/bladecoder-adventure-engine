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
package com.bladecoder.engine.ui.defaults;

import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.model.WorldListener;
import com.bladecoder.engine.ui.ITextManagerUI;
import com.bladecoder.engine.util.UIUtils;

public class SceneWorldListener implements WorldListener {

	private final DefaultSceneScreen dsc;

	public SceneWorldListener(DefaultSceneScreen dsc) {
		this.dsc = dsc;
	}

	@Override
	public void text(Text t) {
		if (t != null && t.type == Text.Type.UI) {
			UIUtils.showUIText(dsc.getStage(), dsc.getUI().getSkin(), dsc.getWorld(), t);
		} else {
			((ITextManagerUI) dsc.getTextManagerUI()).setText(t);
		}
	}

	@Override
	public void dialogOptions() {
		dsc.updateUI();
	}

	@Override
	public void cutMode(boolean value) {
		dsc.updateUI();
	}

	@Override
	public void inventoryEnabled(boolean value) {
		dsc.getInventoryUI().hide();
		dsc.getInventoryButton().setVisible(value);
	}

	@Override
	public void pause(boolean value) {
		if (dsc.getUI().getWorld().getAssetState() == AssetState.LOADED)
			dsc.updateUI();
	}
}
