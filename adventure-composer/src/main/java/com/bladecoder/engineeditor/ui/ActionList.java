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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ElementList;

public class ActionList extends ElementList {
	private static final String END_ACTION = "com.bladecoder.engine.actions.EndAction";
	private static final String ENDTYPE_ATTR = "endType";

	Skin skin;

	private ImageButton upBtn;
	private ImageButton downBtn;
	
	private ImageButton disableBtn;

	public ActionList(Skin skin) {
		super(skin, true);
		this.skin = skin;

		setCellRenderer(listCellRenderer);
		
		disableBtn = new ImageButton(skin);
		toolbar.addToolBarButton(disableBtn, "ic_eye", "Enable/Disable",
				"Enable/Disable");

		disableBtn.setDisabled(false);

		disableBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				toggleEnabled();
			}
		});

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
				disableBtn.setDisabled(pos == -1);
				upBtn.setDisabled(pos == -1 || pos == 0);
				downBtn.setDisabled(pos == -1
						|| pos == list.getItems().size - 1);
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
	
	private void toggleEnabled() {

		Element e = list.getSelected();

		// CONTROL ACTIONS CAN'T BE DISABLED
		if (e == null || isControlAction(e))
			return;

		String value = e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR);
		
		if(value.isEmpty() || value.equals(XMLConstants.TRUE_VALUE))
			value = XMLConstants.FALSE_VALUE;
		else
			value = XMLConstants.TRUE_VALUE;
		
		e.setAttribute(XMLConstants.ACTION_ENABLED_ATTR, value);
		doc.setModified(e);
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditActionDialog(skin, doc, parent, e);
	}

	@Override
	protected void create() {
		EditElementDialog dialog = getEditElementDialogInstance(null);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex() + 1;

				Element e2 = null;

				if (pos != 0 && pos < list.getItems().size)
					e2 = list.getItems().get(pos);

				Element e = ((EditElementDialog) actor).getElement();
				list.getItems().insert(pos, e);

				Node parent = e.getParentNode();
				parent.removeChild(e);
				parent.insertBefore(e, e2);

				list.setSelectedIndex(pos);

				if (isControlAction(e))
					insertEndAction(e);

				list.invalidateHierarchy();
			}
		});
	}

	Element editedElement;

	@Override
	protected void edit() {

		Element e = list.getSelected();

		if (e == null || e.getAttribute("class").equals(END_ACTION))
			return;

		editedElement = (Element) e.cloneNode(true);

		EditElementDialog dialog = getEditElementDialogInstance(e);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Element e = ((EditElementDialog) actor).getElement();
				doc.setModified(e);

				if (isControlAction(editedElement)
						&& !editedElement.getAttribute(XMLConstants.ACTION_NAME_ATTR).equals(
								e.getAttribute(XMLConstants.ACTION_NAME_ATTR))) {

					deleteControlAction(list.getSelectedIndex(), editedElement);

					if (isControlAction(e))
						insertEndAction(e);
				}
			}
		});
	}

	private void insertEndAction(Element e) {
		int pos = list.getItems().indexOf(e, true);
		pos++;
		Element e2 = null;

		if (pos != 0 && pos < list.getItems().size)
			e2 = list.getItems().get(pos);

		if (e.getAttribute(ENDTYPE_ATTR).equals("else")) {
			Element elseEl = doc.createElement((Element) parent, "action");
			elseEl.setAttribute(XMLConstants.ACTION_NAME_ATTR, "Else");
			elseEl.setAttribute("class", END_ACTION);
			elseEl.setAttribute(ENDTYPE_ATTR, "else");

			Element endEl = doc.createElement((Element) parent, "action");
			endEl.setAttribute(XMLConstants.ACTION_NAME_ATTR,
					"End" + e.getAttribute(XMLConstants.ACTION_NAME_ATTR));
			endEl.setAttribute("class", END_ACTION);
			endEl.setAttribute(ENDTYPE_ATTR, "if");

			list.getItems().insert(pos, elseEl);
			list.getItems().insert(pos + 1, endEl);

			parent.insertBefore(elseEl, e2);
			parent.insertBefore(endEl, e2);
		} else {
			Element endEl = doc.createElement((Element) parent, "action");
			endEl.setAttribute(XMLConstants.ACTION_NAME_ATTR,
					"End" + e.getAttribute(XMLConstants.ACTION_NAME_ATTR));
			endEl.setAttribute("class", END_ACTION);
			endEl.setAttribute(ENDTYPE_ATTR, e.getAttribute(ENDTYPE_ATTR));

			list.getItems().insert(pos, endEl);

			parent.insertBefore(endEl, e2);
		}
	}

	@Override
	protected void copy() {
		Element e = list.getSelected();

		if (e.getAttribute("class").equals(END_ACTION))
			return;

		super.copy();
	}

	@Override
	protected void paste() {
		super.paste();
		Element e = list.getSelected();

		if (isControlAction(e))
			insertEndAction(e);
	}

	@Override
	protected void delete() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		Element e = list.getItems().get(pos);

		if (e.getAttribute("class").equals(END_ACTION))
			return;

		super.delete();

		deleteControlAction(pos, e);
	}

	private boolean isControlAction(Element e) {
		return !e.getAttribute(ENDTYPE_ATTR).isEmpty();
	}

	private void deleteControlAction(int pos, final Element e) {
		if (e.getAttribute(ENDTYPE_ATTR).equals("else")) {
			pos = deleteFirstActionNamed(pos, "Else");
		}
		if (!e.getAttribute(ENDTYPE_ATTR).isEmpty()) {
			deleteFirstActionNamed(pos, "End" + e.getAttribute(XMLConstants.ACTION_NAME_ATTR));
		}
	}

	private int deleteFirstActionNamed(int pos, String name) {
		while (!list.getItems().get(pos).getAttribute(XMLConstants.ACTION_NAME_ATTR).equals(name))
			pos++;

		Element e2 = list.getItems().removeIndex(pos);
		doc.deleteElement(e2);
		return pos;
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		Array<Element> items = list.getItems();
		Element e = items.get(pos);
		Element e2 = items.get(pos - 1);

		if (isControlAction(e)
				&& isControlAction(e2)) {
			return;
		}

		Node parent = e.getParentNode();
		parent.removeChild(e);
		parent.insertBefore(e, e2);

		items.removeIndex(pos);
		items.insert(pos - 1, e);
		list.setSelectedIndex(pos - 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		doc.setModified(e);
	}

	private void down() {
		int pos = list.getSelectedIndex();
		Array<Element> items = list.getItems();

		if (pos == -1 || pos == items.size - 1)
			return;

		Element e = items.get(pos);
		Element e2 = pos + 2 < items.size ? items.get(pos + 2) : null;
		Element e3 = items.get(pos + 1);

		if (isControlAction(e)
				&& isControlAction(e3)) {
			return;
		}

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
			String id = e.getAttribute(XMLConstants.ACTION_NAME_ATTR).isEmpty() ? e
					.getAttribute("class") : e.getAttribute(XMLConstants.ACTION_NAME_ATTR);

			String actor = e.getAttribute("actor");
			boolean animationAction = e.getAttribute(XMLConstants.ACTION_NAME_ATTR).equals(
					"Animation");
			boolean controlAction = isControlAction(e);
			
			boolean enabled = e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR).isEmpty() || e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR).equals(XMLConstants.TRUE_VALUE);
			
			if(!enabled && !controlAction) {
				if (!actor.isEmpty() && !animationAction) {
					String[] s = Param.parseString2(actor);

					if (s[0] != null)
						id = MessageFormat.format("[GRAY]{0} {1}.{2}[]", s[0],
								s[1], id);
					else
						id = MessageFormat.format("[GRAY]{0}.{1}[]", actor, id);
				} else if (animationAction) {
					String a = e.getAttribute("animation");
					String[] s = Param.parseString2(a);

					if (s[0] != null)
						id = MessageFormat.format("[GRAY]{0}.{1} {2}[]", s[0], id,
								s[1]);
					else
						id = MessageFormat.format("[GRAY]{0} {1}[]", id, a);
				} else {
					id = MessageFormat.format("[GRAY]{0}[]", id);
				}

			} else if (!actor.isEmpty() && !animationAction && !controlAction) {
				String[] s = Param.parseString2(actor);

				if (s[0] != null)
					id = MessageFormat.format("[GREEN]{0}[] {1}.{2}", s[0],
							s[1], id);
				else
					id = MessageFormat.format("{0}.{1}", actor, id);
			} else if (animationAction) {
				String a = e.getAttribute("animation");
				String[] s = Param.parseString2(a);
				
				if(s[0] == null)
					s[0] = actor;

				if (s[0] != null)
					id = MessageFormat.format("{0}.{1} [GREEN]{2}[]", s[0], id,
							s[1]);
				else
					id = MessageFormat.format("{0} [GREEN]{1}[]", id, a);
			} else if (controlAction) {
				if (!actor.isEmpty()) {
					String[] s = Param.parseString2(actor);

					if (s[0] != null)
						id = MessageFormat.format("[GREEN]{0}[] [BLUE]{1}.{2}[]", s[0],
								s[1], id);
					else
						id = MessageFormat.format("[BLUE]{0}.{1}[BLUE]", actor, id);
				} else
					id = MessageFormat.format("[BLUE]{0}[]", id);
			}

			return id;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			StringBuilder sb = new StringBuilder();

			NamedNodeMap attr = e.getAttributes();

			for (int i = 0; i < attr.getLength(); i++) {
				Node n = attr.item(i);
				String name = n.getNodeName();

				if (name.equals(ENDTYPE_ATTR)
						|| name.equals("actor")
						|| name.equals(XMLConstants.CLASS_ATTR)
						|| name.equals(XMLConstants.ACTION_NAME_ATTR)
						|| name.equals(XMLConstants.ACTION_ENABLED_ATTR)
						|| (e.getAttribute(XMLConstants.ACTION_NAME_ATTR).equals("Animation") && name
								.equals("animation")))
					continue;

				String v = n.getNodeValue();

				sb.append(name).append(": ").append(doc.getTranslation(v))
						.append(' ');
			}

			return sb.toString();
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}
