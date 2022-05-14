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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.ImageUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CustomList;
import com.bladecoder.engineeditor.ui.panels.EditToolbar;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooser.SelectionMode;
import com.kotcrab.vis.ui.widget.file.FileChooser.ViewMode;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssetsList extends Table {
    private static final String[] ASSET_TYPES = {"atlases", "music", "sounds", "images", "spine", "particles", "voices"};

    private SelectBox<String> assetTypes;
    protected EditToolbar toolbar;
    protected CustomList<String> list;
    protected Skin skin;
    protected Container<ScrollPane> container;

    private File lastDir;

    public AssetsList(Skin skin) {
        super(skin);

        assetTypes = new SelectBox<>(skin);
        assetTypes.setItems(ASSET_TYPES);

        this.skin = skin;

        list = new CustomList<>(skin);

        Array<String> items = new Array<String>();
        list.setItems(items);

        ScrollPane scrollPane = new ScrollPane(list, skin);
        container = new Container<>(scrollPane);
        container.fill();
        container.prefHeight(1000);

        toolbar = new EditToolbar(skin);
        // debug();
        add(assetTypes).expandX().fillX();
        row();
        add(toolbar).expandX().fillX();
        row();
        add(container).expand().fill();

        toolbar.addCreateListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                create();
            }
        });

        toolbar.addEditListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                edit();
            }
        });

        toolbar.addDeleteListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                delete();
            }
        });

        list.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toolbar.disableEdit(false);
            }
        });

        Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent arg0) {
                toolbar.disableCreate(!Ctx.project.isLoaded());
                addAssets();
            }
        });

        assetTypes.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                addAssets();
            }
        });
    }

    private void addAssets() {
        list.getItems().clear();

        if (Ctx.project.isLoaded()) {
            String type = assetTypes.getSelected();
            String dir = getAssetDir(type);

            if (type.equals("images") || type.equals("atlases"))
                dir += "/1";

            String[] files = new File(dir).list(new FilenameFilter() {
                @Override
                public boolean accept(File arg0, String arg1) {
                    String type = assetTypes.getSelected();

                    return !type.equals("atlases") || arg1.endsWith(".atlas");
                }
            });

            if (files != null) {
                Arrays.sort(files);

                for (String f : files)
                    list.getItems().add(f);
            }

            if (list.getItems().size > 0) {
                list.setSelectedIndex(0);
            }
        }

        toolbar.disableCreate(!Ctx.project.isLoaded());
        list.invalidateHierarchy();
    }

    private String getAssetDir(String type) {
        String dir;

        if (type.equals("atlases")) {
            dir = Ctx.project.getAssetPath() + Project.ATLASES_PATH;
        } else if (type.equals("music")) {
            dir = Ctx.project.getAssetPath() + Project.MUSIC_PATH;
        } else if (type.equals("sounds")) {
            dir = Ctx.project.getAssetPath() + Project.SOUND_PATH;
        } else if (type.equals("images")) {
            dir = Ctx.project.getAssetPath() + Project.IMAGE_PATH;
            dir = Ctx.project.getAssetPath() + Project.IMAGE_PATH;
        } else if (type.equals("spine")) {
            dir = Ctx.project.getAssetPath() + Project.SPINE_PATH;
        } else if (type.equals("particles")) {
            dir = Ctx.project.getAssetPath() + Project.PARTICLE_PATH;
        } else if (type.equals("voices")) {
            dir = Ctx.project.getAssetPath() + Project.VOICE_PATH;
        } else {
            dir = Ctx.project.getAssetPath();
        }

        return dir;
    }

    private void create() {
        final String type = assetTypes.getSelected();

        if (type.equals("atlases")) {
            new CreateAtlasDialog(skin).show(getStage());

//			addAssets();

        } else if (type.equals("particles")) {
            //	Open the particle editor
            List<String> cp = new ArrayList<String>();
            cp.add(System.getProperty("java.class.path"));
            try {
                RunProccess.runJavaProccess("com.badlogic.gdx.tools.particleeditor.ParticleEditor", cp, null);
            } catch (IOException e) {
                Message.showMsgDialog(getStage(), "Error", "Error launching Particle Editor.");
                EditorLogger.printStackTrace(e);
            }
        } else {

            FileChooser fileChooser = new FileChooser(Mode.OPEN);

            fileChooser.setSelectionMode(SelectionMode.FILES);
            fileChooser.setMultiSelectionEnabled(true);

            fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
            fileChooser.setViewMode(ViewMode.LIST);

            getStage().addActor(fileChooser);
            if (lastDir != null)
                fileChooser.setDirectory(lastDir);
//			chooser.setTitle("Select the '" + type + "' asset files");


            FileTypeFilter typeFilter = new FileTypeFilter(true); //allow "All Types" mode where all files are shown

            switch (type) {
                case "images":
                    typeFilter.addRule("Images (*.png, *.jpg, *.etc1)", "jpg", "png", "etc1");
                    break;
                case "music":
                case "sounds":
                case "voices":
                    typeFilter.addRule("Sound (*.mp3, *.wav, *.ogg)", "wav", "mp3", "ogg");
                    break;
                case "spine":
                    typeFilter.addRule("Spine (*.skel, *.json)", "skel", "json");
                    break;
                default:
                    typeFilter.addRule("All", "");
                    break;
            }

            fileChooser.setFileTypeFilter(typeFilter);

            fileChooser.setListener(new FileChooserListener() {

                @Override
                public void selected(Array<FileHandle> files) {

                    try {
                        String dirName = getAssetDir(type);
                        lastDir = files.get(0).parent().file();

                        // Si no existe la carpeta la creamos
                        File dir = new File(dirName);
                        if (!dir.exists())
                            dir.mkdir();

                        for (FileHandle f : files) {
                            if (type.equals("images")) {
                                List<String> res = Ctx.project.getResolutions();

                                for (String r : res) {
                                    File destFile = new File(dirName + "/" + r + "/" + f.file().getName());
                                    float scale = Float.parseFloat(r);

                                    if (scale != 1.0f) {

                                        ImageUtils.scaleImageFile(f.file(), destFile, scale);
                                    } else {
                                        Files.copy(f.file().toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                            } else {
                                File destFile = new File(dir, f.file().getName());
                                Files.copy(f.file().toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }

                        }

                        addAssets();
                    } catch (Exception ex) {
                        String msg = "Something went wrong while getting the assets.\n\n"
                                + ex.getClass().getSimpleName() + " - " + ex.getMessage();
                        Message.showMsgDialog(getStage(), "Error", msg);
                        EditorLogger.printStackTrace(ex);
                    }
                }

                @Override
                public void canceled() {

                }
            });
        }
    }

    private void edit() {
        if (Desktop.isDesktopSupported()) {
            String type = assetTypes.getSelected();
            String dir = getAssetDir(type);

            if (type.equals("images") || type.equals("atlases"))
                dir += "/1";

            try {
                Desktop.getDesktop().open(new File(dir));
            } catch (IOException e1) {
                String msg = "Something went wrong while opening assets folder.\n\n" + e1.getClass().getSimpleName()
                        + " - " + e1.getMessage();
                Message.showMsgDialog(getStage(), "Error", msg);
            }
        }
    }

    private void delete() {
        String type = assetTypes.getSelected();
        String dir = getAssetDir(type);

        String name = list.getSelected();
        try {
            if (type.equals("images") || type.equals("atlases")) {
                List<String> res = Ctx.project.getResolutions();

                for (String r : res) {
                    File file = new File(dir + "/" + r + "/" + name);

                    file.delete();

                    // delete pages on atlases
                    if (type.equals("atlases")) {
                        File atlasDir = new File(dir + "/" + r);

                        File[] files = atlasDir.listFiles();

                        if (files != null)
                            for (File f : files) {
                                String destName = f.getName();
                                String nameWithoutExt = name.substring(0, name.lastIndexOf('.'));
                                String destNameWithoutExt = destName.substring(0, destName.lastIndexOf('.'));

                                if (destNameWithoutExt.length() < nameWithoutExt.length())
                                    continue;

                                String suffix = destNameWithoutExt.substring(nameWithoutExt.length());

                                if (!suffix.isEmpty() && !suffix.matches("[0-9]+"))
                                    continue;

                                if (destName.startsWith(nameWithoutExt) && destName.toLowerCase().endsWith(".png"))
                                    Files.delete(f.toPath());
                            }
                    }
                }
            } else {
                File file = new File(dir + "/" + name);
                file.delete();
            }

            addAssets();
        } catch (Exception ex) {
            String msg = "Something went wrong while deleting the asset.\n\n" + ex.getClass().getSimpleName() + " - "
                    + ex.getMessage();
            Message.showMsgDialog(getStage(), "Error", msg);
            EditorLogger.printStackTrace(ex);
        }
    }
}
