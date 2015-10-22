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

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;

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
					Ctx.project.setSelectedActor((BaseActor)null);
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
				if (i >= 0)
					list.setSelectedIndex(i);
			}
		});

//		Ctx.project.getWorld().addPropertyChangeListener(new PropertyChangeListener() {
//			@Override
//			public void propertyChange(PropertyChangeEvent e) {
//				if (e.getPropertyName().equals(BaseDocument.NOTIFY_ELEMENT_DELETED)) {
//					if (((Element) e.getNewValue()).getTagName().equals("actor")) {
//						Element el = (Element) e.getNewValue();
//
//						for (BaseActor e2 : list.getItems()) {
//							if (e2 == el) {
//								int pos = list.getItems().indexOf(e2, true);
//
//								list.getItems().removeIndex(pos);
//
//								clipboard = e2;
//								I18NUtils.putTranslationsInElement(doc, clipboard);
//								toolbar.disablePaste(false);
//
//								if (pos > 0)
//									list.setSelectedIndex(pos - 1);
//								else if (pos == 0 && list.getItems().size > 0)
//									list.setSelectedIndex(0);
//							}
//						}
//					}
//				} else if (e.getPropertyName().equals("actor") && e.getSource() instanceof UndoOp) {
//					BaseActor el = (BaseActor) e.getNewValue();
//
//					if (getItems().indexOf(el, true) != -1)
//						return;
//
//					addItem(el);
//
//					int i = getItems().indexOf(el, true);
//					if (i != -1)
//						list.setSelectedIndex(i);
//
//					list.invalidateHierarchy();
//				}
//			}
//		});
	}

	@Override
	protected void delete() {
//		int pos = list.getSelectedIndex();
//
//		if (pos == -1)
//			return;
//
//		Element e = list.getItems().get(pos);
//
//		// delete player attr if the actor to delete is the player
//		if (((Element) e.getParentNode()).getAttribute("player").equals(e.getAttribute("id"))) {
//			((Element) e.getParentNode()).removeAttribute("player");
//		}
//
//		super.delete();
	}

	@Override
	protected EditModelDialog<Scene, BaseActor> getEditElementDialogInstance(BaseActor a) {
		
		return null;
	}

	private void setPlayer() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		BaseActor a = list.getItems().get(pos);

		if (a instanceof CharacterActor) {
			a.getScene().setPlayer((CharacterActor)a);
		}
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
			if(e instanceof InteractiveActor)
				return Ctx.project.getSelectedChapter().getTranslation(((InteractiveActor) e).getDesc());
			
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
