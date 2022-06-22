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
package com.bladecoder.engineeditor;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.EditorLogger.Levels;
import com.bladecoder.engineeditor.common.Versions;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends Lwjgl3Application {

    public static void main(final String[] args) {

        if(System.getProperty("os.name").contains("Mac"))
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");

        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();

        cfg.setTitle("Adventure Editor v" + Versions.getVersion());

        cfg.setResizable(true);
        cfg.useVsync(true);

        List<String> iconList = new ArrayList<>();

        if (Main.class.getResource("/images/ic_app64.png") != null)
            iconList.add("images/ic_app64.png");

        if (Main.class.getResource("/images/ic_app32.png") != null)
            iconList.add("images/ic_app32.png");

        if (Main.class.getResource("/images/ic_app16.png") != null)
            iconList.add("images/ic_app16.png");

        cfg.setWindowIcon(FileType.Internal, iconList.toArray(new String[0]));
        cfg.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        parseArgs(args);

        new Main(new Editor(), cfg);
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-f") && i < args.length - 1) {
                try {
                    File file = new File(args[i + 1]).getCanonicalFile();
                    Ctx.project.loadProject(file);
                } catch (Exception ex) {
                    EditorLogger.printStackTrace(ex);
                }
            } else if (args[i].equals("-d")) {
                EditorLogger.setDebugLevel(Levels.DEBUG);
            }
        }
    }

    public Main(Editor editor, Lwjgl3ApplicationConfiguration cfg) {
        super(editor, cfg);
    }

    @Override
    public void exit() {
        ((Editor) getApplicationListener()).exit();
    }

    public void exitSaved() {
        super.exit();
    }
}
