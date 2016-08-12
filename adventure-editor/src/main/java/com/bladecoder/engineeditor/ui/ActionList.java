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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.actions.AbstractControlAction;
import com.bladecoder.engine.actions.AbstractIfAction;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.actions.DisableActionAction;
import com.bladecoder.engine.actions.EndAction;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;
import com.bladecoder.engineeditor.ui.components.ScopePanel;
import com.bladecoder.engineeditor.undo.UndoDeleteAction;

public class ActionList extends ModelList<Verb, Action> {
	private static final String CONTROL_ACTION_ID_ATTR = "caID";

	Skin skin;

	private ImageButton upBtn;
	private ImageButton downBtn;

	private ImageButton disableBtn;

	private String scope;

	public ActionList(Skin skin) {
		super(skin, false);
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
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ELEMENT_CREATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof Action && !(evt.getSource() instanceof EditActionDialog)) {
					addElements(parent, parent.getActions());
				}
			}
		});
	}

	private void toggleEnabled() {

		Action a = list.getSelected();

		// CONTROL ACTIONS CAN'T BE DISABLED
		if (a == null || isControlAction(a))
			return;

		int pos = list.getSelectedIndex();
		Array<Action> items = list.getItems();

		if (a instanceof DisableActionAction) {
			Action a2 = ((DisableActionAction)a).getAction();
			parent.getActions().set(pos, a2);
			items.set(pos, a2);
		} else {
			DisableActionAction a2 = new DisableActionAction();			
			a2.setAction(a);			
			parent.getActions().set(pos, a2);
			items.set(pos, a2);
		}
		
		list.setSelectedIndex(pos);

		Ctx.project.setModified();
	}

	@Override
	protected EditModelDialog<Verb, Action> getEditElementDialogInstance(Action e) {
		EditActionDialog editActionDialog = new EditActionDialog(skin, parent, e, scope,  list.getSelectedIndex());

		return editActionDialog;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	protected void create() {
		EditModelDialog<Verb, Action> dialog = getEditElementDialogInstance(null);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex() + 1;

				Action e = ((EditModelDialog<Verb, Action>) actor).getElement();
				list.getItems().insert(pos, e);
				parent.getActions().add(pos, e);

				list.setSelectedIndex(pos);

				if (isControlAction(e)) {
					insertEndAction(pos + 1, getOrCreateControlActionId((AbstractControlAction) e));
					
					if(e instanceof AbstractIfAction)
						insertEndAction(pos + 2, getOrCreateControlActionId((AbstractControlAction) e));
				}

				list.invalidateHierarchy();
			}
		});
	}

	private Action editedElement;

	@Override
	protected void edit() {

		Action e = list.getSelected();

		if (e == null || e instanceof EndAction || e instanceof DisableActionAction)
			return;

		editedElement = (Action) ElementUtils.cloneElement(e);

		EditModelDialog<Verb, Action> dialog = getEditElementDialogInstance(e);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Action e = ((EditModelDialog<Verb, Action>) actor).getElement();
				int pos = list.getSelectedIndex();
				list.getItems().set(pos, e);
				parent.getActions().set(pos, e);

				Ctx.project.setModified();

				if (isControlAction(editedElement)) {
					if (!editedElement.getClass().getName().equals(e.getClass().getName())) {

						deleteControlAction(list.getSelectedIndex(), (AbstractControlAction) editedElement);

						if (isControlAction(e)) {
							insertEndAction(list.getSelectedIndex() + 1,
									getOrCreateControlActionId((AbstractControlAction) e));
							
							if(e instanceof AbstractIfAction)
								insertEndAction(list.getSelectedIndex() + 2, getOrCreateControlActionId((AbstractControlAction) e));
						}
					} else {
						// insert previous caId
						try {
							ActionUtils.setParam(e, CONTROL_ACTION_ID_ATTR,
									getOrCreateControlActionId((AbstractControlAction) editedElement));
						} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
							EditorLogger.error(e1.getMessage());
						}
					}
				}
			}
		});
	}

	private String getOrCreateControlActionId(AbstractControlAction a) {
		String id = a.getControlActionID();

		if (id == null || id.isEmpty()) {
			id = Integer.toString(MathUtils.random(1, Integer.MAX_VALUE));
			try {
				ActionUtils.setParam(a, CONTROL_ACTION_ID_ATTR, id);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
		}

		return id;
	}

	private void insertEndAction(int pos, String id) {
		final Action e = new EndAction();

		try {
			ActionUtils.setParam(e, CONTROL_ACTION_ID_ATTR, id);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
			EditorLogger.error(e1.getMessage());
		}

		list.getItems().insert(pos, e);
		parent.getActions().add(pos, e);
	}

	@Override
	protected void copy() {
		Action e = list.getSelected();

		if (e == null || e instanceof EndAction)
			return;

		clipboard = (Action) ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		if (scope.equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().putTranslationsInElement(clipboard, true);
		else
			Ctx.project.getI18N().putTranslationsInElement(clipboard, false);
	}

	@Override
	protected void paste() {
		
		if(parent == null || clipboard == null)
			return;
		
		Action newElement = (Action) ElementUtils.cloneElement(clipboard);

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);
		parent.getActions().add(pos, newElement);

		if (scope.equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().extractStrings(null, null, parent.getHashKey(), pos, newElement);
		else if (scope.equals(ScopePanel.SCENE_SCOPE))
			Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(), null, parent.getHashKey(), pos,  newElement);
		else
			Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(), Ctx.project.getSelectedActor().getId(), parent.getHashKey(), pos, newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();

		Ctx.project.setModified();

		if (isControlAction(newElement)) {
			try {
				ActionUtils.setParam(newElement, CONTROL_ACTION_ID_ATTR, null);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				EditorLogger.error(e.getMessage());
			}
			
			insertEndAction(pos + 1, getOrCreateControlActionId((AbstractControlAction) newElement));
			
			if(newElement instanceof AbstractIfAction)
				insertEndAction(pos + 2, getOrCreateControlActionId((AbstractControlAction) newElement));
		}
	}

	@Override
	protected void delete() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		Action e = list.getItems().get(pos);

		if (e instanceof EndAction)
			return;

		Action action = removeSelected();

		int idx = parent.getActions().indexOf(e);

		parent.getActions().remove(action);

		// TRANSLATIONS
		if (scope.equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().putTranslationsInElement(e, true);
		else
			Ctx.project.getI18N().putTranslationsInElement(e, false);

		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteAction(parent, e, idx));

		if (isControlAction(e))
			deleteControlAction(pos, (AbstractControlAction) e);

		Ctx.project.setModified();
	}

	private boolean isControlAction(Action e) {
		return e instanceof AbstractControlAction;
	}

	private void deleteControlAction(int pos, final AbstractControlAction e) {
		final String id = getOrCreateControlActionId(e);

		if (e instanceof AbstractIfAction) {
			pos = deleteFirstActionNamed(pos, id);
		}

		deleteFirstActionNamed(pos, id);
	}

	private int deleteFirstActionNamed(int pos, String actionId) {
		while (!(list.getItems().get(pos) instanceof AbstractControlAction
				&& getOrCreateControlActionId((AbstractControlAction) list.getItems().get(pos)).equals(actionId)))
			pos++;

		Action e2 = list.getItems().removeIndex(pos);
		parent.getActions().remove(e2);

		return pos;
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		Array<Action> items = list.getItems();
		Action e = items.get(pos);
		Action e2 = items.get(pos - 1);

		if (isControlAction(e) && isControlAction(e2)) {
			return;
		}

		parent.getActions().set(pos - 1, e);
		parent.getActions().set(pos, e2);

		items.set(pos - 1, e);
		items.set(pos, e2);

		list.setSelectedIndex(pos - 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}

	private void down() {
		int pos = list.getSelectedIndex();
		Array<Action> items = list.getItems();

		if (pos == -1 || pos == items.size - 1)
			return;

		Action e = items.get(pos);
		Action e2 = items.get(pos + 1);

		if (isControlAction(e) && isControlAction(e2)) {
			return;
		}

		parent.getActions().set(pos + 1, e);
		parent.getActions().set(pos, e2);

		items.set(pos + 1, e);
		items.set(pos, e2);
		list.setSelectedIndex(pos + 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Action> listCellRenderer = new CellRenderer<Action>() {

		@Override
		protected String getCellTitle(Action a) {
			boolean enabled = true;
			
			if(a instanceof DisableActionAction) {
				a = ((DisableActionAction) a).getAction();
				enabled = false;
			}
			
			String id = ActionUtils.getName(a.getClass());

			if (id == null)
				id = a.getClass().getCanonicalName();

			Field field = ActionUtils.getField(a.getClass(), "actor");
			String actor = null;

			if (field != null) {
				try {
					field.setAccessible(true);
					Object v = field.get(a);
					if (v != null)
						actor = v.toString();
				} catch (IllegalArgumentException | IllegalAccessException e) {
					EditorLogger.error(e.getMessage());
				}
			}

			boolean animationAction = id.equals("Animation");
			boolean controlAction = isControlAction(a);

			if (!enabled && !controlAction) {
				if (actor != null && !animationAction) {
					SceneActorRef sa = new SceneActorRef(actor);

					if (sa.getSceneId() != null)
						id = MessageFormat.format("[GRAY]{0} {1}.{2}[]", sa.getSceneId(), sa.getActorId(), id);
					else
						id = MessageFormat.format("[GRAY]{0}.{1}[]", sa.getActorId(), id);
				} else if (animationAction) {
					field = ActionUtils.getField(a.getClass(), "animation");
					String animation = null;

					if (field != null) {
						try {
							field.setAccessible(true);
							animation = field.get(a).toString();
						} catch (IllegalArgumentException | IllegalAccessException e) {
							EditorLogger.error(e.getMessage());
						}
					}

					ActorAnimationRef aa = new ActorAnimationRef(animation);

					if (aa.getActorId() != null)
						id = MessageFormat.format("[GRAY]{0}.{1} {2}[]", aa.getActorId(), id, aa.getAnimationId());
					else
						id = MessageFormat.format("[GRAY]{0} {1}[]", id, aa.getAnimationId());
				} else {
					id = MessageFormat.format("[GRAY]{0}[]", id);
				}

			} else if (actor != null && !animationAction && !controlAction) {
				SceneActorRef sa = new SceneActorRef(actor);

				if (sa.getSceneId() != null)
					id = MessageFormat.format("[GREEN]{0}[] {1}.{2}", sa.getSceneId(), sa.getActorId(), id);
				else
					id = MessageFormat.format("{0}.{1}", sa.getActorId(), id);
			} else if (animationAction) {
				field = ActionUtils.getField(a.getClass(), "animation");
				String animation = null;

				if (field != null) {
					try {
						field.setAccessible(true);
						animation = field.get(a).toString();
					} catch (IllegalArgumentException | IllegalAccessException e) {
						EditorLogger.error(e.getMessage());
					}
				}

				ActorAnimationRef aa = new ActorAnimationRef(animation);

				if (aa.getActorId() != null)
					id = MessageFormat.format("[GREEN]{0}.{1} {2}[]", aa.getActorId(), id, aa.getAnimationId());
				else
					id = MessageFormat.format("[GREEN]{0} {1}[]", id, aa.getAnimationId());
			} else if (controlAction) {
				if (a instanceof EndAction) {
					Action parentAction = findParentAction((EndAction) a);

					if (parentAction instanceof AbstractIfAction
							&& isElse((AbstractIfAction) parentAction, (EndAction) a)) {
						id = "Else";
					} else {
						id = "End" + ActionUtils.getName(parentAction.getClass());
					}
				}

				if (actor != null) {
					SceneActorRef sa = new SceneActorRef(actor);

					if (sa.getSceneId() != null)
						id = MessageFormat.format("[GREEN]{0}[] [BLUE]{1}.{2}[]", sa.getSceneId(), sa.getActorId(), id);
					else
						id = MessageFormat.format("[BLUE]{0}.{1}[BLUE]", sa.getActorId(), id);
				} else
					id = MessageFormat.format("[BLUE]{0}[]", id);
			}

			return id;
		}

		private boolean isElse(AbstractIfAction parentAction, EndAction ea) {
			final String caID = ea.getControlActionID();
			ArrayList<Action> actions = parent.getActions();

			int idx = actions.indexOf(parentAction);

			for (int i = idx + 1; i < actions.size(); i++) {
				Action aa = actions.get(i);

				if (isControlAction(aa) && ((AbstractControlAction) aa).getControlActionID().equals(caID)) {
					if (aa == ea)
						return true;

					return false;
				}
			}

			return false;
		}

		private Action findParentAction(EndAction a) {
			final String caID = a.getControlActionID();
			ArrayList<Action> actions = parent.getActions();

			for (Action a2 : actions) {
				if (isControlAction(a2) && ((AbstractControlAction) a2).getControlActionID().equals(caID)) {
					return a2;
				}
			}

			return null;
		}

		@Override
		protected String getCellSubTitle(Action a) {
			if(a instanceof DisableActionAction)
				a = ((DisableActionAction) a).getAction();
			
			
			StringBuilder sb = new StringBuilder();

			Param[] params = ActionUtils.getParams(a);
			String actionName = ActionUtils.getName(a.getClass());

			for (Param p : params) {
				String name = p.name;

				if (name.equals("actor")
						|| (actionName != null && actionName.equals("Animation") && name.equals("animation")))
					continue;

				Field f = ActionUtils.getField(a.getClass(), p.name);

				try {
					final boolean accessible = f.isAccessible();
					f.setAccessible(true);
					Object o = f.get(a);
					if (o == null)
						continue;
					String v = o.toString();

					// Check world Scope for translations
					if (scope.equals(ScopePanel.WORLD_SCOPE))
						sb.append(name).append(": ").append(Ctx.project.getI18N().getWorldTranslation(v).replace("\n", "|")).append(' ');
					else
						sb.append(name).append(": ").append(Ctx.project.translate(v).replace("\n", "|")).append(' ');

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
