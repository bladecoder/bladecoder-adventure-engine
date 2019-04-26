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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.ui.panels.ScopePanel;
import com.bladecoder.engineeditor.undo.UndoDeleteVerb;

public class VerbList extends ModelList<VerbManager, Verb> {

	public static final String VERBS[] = { "lookat", "pickup", "talkto", "use", "leave", "enter", "exit", "init",
			"test", "custom" };

	private ActionList actionList;

	private ScopePanel scopePanel;

	public VerbList(Skin skin) {
		super(skin, true);

		clearChildren();

		scopePanel = new ScopePanel(skin) {

			@Override
			public void scopeChanged(String scope) {
				if (WORLD_SCOPE.equals(scope)) {
					addElements(Ctx.project.getWorld().getVerbManager(), Arrays
							.asList(Ctx.project.getWorld().getVerbManager().getVerbs().values().toArray(new Verb[0])));
				} else if (SCENE_SCOPE.equals(scope)) {
					if (Ctx.project.getSelectedScene() != null)
						addElements(Ctx.project.getSelectedScene().getVerbManager(), Arrays.asList(Ctx.project
								.getSelectedScene().getVerbManager().getVerbs().values().toArray(new Verb[0])));
					else
						addElements(null, null);
				} else if (ACTOR_SCOPE.equals(scope)) {
					BaseActor a = Ctx.project.getSelectedActor();
					if (a instanceof InteractiveActor) {
						addElements(((InteractiveActor) a).getVerbManager(), Arrays.asList(
								((InteractiveActor) a).getVerbManager().getVerbs().values().toArray(new Verb[0])));
					} else {
						addElements(null, null);
					}
				}
			}
		};

		add(scopePanel).expandX().fillX();
		row();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();

		actionList = new ActionList(skin);

		row();
		add(actionList).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addActions();
			}
		});

		list.setCellRenderer(listCellRenderer);
		listCellRenderer.layout(list.getStyle());
		container.minHeight(listCellRenderer.getItemHeight() * 5);
		container.maxHeight(listCellRenderer.getItemHeight() * 5);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ELEMENT_CREATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof Verb && !(evt.getSource() instanceof EditVerbDialog)) {
					if (ScopePanel.WORLD_SCOPE.equals(scopePanel.getScope())) {
						addElements(Ctx.project.getWorld().getVerbManager(), Arrays.asList(
								Ctx.project.getWorld().getVerbManager().getVerbs().values().toArray(new Verb[0])));
					} else if (ScopePanel.SCENE_SCOPE.equals(scopePanel.getScope())) {
						addElements(Ctx.project.getSelectedScene().getVerbManager(), Arrays.asList(Ctx.project
								.getSelectedScene().getVerbManager().getVerbs().values().toArray(new Verb[0])));
					} else if (ScopePanel.ACTOR_SCOPE.equals(scopePanel.getScope())) {
						BaseActor a = Ctx.project.getSelectedActor();
						if (a instanceof InteractiveActor) {
							addElements(((InteractiveActor) a).getVerbManager(), Arrays.asList(
									((InteractiveActor) a).getVerbManager().getVerbs().values().toArray(new Verb[0])));
						} else {
							addElements(null, null);
						}
					}
				}
			}
		});
	}

	public void changeActor() {
		scopePanel.scopeChanged(scopePanel.getScope());
	}

	@Override
	protected EditModelDialog<VerbManager, Verb> getEditElementDialogInstance(Verb e) {
		return new EditVerbDialog(skin, scopePanel.getScope(), parent, e);
	}

	@Override
	public void addElements(VerbManager vm, List<Verb> elements) {
		super.addElements(vm, elements);
		addActions();
	}

	@Override
	protected void delete() {

		Verb v = removeSelected();

		parent.getVerbs().remove(v.getHashKey());

		// TRANSLATIONS
		if (scopePanel.getScope().equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().putTranslationsInElement(v, true);
		else
			Ctx.project.getI18N().putTranslationsInElement(v, false);

		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteVerb(parent, v));

		// Clear actions here because change event doesn't call when deleting
		// the last element
		if (list.getSelectedIndex() == -1)
			addActions();

		Ctx.project.setModified();
	}

	@Override
	protected void copy() {
		Verb e = list.getSelected();

		if (e == null)
			return;

		clipboard = (Verb) ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		if (scopePanel.getScope().equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().putTranslationsInElement(clipboard, true);
		else
			Ctx.project.getI18N().putTranslationsInElement(clipboard, false);
	}

	@Override
	protected void paste() {
		Verb newElement = (Verb) ElementUtils.cloneElement(clipboard);

		// Check for id duplicates
		String[] keys = new String[parent.getVerbs().size()];
		Verb[] values = parent.getVerbs().values().toArray(new Verb[0]);

		for (int i = 0; i < keys.length; i++) {
			keys[i] = values[i].getId();
		}

		newElement.setId(ElementUtils.getCheckedId(newElement.getId(), keys));

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		parent.addVerb(newElement);

		if (scopePanel.getScope().equals(ScopePanel.WORLD_SCOPE))
			Ctx.project.getI18N().extractStrings(null, null, newElement);
		else if (scopePanel.getScope().equals(ScopePanel.SCENE_SCOPE))
			Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(), null, newElement);
		else
			Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(),
					Ctx.project.getSelectedActor().getId(), newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();

		Ctx.project.setModified();
	}

	private void addActions() {
		int pos = list.getSelectedIndex();

		Verb v = null;

		if (pos != -1) {
			v = list.getItems().get(pos);
			actionList.setScope(scopePanel.getScope());
			actionList.addElements(v, v.getActions());
		} else {
			actionList.addElements(null, null);
			actionList.setScope(null);
		}
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Verb> listCellRenderer = new CellRenderer<Verb>() {

		@Override
		protected String getCellTitle(Verb e) {
			return e.getId();
		}

		@Override
		protected String getCellSubTitle(Verb e) {
			String state = e.getState();
			String target = e.getTarget();

			StringBuilder sb = new StringBuilder();

			if (state != null)
				sb.append("when ").append(state);

			if (target != null)
				sb.append(" target: '").append(target).append("'");

			if (e.getIcon() != null)
				sb.append(" icon: '").append(e.getIcon()).append("'");

			return sb.toString();
		}

		@Override
		public TextureRegion getCellImage(Verb e) {
			boolean custom = true;

			String verbName = e.getId();
			for (String v : VERBS) {
				if (v.equals(verbName)) {
					custom = false;
					break;
				}
			}

			String iconName = MessageFormat.format("ic_{0}", e.getId());
			TextureRegion image = null;

			if (!custom)
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
