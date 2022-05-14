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

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.*;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteActor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ActorList extends ModelList<Scene, BaseActor> {

    private ImageButton playerBtn;
    private ImageButton visibilityBtn;
    private String filterText;

    public ActorList(Skin skin) {
        super(skin, true);

        // Eye button
        visibilityBtn = new ImageButton(skin);
        toolbar.addToolBarButton(visibilityBtn, "ic_eye", "Toggle Visibility", "Toggle Visibility in editor");
        visibilityBtn.setDisabled(false);
        visibilityBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleVisibility();
            }
        });

        // Player button
        playerBtn = new ImageButton(skin);
        toolbar.addToolBarButton(playerBtn, "ic_player_small", "Set player", "Set player");
        playerBtn.setDisabled(true);
        playerBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setPlayer();
            }
        });

        TextField tf = toolbar.addFilterBox(new EventListener() {

            @Override
            public boolean handle(Event e) {
                if (((TextField) e.getTarget()).getText() != filterText) {
                    filterText = ((TextField) e.getTarget()).getText();

                    addFilteredElements();
                }

                return false;
            }

        });

        filterText = tf.getText();

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

                    container.getActor().setScrollPercentY(i / (float) list.getItems().size);
                }
            }
        });

        Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_DELETED)) {
                    if (evt.getNewValue() instanceof BaseActor) {
                        addFilteredElements();
                    }
                } else if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_CREATED)) {
                    if (evt.getNewValue() instanceof BaseActor && !(evt.getSource() instanceof EditActorDialog)) {
                        addFilteredElements();
                    }
                }
            }
        });
    }

    private void addFilteredElements() {

        List<BaseActor> filtered = new ArrayList<>();

        for (BaseActor a : Ctx.project.getSelectedScene().getActors().values()) {
            if (filterText == null || filterText.isEmpty() || a.getId().contains(filterText)) {
                filtered.add(a);
            }
        }

        addElements(Ctx.project.getSelectedScene(), filtered);

    }

    private void toggleVisibility() {

        BaseActor e = list.getSelected();

        if (e == null)
            return;

        Ctx.project.toggleEditorVisibility(e);
    }

    @Override
    protected void delete() {
        BaseActor a = removeSelected();

        parent.removeActor(a);

        // delete player attr if the actor to delete is the player
        if (parent.getPlayer() == a) {
            parent.setPlayer(null);
        }

        if (a.getId().equals(parent.getWalkZone())) {
            parent.setWalkZone(null);
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

                // When the type is changed, a new element is created and it is needed to
                // replace the previous element.
                if (e != list.getSelected()) {
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

        if (newElement instanceof SpriteActor) {
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
            boolean enabled = Ctx.project.isEditorVisible(e);

            String text = e.getId();

            if (!enabled) {
                text = MessageFormat.format("[GRAY]{0}[]", text);
            }

            return text;
        }

        @Override
        protected String getCellSubTitle(BaseActor e) {
            boolean enabled = Ctx.project.isEditorVisible(e);

            String text = "";

            if (e instanceof SpriteActor && ((SpriteActor) e).getRenderer() instanceof TextRenderer
                    && ((TextRenderer) ((SpriteActor) e).getRenderer()).getText() != null) {
                text = Ctx.project.translate(((TextRenderer) ((SpriteActor) e).getRenderer()).getText()).replace("\n",
                        "|");
            }

            if (e instanceof InteractiveActor) {
                text = Ctx.project.translate(((InteractiveActor) e).getDesc());
            }

            if (!enabled && text != null && !text.isEmpty()) {
                text = MessageFormat.format("[GRAY]{0}[]", text);
            }

            return text;
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
                }
            } else if (a instanceof InteractiveActor) {
                u = "ic_base_actor";
            } else if (a instanceof ObstacleActor) {
                u = "ic_obstacle_actor";
            } else if (a instanceof AnchorActor) {
                u = "ic_anchor";
            } else if (a instanceof WalkZoneActor) {
                u = "ic_walkzone";
            } else {
                u = "ic_base_actor";
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
