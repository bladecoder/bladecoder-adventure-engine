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

import java.text.MessageFormat;

import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ElementList;


public class VerbList extends ElementList {
	
	public static final String VERBS[] = { "lookat", "pickup", "talkto", "use", "leave", "enter", "exit", "init",
		"test", "custom" };

	private ActionList actionList;

	public VerbList(Skin skin) {
		super(skin, true);
		actionList = new ActionList(skin);
		
//		addActor(actionList);
		row();
		add(actionList).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addActions();
			}
		});

		list.setCellRenderer(listCellRenderer);
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditVerbDialog(skin, doc, parent, e);
	}

	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		super.addElements(doc, parent, tag);
		addActions();
	}
	
	@Override
	protected void delete() {
		super.delete();
		
		// Clear actions here because change event doesn't call when deleting the last element
		if(list.getSelectedIndex() == -1)
			addActions();
	}

	private void addActions() {
		int pos = list.getSelectedIndex();

		Element v = null;

		if (pos != -1) {
			v = list.getItems().get(pos);
		}

		actionList.addElements(doc, v, "action");
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			return e.getAttribute("id");
		}

		@Override
		protected String getCellSubTitle(Element e) {
			String state = e.getAttribute("state");
			String target = e.getAttribute("target");

			StringBuilder sb = new StringBuilder();

			if (!state.isEmpty())
				sb.append("when ").append(state);
			if (!target.isEmpty())
				sb.append(" with target '").append(target).append("'");

			return sb.toString();
		}

		@Override
		public TextureRegion getCellImage(Element e) {
			boolean custom = true;
			
			String verbName = e.getAttribute("id");
			for(String v:VERBS) {
				if(v.equals(verbName)) {
					custom = false;
					break;
				}
			}
			
			String iconName = MessageFormat.format("ic_{0}", e.getAttribute("id"));
			TextureRegion image = null;
			
			if(!custom)
				image = Ctx.assetManager.getIcon(iconName);
			else
				image = Ctx.assetManager.getIcon("ic_custom");

			return image;
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
		
		@Override
		protected boolean hasImage() {
			return true;
		}
	};
}
