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
package org.bladecoder.engineeditor.ui;

import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class SoundList extends ElementList {	
	
	public SoundList(Skin skin) {
		super(skin, true);
		
		setCellRenderer(listCellRenderer);
	}	

	@Override
	protected EditSoundDialog getEditElementDialogInstance(Element e) {
		return new EditSoundDialog(skin, doc, parent, e);
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private static final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			String id  = e.getAttribute("id");

			return id;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			String filename = e.getAttribute("filename");
			String loop = e.getAttribute("loop");
			String volume = e.getAttribute("volume");

			StringBuilder sb = new StringBuilder();

			if (!filename.isEmpty())
				sb.append("filename: ").append(filename);
			if (!loop.isEmpty())
				sb.append(" loop: ").append(loop);
			if (!volume.isEmpty())
				sb.append(" volume: ").append(volume);
			
			return sb.toString();
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};
}
