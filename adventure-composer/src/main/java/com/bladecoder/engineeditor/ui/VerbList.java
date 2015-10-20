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
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;
import com.bladecoder.engineeditor.ui.components.ScopePanel;

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
					addElements(World.getInstance().getVerbManager(), Arrays
							.asList(World.getInstance().getVerbManager().getVerbs().values().toArray(new Verb[0])));
				} else if (SCENE_SCOPE.equals(scope)) {
					addElements(Ctx.project.getSelectedScene().getVerbManager(), Arrays.asList(
							Ctx.project.getSelectedScene().getVerbManager().getVerbs().values().toArray(new Verb[0])));
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
	}

	public void changeActor() {
		scopePanel.scopeChanged(scopePanel.getScope());
	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(Verb e) {
		// return new EditVerbDialog(skin, doc, parent, e);
		return null;
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
			
	// TODO UNDO
//			UndoOp undoOp = new UndoDeleteElement(doc, e);
//			Ctx.project.getUndoStack().add(undoOp);
//			doc.deleteElement(e);

	// TODO TRANSLATIONS
//			I18NUtils.putTranslationsInElement(doc, clipboard);

		// Clear actions here because change event doesn't call when deleting
		// the last element
		if (list.getSelectedIndex() == -1)
			addActions();
	}

	private void addActions() {
		int pos = list.getSelectedIndex();

		Verb v = null;

		if (pos != -1) {
			v = list.getItems().get(pos);
			actionList.addElements(v, v.getActions());
		} else {
			actionList.addElements(null, null);
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

			StringBuilder sb = new StringBuilder(e.getId());

			if (state != null)
				sb.append("when ").append(state);

			if (target != null)
				sb.append(" with target '").append(target).append("'");

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
