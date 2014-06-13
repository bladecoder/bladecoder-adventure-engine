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

import java.text.MessageFormat;

import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;

public class ActionList extends ElementList {
	Skin skin;
	
	private ImageButton upBtn;
	private ImageButton downBtn;

	public ActionList(Skin skin) {
		super(skin, true);
		this.skin = skin;
		
		setCellRenderer(listCellRenderer);

		upBtn = new ImageButton(skin);
		downBtn = new ImageButton(skin);

		toolbar.addToolBarButton(upBtn, "ic_up", "Move up", "Move up");
		toolbar.addToolBarButton(downBtn, "ic_down", "Move down", "Move down");
		toolbar.pack();

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
				upBtn.setDisabled(pos == -1 || pos == 0);
				downBtn.setDisabled(pos == -1 || pos == list.getItems().size - 1);
			}
		});

		upBtn.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				up();
			}
		});
		
		
		downBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				down();
			}
		});
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditActionDialog(skin, doc, parent, e);
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		Array<Element> items =  list.getItems();
		Element e = items.get(pos);
		Element e2 = items.get(pos - 1);

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);

		items.removeIndex(pos);
		items.insert( pos - 1, e);
		list.setSelectedIndex(pos - 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		doc.setModified(e);
	}

	private void down() {
		int pos = list.getSelectedIndex();
		Array<Element> items =  list.getItems();

		if (pos == -1 || pos == items.size - 1)
			return;

		Element e = items.get(pos);
		Element e2 = pos + 2 < items.size ? items.get(pos + 2) : null;

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);

		
		items.removeIndex(pos);
		items.insert(pos + 1, e);
		list.setSelectedIndex(pos + 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		doc.setModified(e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			String id = e.getAttribute("class").isEmpty()?e.getAttribute("action_name"): e.getAttribute("class");

			String actor = e.getAttribute("actor");

			if (!actor.isEmpty())
				id = MessageFormat.format("{1}.{0}", id, actor);
			
			return id;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			StringBuilder sb = new StringBuilder();

			NamedNodeMap attr = e.getAttributes();

			for (int i = 0; i < attr.getLength(); i++) {
				Node n = attr.item(i);
				String name = n.getNodeName();

				if (name.equals("actor") || name.equals("class") || name.equals("action_name"))
					continue;

				String v = n.getNodeValue();

				sb.append(name).append(": ").append(doc.getTranslation(v)).append(' ');
			}
			
			return sb.toString();
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}
