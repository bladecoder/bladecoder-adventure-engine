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
import java.util.Arrays;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.ParticleRenderer;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.TextRenderer;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteActor;

public class ActorList extends ModelList<Scene, BaseActor> {

	private ImageButton playerBtn;

	public ActorList(Skin skin) {
		super(skin, true);

		playerBtn = new ImageButton(skin);
		toolbar.addToolBarButton(playerBtn, "ic_player_small", "Set player", "Set player");
		playerBtn.setDisabled(true);

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				// EditorLogger.debug("ACTOR LIST ELEMENT SELECTED");
				int pos = list.getSelectedIndex();

				if (pos == -1) {
					Ctx.project.setSelectedActor((BaseActor) null);
				} else {
					BaseActor a = list.getItems().get(pos);
					Ctx.project.setSelectedActor(a);
				}

				toolbar.disableEdit(pos == -1);
				playerBtn.setDisabled(pos == -1);
			}
		});

		playerBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setPlayer();
			}
		});

		list.setCellRenderer(listCellRenderer);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ACTOR_SELECTED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				int pos = list.getSelectedIndex();

				// Element newActor = (Element) e.getNewValue();
				BaseActor newActor = Ctx.project.getSelectedActor();

				if (newActor == null)
					return;

				if (pos != -1) {
					BaseActor oldActor = list.getItems().get(pos);

					if (oldActor == newActor) {
						return;
					}
				}

				int i = list.getItems().indexOf(newActor, true);
				
				if (i >= 0) {
					list.setSelectedIndex(i);
					
					container.getActor().setScrollPercentY(i/(float)list.getItems().size);
				}
			}
		});

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {

				if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_DELETED)) {
					if (evt.getNewValue() instanceof BaseActor) {
						addElements(Ctx.project.getSelectedScene(),
								Arrays.asList(Ctx.project.getSelectedScene().getActors().values().toArray(new BaseActor[0])));
					}
				} else if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_CREATED)) {
					if (evt.getNewValue() instanceof BaseActor && !(evt.getSource() instanceof EditActorDialog)) {
						addElements(Ctx.project.getSelectedScene(),
								Arrays.asList(Ctx.project.getSelectedScene().getActors().values().toArray(new BaseActor[0])));
					}
				}
			}
		});
	}

	@Override
	protected void delete() {
		BaseActor a = removeSelected();

		parent.removeActor(a);

		// delete player attr if the actor to delete is the player
		if (parent.getPlayer() == a) {
			parent.setPlayer(null);
		}

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(a);

		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteActor(parent, a));

		Ctx.project.setModified();
	}

	@Override
	protected EditModelDialog<Scene, BaseActor> getEditElementDialogInstance(BaseActor a) {
		return new EditActorDialog(skin, parent, a);
	}
	
	@Override
	protected void edit() {
		BaseActor e = list.getSelected();

		if (e == null)
			return;

		EditModelDialog<Scene, BaseActor> dialog = getEditElementDialogInstance(e);
		dialog.show(getStage());
		
		dialog.setListener(new ChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				BaseActor e = ((EditModelDialog<Scene, BaseActor>) actor).getElement();
				
				// When the type is changed, a new element is created and it is needed to replace the previous element.
				if(e != list.getSelected()) {
					int i = list.getSelectedIndex();
					getItems().set(i, e);
					list.setSelectedIndex(i);
					list.invalidateHierarchy();				
				}			
			}
		});
	}

	private void setPlayer() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		BaseActor a = list.getItems().get(pos);

		if (a instanceof CharacterActor) {
			Ctx.project.getSelectedScene().setPlayer((CharacterActor) a);
			Ctx.project.setModified();
		}
	}

	@Override
	protected void copy() {
		BaseActor e = list.getSelected();

		if (e == null)
			return;

		clipboard = (BaseActor) ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(clipboard);
	}

	@Override
	protected void paste() {
		BaseActor newElement = (BaseActor) ElementUtils.cloneElement(clipboard);

		newElement.setId(
				ElementUtils.getCheckedId(newElement.getId(), parent.getActors().keySet().toArray(new String[0])));

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		parent.addActor(newElement);
		Ctx.project.getI18N().extractStrings(parent.getId(), newElement);
		
		if(newElement instanceof SpriteActor) {
			SpriteActor ia = (SpriteActor) newElement;
			ia.loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			ia.retrieveAssets();
		}

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();

		Ctx.project.setModified();
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<BaseActor> listCellRenderer = new CellRenderer<BaseActor>() {

		@Override
		protected String getCellTitle(BaseActor e) {
			return e.getId();
		}

		@Override
		protected String getCellSubTitle(BaseActor e) {
			if (e instanceof InteractiveActor)
				return Ctx.project.translate(((InteractiveActor) e).getDesc());

			return "";
		}

		@Override
		public TextureRegion getCellImage(BaseActor a) {

			boolean isPlayer = (a.getScene().getPlayer() == a);
			String u = null;

			if (isPlayer) {
				u = "ic_player";
			} else if (a instanceof CharacterActor) {
				u = "ic_character_actor";
			} else if (a instanceof SpriteActor) {
				ActorRenderer r = ((SpriteActor) a).getRenderer();

				if (r instanceof ImageRenderer) {
					u = "ic_sprite_actor";
				} else if (r instanceof AtlasRenderer) {
					u = "ic_sprite_actor";
				} else if (r instanceof SpineRenderer) {
					u = "ic_spine";
				} else if (r instanceof ParticleRenderer) {
					u = "ic_particles";
				} else if (r instanceof TextRenderer) {
					u = "ic_text";
				} else if (r instanceof Sprite3DRenderer) {
					u = "ic_3d";
				}
			} else if (a instanceof InteractiveActor) {
				u = "ic_base_actor";
			} else if (a instanceof ObstacleActor) {
				u = "ic_obstacle_actor";
			} else if (a instanceof AnchorActor) {
				u = "ic_anchor";
			}

			return Ctx.assetManager.getIcon(u);
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
