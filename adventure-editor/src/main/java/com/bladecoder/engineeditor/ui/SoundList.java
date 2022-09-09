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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteSound;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

public class SoundList extends ModelList<World, SoundDesc> {

    private ImageButton playBtn;
    private Sound playingSound = null;

    public SoundList(Skin skin) {
        super(skin, true);

        playBtn = new ImageButton(skin);
        toolbar.addToolBarButton(playBtn, "ic_check", "Play Sound", "Plays the selected sound");
        playBtn.setDisabled(true);

        setCellRenderer(listCellRenderer);

        Ctx.project.addPropertyChangeListener(Project.NOTIFY_CHAPTER_LOADED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                addElements(Ctx.project.getWorld(),
                        Arrays.asList(Ctx.project.getWorld().getSounds().values().toArray(new SoundDesc[0])));
            }
        });

        list.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int pos = list.getSelectedIndex();

                toolbar.disableEdit(pos == -1);
                playBtn.setDisabled(pos == -1);
            }
        });

        playBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundDesc selected = list.getSelected();

                if (playingSound != null) {
                    playingSound.stop();
                    playingSound.dispose();
                    playingSound = null;
                }

                try {
                    playingSound = Gdx.audio.newSound(
                            new FileHandle(
                                    Ctx.project.getAssetPath() + Project.SOUND_PATH + "/" + selected.getFilename()));

                    playingSound.play(selected.getVolume(), 1, selected.getPan());

                    Timer.schedule(new Task() {

                        @Override
                        public void run() {
                            if (playingSound != null) {
                                playingSound.stop();
                                playingSound.dispose();
                                playingSound = null;
                            }
                        }
                    }, 5);
                } catch (Exception e) {
                    Message.showMsg(getStage(), "Could not play sound: " + e.getMessage(), 4);
                }
            }
        });
    }

    @Override
    protected EditSoundDialog getEditElementDialogInstance(SoundDesc s) {
        return new EditSoundDialog(skin, parent, s);
    }

    @Override
    protected void delete() {

        SoundDesc s = removeSelected();

        parent.getSounds().remove(s.getId());

        // UNDO
        Ctx.project.getUndoStack().add(new UndoDeleteSound(s));
        Ctx.project.setModified();
    }

    @Override
    protected void copy() {
        SoundDesc e = list.getSelected();

        if (e == null)
            return;

        clipboard = (SoundDesc) ElementUtils.cloneElement(e);
        toolbar.disablePaste(false);
    }

    @Override
    protected void paste() {
        SoundDesc newElement = (SoundDesc) ElementUtils.cloneElement(clipboard);

        int pos = list.getSelectedIndex() + 1;

        list.getItems().insert(pos, newElement);

        String id = newElement.getId();

        if (parent.getSounds() != null)
            id = ElementUtils.getCheckedId(newElement.getId(),
                    parent.getSounds().keySet().toArray(new String[parent.getSounds().size()]));

        newElement.setId(id);

        parent.getSounds().put(newElement.getId(), newElement);

        list.setSelectedIndex(pos);
        list.invalidateHierarchy();

        Ctx.project.setModified();
    }

    // -------------------------------------------------------------------------
    // ListCellRenderer
    // -------------------------------------------------------------------------
    private static final CellRenderer<SoundDesc> listCellRenderer = new CellRenderer<SoundDesc>() {

        @Override
        protected String getCellTitle(SoundDesc e) {
            return e.getId();
        }

        StringBuilder sb = new StringBuilder();

        @Override
        protected String getCellSubTitle(SoundDesc e) {
            sb.setLength(0);

            String filename = e.getFilename();
            if (filename != null && !filename.isEmpty())
                sb.append("filename: ").append(filename);

            sb.append(" loop: ").append(e.getLoop());
            sb.append(" volume: ").append(e.getVolume());

            return sb.toString();
        }

        @Override
        protected boolean hasSubtitle() {
            return true;
        }
    };

}
