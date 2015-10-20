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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.bladecoder.engineeditor.utils.I18NUtils;

public class ActionList extends ModelList<Verb, Action> {
	// TODO Action cache for getting names
	
	private static final String END_ACTION = "com.bladecoder.engine.actions.EndAction";
	private static final String ACTION_NAME_VALUE_ELSE = "Else";

	// FIXME: This needs to go, just added here in the interim while we work on
	// replacing the DOM with Beans
	@SuppressWarnings("serial")
	private static final Set<String> CONTROL_ACTIONS = new HashSet<String>() {
		{
			add("Choose");
			add("IfAttr");
			add("IfProperty");
			add("IfSceneAttr");
			add("Repeat");
			add("RunOnce");
		}
	};

	@SuppressWarnings("serial")
	private static final Set<String> IF_CONTROL_ACTIONS = new HashSet<String>() {
		{
			add("IfAttr");
			add("IfProperty");
			add("IfSceneAttr");
		}
	};

	Skin skin;

	private ImageButton upBtn;
	private ImageButton downBtn;

	private ImageButton disableBtn;

	public ActionList(Skin skin) {
		super(skin, true);
		this.skin = skin;

		setCellRenderer(listCellRenderer);

		disableBtn = new ImageButton(skin);
		toolbar.addToolBarButton(disableBtn, "ic_eye", "Enable/Disable", "Enable/Disable");

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

	private void toggleEnabled() {
		//
		// Element e = list.getSelected();
		//
		// // CONTROL ACTIONS CAN'T BE DISABLED
		// if (e == null || isControlAction(e))
		// return;
		//
		// String value = e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR);
		//
		// if (value.isEmpty() || value.equals(XMLConstants.TRUE_VALUE))
		// value = XMLConstants.FALSE_VALUE;
		// else
		// value = XMLConstants.TRUE_VALUE;
		//
		// e.setAttribute(XMLConstants.ACTION_ENABLED_ATTR, value);
		// doc.setModified(e);
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Action e) {
		// return new EditActionDialog(skin, doc, parent, e);
		return null;
	}

	@Override
	protected void create() {
		// EditElementDialog dialog = getEditElementDialogInstance(null);
		// dialog.show(getStage());
		// dialog.setListener(new ChangeListener() {
		// @Override
		// public void changed(ChangeEvent event, Actor actor) {
		// int pos = list.getSelectedIndex() + 1;
		//
		// Element e2 = null;
		//
		// if (pos != 0 && pos < list.getItems().size)
		// e2 = list.getItems().get(pos);
		//
		// Element e = ((EditElementDialog) actor).getElement();
		// list.getItems().insert(pos, e);
		//
		// Node parent = e.getParentNode();
		// parent.removeChild(e);
		// parent.insertBefore(e, e2);
		//
		// list.setSelectedIndex(pos);
		//
		// if (isControlAction(e))
		// insertEndAction(e);
		//
		// list.invalidateHierarchy();
		// }
		// });
	}

	Element editedElement;

	@Override
	protected void edit() {
		//
		// Element e = list.getSelected();
		//
		// if (e == null ||
		// e.getAttribute(XMLConstants.CLASS_ATTR).equals(END_ACTION))
		// return;
		//
		// editedElement = (Element) e.cloneNode(true);
		//
		// EditElementDialog dialog = getEditElementDialogInstance(e);
		// dialog.show(getStage());
		// dialog.setListener(new ChangeListener() {
		// @Override
		// public void changed(ChangeEvent event, Actor actor) {
		// Element e = ((EditElementDialog) actor).getElement();
		// doc.setModified(e);
		//
		// if (isControlAction(editedElement)) {
		// if (!editedElement.getAttribute(XMLConstants.ACTION_NAME_ATTR)
		// .equals(e.getAttribute(XMLConstants.ACTION_NAME_ATTR))) {
		//
		// deleteControlAction(list.getSelectedIndex(), editedElement);
		//
		// if (isControlAction(e))
		// insertEndAction(e);
		// } else {
		// // insert previous caId
		// e.setAttribute(XMLConstants.CONTROL_ACTION_ID_ATTR,
		// editedElement.getAttribute(XMLConstants.CONTROL_ACTION_ID_ATTR));
		// }
		// }
		// }
		// });
	}

	private void insertEndAction(Element e) {
		// int pos = list.getItems().indexOf(e, true);
		// pos++;
		// Element e2 = null;
		//
		// if (pos != 0 && pos < list.getItems().size)
		// e2 = list.getItems().get(pos);
		//
		// final String actionName =
		// e.getAttribute(XMLConstants.ACTION_NAME_ATTR);
		// if (IF_CONTROL_ACTIONS.contains(actionName)) {
		// String id = getOrCreateControlActionId(e);
		// saveEndAction(pos, e2, ACTION_NAME_VALUE_ELSE, id);
		//
		// pos++;
		// }
		//
		// String id = getOrCreateControlActionId(e);
		// saveEndAction(pos, e2, "End" +
		// e.getAttribute(XMLConstants.ACTION_NAME_ATTR), id);
	}

	private String getOrCreateControlActionId(Element e) {
		String id = e.getAttribute(XMLConstants.CONTROL_ACTION_ID_ATTR);
		if (id.isEmpty()) {
			// FIXME: While highly, highly, highly unlikely, this might still
			// cause collisions. Replace it with a count or similar
			final String actionName = e.getAttribute(XMLConstants.ACTION_NAME_ATTR);
			id = actionName + MathUtils.random(1, Integer.MAX_VALUE);
			e.setAttribute(XMLConstants.CONTROL_ACTION_ID_ATTR, id);
		}
		return id;
	}

	private void saveEndAction(int pos, Element e2, String actionName, String id) {
		// final Element e = doc.createElement(parent, "action");
		// e.setAttribute(XMLConstants.ACTION_NAME_ATTR, actionName);
		// e.setAttribute("class", END_ACTION);
		// e.setAttribute(XMLConstants.CONTROL_ACTION_ID_ATTR, id);
		//
		// list.getItems().insert(pos, e);
		// parent.insertBefore(e, e2);
	}

	@Override
	protected void copy() {
		// Element e = list.getSelected();
		//
		// if (e.getAttribute("class").equals(END_ACTION))
		// return;
		//
		// super.copy();
	}

	@Override
	protected void paste() {
		// super.paste();
		// Element e = list.getSelected();
		//
		// if (isControlAction(e))
		// insertEndAction(e);
	}

	@Override
	protected void delete() {
		// int pos = list.getSelectedIndex();
		//
		// if (pos == -1)
		// return;
		//
		// Element e = list.getItems().get(pos);
		//
		// if (e.getAttribute("class").equals(END_ACTION))
		// return;
		//
		// super.delete();
		//
		// deleteControlAction(pos, e);
	}

	private boolean isControlAction(Element e) {
		final String actionName = e.getAttribute(XMLConstants.ACTION_NAME_ATTR);
		return CONTROL_ACTIONS.contains(actionName) || e.getAttribute(XMLConstants.CLASS_ATTR).equals(END_ACTION);
	}

	private void deleteControlAction(int pos, final Element e) {
		// final String actionName =
		// e.getAttribute(XMLConstants.ACTION_NAME_ATTR);
		// if (IF_CONTROL_ACTIONS.contains(actionName)) {
		// pos = deleteFirstActionNamed(pos, ACTION_NAME_VALUE_ELSE);
		// }
		// if (isControlAction(e)) {
		// deleteFirstActionNamed(pos, "End" + actionName);
		// }
	}

	// private int deleteFirstActionNamed(int pos, String name) {
	// while
	// (!list.getItems().get(pos).getAttribute(XMLConstants.ACTION_NAME_ATTR).equals(name))
	// pos++;
	//
	// Element e2 = list.getItems().removeIndex(pos);
	// doc.deleteElement(e2);
	// return pos;
	// }

	private void up() {
		// int pos = list.getSelectedIndex();
		//
		// if (pos == -1 || pos == 0)
		// return;
		//
		// Array<Element> items = list.getItems();
		// Element e = items.get(pos);
		// Element e2 = items.get(pos - 1);
		//
		// if (isControlAction(e) && isControlAction(e2)) {
		// return;
		// }
		//
		// Node parent = e.getParentNode();
		// parent.removeChild(e);
		// parent.insertBefore(e, e2);
		//
		// items.removeIndex(pos);
		// items.insert(pos - 1, e);
		// list.setSelectedIndex(pos - 1);
		// upBtn.setDisabled(list.getSelectedIndex() == 0);
		// downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size -
		// 1);

		// doc.setModified(e);
	}

	private void down() {
		// int pos = list.getSelectedIndex();
		// Array<Element> items = list.getItems();
		//
		// if (pos == -1 || pos == items.size - 1)
		// return;
		//
		// Element e = items.get(pos);
		// Element e2 = pos + 2 < items.size ? items.get(pos + 2) : null;
		// Element e3 = items.get(pos + 1);
		//
		// if (isControlAction(e) && isControlAction(e3)) {
		// return;
		// }
		//
		// Node parent = e.getParentNode();
		// parent.removeChild(e);
		// parent.insertBefore(e, e2);
		//
		// items.removeIndex(pos);
		// items.insert(pos + 1, e);
		// list.setSelectedIndex(pos + 1);
		// upBtn.setDisabled(list.getSelectedIndex() == 0);
		// downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size -
		// 1);
		//
		// doc.setModified(e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Action> listCellRenderer = new CellRenderer<Action>() {

		@Override
		protected String getCellTitle(Action a) {
			String id = ActionFactory.getName(a);
			
			if(id == null)
				id = a.getClass().getCanonicalName();
//			
//			
//			String id = e.getAttribute(XMLConstants.ACTION_NAME_ATTR).isEmpty() ? e.getAttribute("class")
//					: e.getAttribute(XMLConstants.ACTION_NAME_ATTR);
//
//			String actor = e.getAttribute("actor");
//			boolean animationAction = e.getAttribute(XMLConstants.ACTION_NAME_ATTR).equals("Animation");
//			boolean controlAction = isControlAction(e);
//
//			boolean enabled = e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR).isEmpty()
//					|| e.getAttribute(XMLConstants.ACTION_ENABLED_ATTR).equals(XMLConstants.TRUE_VALUE);
//
//			if (!enabled && !controlAction) {
//				if (!actor.isEmpty() && !animationAction) {
//					String[] s = Param.parseString2(actor);
//
//					if (s[0] != null)
//						id = MessageFormat.format("[GRAY]{0} {1}.{2}[]", s[0], s[1], id);
//					else
//						id = MessageFormat.format("[GRAY]{0}.{1}[]", actor, id);
//				} else if (animationAction) {
//					String a = e.getAttribute("animation");
//					String[] s = Param.parseString2(a);
//
//					if (s[0] != null)
//						id = MessageFormat.format("[GRAY]{0}.{1} {2}[]", s[0], id, s[1]);
//					else
//						id = MessageFormat.format("[GRAY]{0} {1}[]", id, a);
//				} else {
//					id = MessageFormat.format("[GRAY]{0}[]", id);
//				}
//
//			} else if (!actor.isEmpty() && !animationAction && !controlAction) {
//				String[] s = Param.parseString2(actor);
//
//				if (s[0] != null)
//					id = MessageFormat.format("[GREEN]{0}[] {1}.{2}", s[0], s[1], id);
//				else
//					id = MessageFormat.format("{0}.{1}", actor, id);
//			} else if (animationAction) {
//				String a = e.getAttribute("animation");
//				String[] s = Param.parseString2(a);
//
//				if (s[0] == null)
//					s[0] = actor;
//
//				if (s[0] != null)
//					id = MessageFormat.format("{0}.{1} [GREEN]{2}[]", s[0], id, s[1]);
//				else
//					id = MessageFormat.format("{0} [GREEN]{1}[]", id, a);
//			} else if (controlAction) {
//				if (!actor.isEmpty()) {
//					String[] s = Param.parseString2(actor);
//
//					if (s[0] != null)
//						id = MessageFormat.format("[GREEN]{0}[] [BLUE]{1}.{2}[]", s[0], s[1], id);
//					else
//						id = MessageFormat.format("[BLUE]{0}.{1}[BLUE]", actor, id);
//				} else
//					id = MessageFormat.format("[BLUE]{0}[]", id);
//			}

			return id;
		}

		@Override
		protected String getCellSubTitle(Action a) {
			StringBuilder sb = new StringBuilder();

			Param[] params = ActionUtils.getParams(a);

			for (Param p : params) {
				String name = p.name;

				if (name.equals("actor") || name.equals(XMLConstants.CLASS_ATTR)
						|| name.equals(XMLConstants.ACTION_NAME_ATTR) || name.equals(XMLConstants.ACTION_ENABLED_ATTR)
						|| name.equals(XMLConstants.CONTROL_ACTION_ID_ATTR)
						|| (ActionFactory.getName(a).equals("Animation") && name.equals("animation")))
					continue;

				Field f = ActionUtils.getField(a.getClass(), p.name);

				try {
					final boolean accessible = f.isAccessible();
					f.setAccessible(true);
					Object o = f.get(a);
					if (o == null) continue;
					String v = o.toString();
					
					sb.append(name).append(": ").append(I18NUtils.translate(v)).append(' ');
					f.setAccessible(accessible);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					EditorLogger.error(e.getMessage());
				}
			}

			return sb.toString();
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}
