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
package com.bladecoder.engineeditor.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engineeditor.model.BaseDocument;
import org.w3c.dom.Element;

public class EditSoundDialog extends EditAnnotatedDialog<SoundFX> {
	public EditSoundDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin, SoundFX.class, doc, parent, XMLConstants.SOUND_TAG, e);
	}
}
