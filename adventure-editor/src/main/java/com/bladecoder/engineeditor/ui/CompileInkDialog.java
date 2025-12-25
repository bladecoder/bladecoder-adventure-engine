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
import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.compiler.IFileHandler;
import com.bladecoder.ink.runtime.Story;
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
    private static final String LANG_PROP = "compileink.lang";
    private static final String EXTRACT_TEXTS_PROP = "compileink.extractTexts";

    private static final String INFO = "Compile the Ink script.";

    private final InputPanel file;
    private final InputPanel extractTexts;
    private final InputPanel lang;

    public CompileInkDialog(Skin skin) {
        super("COMPILE INK SCRIPT", skin);

        file = new FileInputPanel(skin, "Select the Ink script", "Select the Ink source script for your chapter",
                FileInputPanel.DialogType.OPEN_FILE);

        extractTexts = InputPanelFactory.createInputPanel(skin, "Extract texts",
                "Extracts all texts in a .properties for I18N.", Param.Type.BOOLEAN, true, "true");

        lang = InputPanelFactory.createInputPanel(skin, "Lang code",
                "The languaje code (ex. 'fr') where the texts are extracted. Empty for default.", false);

        addInputPanel(file);
        addInputPanel(extractTexts);
        addInputPanel(lang);

        setInfo(INFO);

        if (Ctx.project.getEditorConfig().getProperty(FILE_PROP) != null)
            file.setText(Ctx.project.getEditorConfig().getProperty(FILE_PROP));

        lang.setText(Ctx.project.getEditorConfig().getProperty(LANG_PROP));

        if (Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP) != null)
            extractTexts.setText(Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP));
    }

    @Override
    protected void ok() {
        compileInk();

        Ctx.project.getEditorConfig().setProperty(FILE_PROP, file.getText());

        if (lang.getText() != null)
            Ctx.project.getEditorConfig().setProperty(LANG_PROP, lang.getText());

        Ctx.project.getEditorConfig().setProperty(EXTRACT_TEXTS_PROP, extractTexts.getText());
    }

    @Override
    protected boolean validateFields() {
        boolean ok = file.validateField();

        return ok;
    }

    private void compileInk() {
        FileHandle inFile = Gdx.files.absolute(file.getText());
        String outfile = Ctx.project.getModelPath() + "/" + inFile.name() + ".json";

        // read inFile content as String
        String inkContent;
        try {
            inkContent = inFile.readString("UTF-8");

            Compiler.Options options = new Compiler.Options();
            options.sourceFilename = "main.ink";
            options.fileHandler = new IFileHandler() {
                @Override
                public String resolveInkFilename(String includeName) {
                    return inFile.parent() + "/" + includeName;
                }

                @Override
                public String loadInkFileContents(String fullFilename) {
                    return Gdx.files.absolute(fullFilename).readString("UTF-8");
                }
            };

            Compiler compiler = new Compiler(inkContent, options);
            Story story = compiler.compile();

            if (story == null) {
                Message.showMsgDialog(getStage(), "Error", "Error compiling Ink script.");
                return;
            }

            String outString = story.toJson();

            FileHandle outFile = Gdx.files.absolute(outfile);
            outFile.writeString(outString, false);

        } catch (Exception e) {
            Message.showMsgDialog(getStage(), "Error", "Error compiling Ink script: " + e.getMessage());
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
}
