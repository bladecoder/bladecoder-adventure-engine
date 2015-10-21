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
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.ModelList;

public class SoundList extends ModelList<InteractiveActor, SoundFX> {	
	
	public SoundList(Skin skin) {
		super(skin, true);
		
		setCellRenderer(listCellRenderer);
	}	

	@Override
	protected EditSoundDialog getEditElementDialogInstance(SoundFX s) {
		return new EditSoundDialog(skin, parent, s);
	}

	@Override
	protected void delete() {
		
		SoundFX s = removeSelected();
		
		parent.getSounds().remove(s.getFilename());
		
// TODO UNDO
//		UndoOp undoOp = new UndoDeleteElement(doc, e);
//		Ctx.project.getUndoStack().add(undoOp);
//		doc.deleteElement(e);

// TODO TRANSLATIONS
//		I18NUtils.putTranslationsInElement(doc, clipboard);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private static final CellRenderer<SoundFX> listCellRenderer = new CellRenderer<SoundFX>() {

		@Override
		protected String getCellTitle(SoundFX e) {
			return e.getFilename();
		}

		@Override
		protected String getCellSubTitle(SoundFX e) {
			String filename = e.getFilename();

			StringBuilder sb = new StringBuilder();

			if (filename != null && !filename.isEmpty())
				sb.append("filename: ").append(filename);

			sb.append(" loop: ").append(e.getLoop());
			sb.append(" volume: ").append(e.getVolume());
			
			return sb.toString();
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}
