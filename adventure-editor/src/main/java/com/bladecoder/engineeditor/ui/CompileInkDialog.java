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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.HttpUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.ModelTools;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.common.ZipUtils;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.FileInputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CompileInkDialog extends EditDialog {

    private static final String FILE_PROP = "compileink.file";
    private static final String INKLECATE_PROP = "compileink.inklecate";
    private static final String LANG_PROP = "compileink.lang";
    private static final String EXTRACT_TEXTS_PROP = "compileink.extractTexts";

    private static final String INFO = "Compile the ink script using Inklecate.\n Inklecate must be installed in your computer.";

    private InputPanel inklecatePath;
    private InputPanel file;
    private InputPanel extractTexts;
    private InputPanel lang;

    public CompileInkDialog(Skin skin) {
        super("COMPILE INK SCRIPT", skin);

        inklecatePath = new FileInputPanel(skin, "Select the inklecate folder",
                "Select the folder where the inklecate is installed", FileInputPanel.DialogType.DIRECTORY);

        file = new FileInputPanel(skin, "Select the Ink script", "Select the Ink source script for your chapter",
                FileInputPanel.DialogType.OPEN_FILE);

        extractTexts = InputPanelFactory.createInputPanel(skin, "Extract texts",
                "Extracts all texts in a .properties for I18N.", Param.Type.BOOLEAN, true, "true");

        lang = InputPanelFactory.createInputPanel(skin, "Lang code",
                "The languaje code (ex. 'fr') where the texts are extracted. Empty for default.", false);

        addInputPanel(inklecatePath);
        addInputPanel(file);
        addInputPanel(extractTexts);
        addInputPanel(lang);

        setInfo(INFO);

        if (Ctx.project.getEditorConfig().getProperty(FILE_PROP) != null)
            file.setText(Ctx.project.getEditorConfig().getProperty(FILE_PROP));

        if (Ctx.project.getEditorConfig().getProperty(INKLECATE_PROP) != null)
            inklecatePath.setText(Ctx.project.getEditorConfig().getProperty(INKLECATE_PROP));

        lang.setText(Ctx.project.getEditorConfig().getProperty(LANG_PROP));

        if (Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP) != null)
            extractTexts.setText(Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP));

        // Add the 'download' button to the Path field.
        TextButton downloadButton = new TextButton("Download Inklecate", skin, "no-toggled");

        downloadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                if (SharedLibraryLoader.isWindows) {
                    download(
                            "https://github.com/inkle/ink/releases/download/v1.1.1/inklecate_windows.zip",
                            "inklecate.zip");
                } else if (SharedLibraryLoader.isLinux) {
                    download("https://github.com/inkle/ink/releases/download/v1.1.1/inklecate_linux.zip",
                            "inklecate.zip");
                } else if (SharedLibraryLoader.isMac) {
                    download("https://github.com/inkle/ink/releases/download/v1.1.1/inklecate_mac.zip",
                            "inklecate.zip");
                }
            }
        });

        Table t2 = new Table();
        Actor a2 = inklecatePath.getField();
        Cell<?> c2 = inklecatePath.getCell(a2);
        t2.add(a2);
        t2.add(downloadButton);
        c2.setActor(t2);
    }

    @Override
    protected void ok() {
        compileInk();

        Ctx.project.getEditorConfig().setProperty(FILE_PROP, file.getText());
        Ctx.project.getEditorConfig().setProperty(INKLECATE_PROP, inklecatePath.getText());

        if (lang.getText() != null)
            Ctx.project.getEditorConfig().setProperty(LANG_PROP, lang.getText());

        Ctx.project.getEditorConfig().setProperty(EXTRACT_TEXTS_PROP, extractTexts.getText());
    }

    @Override
    protected boolean validateFields() {
        boolean ok = true;

        if (!inklecatePath.validateField())
            ok = false;

        if (!file.validateField())
            ok = false;

        return ok;
    }

    private void compileInk() {
        String outfile = Ctx.project.getModelPath() + "/" + new File(file.getText()).getName() + ".json";
        List<String> params = new ArrayList<>();
        params.add("-o");
        params.add(outfile);
        params.add(file.getText());
        boolean ok = RunProccess.runInklecate(new File(inklecatePath.getText()), params);

        if (!ok) {
            Message.showMsgDialog(getStage(), "Error", "Error compiling Ink script.");
            return;
        }

        if (extractTexts.getText().equals("true")) {
            try {
                ModelTools.extractInkTexts(outfile, lang.getText());
            } catch (IOException e) {
                Message.showMsgDialog(getStage(), "Error extracting Ink texts.", e.getMessage());
                return;
            }
        }

        Message.showMsg(getStage(), "Ink script compiled successfully", 2);
    }

    private void download(String url, String fileName) {
        FileChooser fileChooser = new FileChooser(FileChooser.Mode.OPEN);
        fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
        fileChooser.setViewMode(FileChooser.ViewMode.LIST);

        fileChooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
        getStage().addActor(fileChooser);

        fileChooser.setListener(new FileChooserListener() {

            @Override
            public void selected(Array<FileHandle> files) {
                try {
                    File zipFile = new File(files.get(0).file(), fileName);
                    HttpUtils.downloadAsync(new URL(url), new FileOutputStream(zipFile), new HttpUtils.Callback() {
                        @Override
                        public void updated(int length, int totalLength) {
                            final int progress = ((int) (((double) length / (double) totalLength) * 100));
                            Message.showMsg(getStage(), "Downloading JDK... " + progress + "%", true);
                        }

                        @Override
                        public void completed() {
                            File outputFolder = new File(files.get(0).file(), "inklecate");
                            try {
                                // create output folder
                                outputFolder.mkdirs();

                                ZipUtils.unzip(zipFile, outputFolder.toPath());
                                zipFile.delete();
                                // add execution permission to the inklecate file
                                if (SharedLibraryLoader.isLinux || SharedLibraryLoader.isMac) {
                                    File inklecateFile = new File(outputFolder, "inklecate");
                                    inklecateFile.setExecutable(true);
                                }
                            } catch (IOException e) {
                                Message.showMsg(getStage(), "Error uncompressing .zip: " + e.getMessage(), true);
                                return;
                            }
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    Message.hideMsg();
                                    inklecatePath.setText(outputFolder.getAbsolutePath());
                                }
                            });
                        }

                        @Override
                        public void canceled() {
                            Message.showMsgDialog(getStage(), "Error", "Download cancelled.");
                        }

                        @Override
                        public void error(IOException ex) {
                            Message.showMsgDialog(getStage(), "Error", "Download error: " + ex.getMessage());
                        }
                    });
                } catch (FileNotFoundException | MalformedURLException e) {
                    Message.showMsgDialog(getStage(), "Error", "Download error: " + e.getMessage());
                }
            }

            @Override
            public void canceled() {

            }
        });
    }
}
