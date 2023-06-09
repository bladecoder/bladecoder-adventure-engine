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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;
import com.badlogicgames.packr.PackrConfig.Platform;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.HttpUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.OrderedProperties;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.FileInputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class PackageDialog extends EditDialog {
    private static final String ARCH_PROP = "package.arch";
    private static final String DIR_PROP = "package.dir";

    private static final String DESKTOP_LAUNCHER = "DesktopLauncher.java";

    private static final String INFO = "Package the Game for distribution";
    private static final String[] ARCHS = {"desktop", "android", "ios"};
    private static final String[] DESKTOP_TYPES = {"Bundle JRE", "Runnable jar"};
    private static final String[] ANDROID_TYPES = {".apk", ".aab"};
    private static final String[] OSS = {"windows64", "linux64", "macOS-x86"};

    private final InputPanel arch;
    private final InputPanel dir;

    private final InputPanel desktopType;
    private final InputPanel androidType;

    private final InputPanel os;
    private final FileInputPanel linux64JRE;
    private final FileInputPanel winJRE64;
    private final FileInputPanel osxJRE;
    private final InputPanel version;
    private final InputPanel icon;
    private final InputPanel versionCode;
    private final InputPanel androidSDK;
    private final InputPanel expansionFile;
    private final InputPanel androidKeyStore;
    private final InputPanel androidKeyAlias;
    private final InputPanel androidKeyStorePassword;
    private final InputPanel androidKeyAliasPassword;

    private final InputPanel iosSignIdentity;
    private final InputPanel iosProvisioningProfile;

    private final InputPanel customBuildParameters;

    private final InputPanel[] options;

    public PackageDialog(final Skin skin) {
        super("PACKAGE GAME", skin);

        arch = InputPanelFactory.createInputPanel(skin, "Architecture", "Select the target Architecture for the game",
                ARCHS, true);
        dir = new FileInputPanel(skin, "Output Directory", "Select the output directory to put the package",
                FileInputPanel.DialogType.DIRECTORY);
        desktopType = InputPanelFactory.createInputPanel(skin, "Type", "Select the package type", DESKTOP_TYPES, true);
        androidType = InputPanelFactory.createInputPanel(skin, "Type", "Select the package type", ANDROID_TYPES, true);
        os = InputPanelFactory.createInputPanel(skin, "OS", "Select the OS of the package", OSS, true);

        FileTypeFilter typeFilter = new FileTypeFilter(true);
        typeFilter.addRule("Compressed files", "zip", "tar.gz");

        linux64JRE = new FileInputPanel(skin, "JRE lin64",
                "Select the 64 bits Linux JRE Location to bundle. Must be a .zip or a .tar.gz file",
                FileInputPanel.DialogType.OPEN_FILE);
        linux64JRE.setFileTypeFilter(typeFilter);

        winJRE64 = new FileInputPanel(skin, "JRE win64",
                "Select the Windows 64 bits JRE Location to bundle. Must be a .zip or a .tar.gz file",
                FileInputPanel.DialogType.OPEN_FILE);
        winJRE64.setFileTypeFilter(typeFilter);

        osxJRE = new FileInputPanel(skin, "JRE mac-x86",
                "Select the MacOS-x86 JRE Location to bundle. Must be a .zip or a .tar.gz file",
                FileInputPanel.DialogType.OPEN_FILE);
        osxJRE.setFileTypeFilter(typeFilter);

        version = InputPanelFactory.createInputPanel(skin, "Version", "Select the package version", true);
        icon = new FileInputPanel(skin, "MacOS Icon", "The icon (.icns) for the Mac package. It is not mandatory.",
                FileInputPanel.DialogType.OPEN_FILE, false);
        versionCode = InputPanelFactory.createInputPanel(skin, "Version Code", "An integer that identify the version.",
                Type.INTEGER, true);
        androidSDK = new FileInputPanel(skin, "SDK",
                "Select the Android SDK Location. If empty, the ANDROID_HOME variable will be used.",
                FileInputPanel.DialogType.DIRECTORY, false);
        expansionFile = InputPanelFactory.createInputPanel(skin, "Expansion File",
                "If your assets exceeds 100mb, you have to create an expansion file to upload the game to the Play " +
                        "Store.",
                Param.Type.BOOLEAN, true, "false");
        androidKeyStore = new FileInputPanel(skin, "KeyStore", "Select the Key Store Location",
                FileInputPanel.DialogType.OPEN_FILE);
        androidKeyAlias = InputPanelFactory.createInputPanel(skin, "KeyAlias", "Select the Key Alias", true);

        androidKeyStorePassword = InputPanelFactory.createInputPanel(skin, "KeyStorePasswd", "Key Store Password",
                true);
        androidKeyAliasPassword = InputPanelFactory.createInputPanel(skin, "KeyAliasPasswd", "Key Alias Password",
                true);

        iosSignIdentity = InputPanelFactory.createInputPanel(skin, "Sign Identity",
                "Empty for auto select.\nThis field matches against the start of the certificate name. Alternatively " +
                        "you can use a certificate fingerprint.\nIf the value is enclosed in / a regexp search will " +
                        "be" +
                        " done against the certificate name instead.\nRun the command 'security find-identity -v -p " +
                        "codesigning' or use theKeyChain Access OS X app to view your installed certificates.",
                false);

        iosProvisioningProfile = InputPanelFactory.createInputPanel(skin, "Provisioning Profile",
                "Empty for auto select.", false);

        customBuildParameters = InputPanelFactory.createInputPanel(skin, "Custom build parameters",
                "You can add extra build parameters for customized build scripts.", false);

        options = new InputPanel[]{androidType, desktopType, os, linux64JRE, winJRE64, osxJRE, version, icon,
                versionCode, androidSDK, expansionFile, androidKeyStore, androidKeyAlias, iosSignIdentity,
                iosProvisioningProfile, customBuildParameters};

        addInputPanel(arch);
        addInputPanel(dir);

        for (InputPanel i : options) {
            addInputPanel(i);
        }

        addInputPanel(androidKeyStorePassword);
        addInputPanel(androidKeyAliasPassword);

        ((TextField) androidKeyStorePassword.getField()).setPasswordMode(true);
        ((TextField) androidKeyStorePassword.getField()).setPasswordCharacter('*');
        ((TextField) androidKeyAliasPassword.getField()).setPasswordMode(true);
        ((TextField) androidKeyAliasPassword.getField()).setPasswordCharacter('*');

        dir.setMandatory(true);

        arch.setText(Ctx.project.getEditorConfig().getProperty(ARCH_PROP, ARCHS[0]));
        dir.setText(Ctx.project.getEditorConfig().getProperty(DIR_PROP, ""));

        for (InputPanel i : options) {
            String prop = Ctx.project.getEditorConfig().getProperty("package." + i.getTitle());

            if (prop != null && !prop.isEmpty())
                i.setText(prop);
        }

        version.setText(getCurrentVersion());

        // TODO Set version code based in version
        // androidVersionCode.setText(genVersionCode(version.getText()));

        setInfo(INFO);

        arch.getField().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                archChanged();
            }
        });

        desktopType.getField().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                desktopTypeChanged();
            }
        });

        os.getField().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                osChanged();
            }
        });

        androidType.getField().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                androidTypeChanged();
            }
        });

        // Add the 'create' button to the keystore.
        TextButton createButton = new TextButton("Create", skin, "no-toggled");

        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final CreateAndroidKeystoreDialog c = new CreateAndroidKeystoreDialog(skin);
                c.show(getStage());

                c.setListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        androidKeyStore.setText(c.getKeyStorePath());
                        androidKeyAlias.setText(c.getKeyAlias());
                        androidKeyStorePassword.setText(c.getKeyStorePassword());
                        androidKeyAliasPassword.setText(c.getKeyAliasPassword());

                    }
                });

            }
        });

        Table t = new Table();
        Actor a = androidKeyStore.getField();
        Cell<?> c = androidKeyStore.getCell(a);
        t.add(a);
        t.add(createButton);
        c.setActor(t);

        // Add the 'download' button to the OS combo.
        TextButton downloadButton = new TextButton("Download JRE", skin, "no-toggled");

        downloadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                if ("windows64".equals(os.getText())) {
                    downloadJDK(
                            "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.7_7.zip",
                            "jdk17_windows64.tar.gz");
                } else if ("linux64".equals(os.getText())) {
                    downloadJDK("https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.7_7.tar.gz",
                            "jdk17_linux64.tar.gz");
                } else if ("macOS-x86".equals(os.getText())) {
                    downloadJDK("https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.7_7.tar.gz",
                            "jdk17_macos64_x86.tar.gz");
                }
            }
        });

        Table t2 = new Table();
        Actor a2 = os.getField();
        Cell<?> c2 = os.getCell(a2);
        t2.add(a2);
        t2.add(downloadButton);
        c2.setActor(t2);

        archChanged();
    }

    private void downloadJDK(String url, String fileName) {
        FileChooser fileChooser = new FileChooser(FileChooser.Mode.OPEN);
        fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
        fileChooser.setViewMode(FileChooser.ViewMode.LIST);

        fileChooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
        getStage().addActor(fileChooser);

        fileChooser.setListener(new FileChooserListener() {

            @Override
            public void selected(Array<FileHandle> files) {
                try {
                    File outputFile = new File(files.get(0).file(), fileName);
                    HttpUtils.downloadAsync(new URL(url), new FileOutputStream(outputFile), new HttpUtils.Callback() {
                        @Override
                        public void updated(int length, int totalLength) {
                            final int progress = ((int) (((double) length / (double) totalLength) * 100));
                            Message.showMsg(getStage(), "Downloading JDK... " + progress + "%", true);
                        }

                        @Override
                        public void completed() {
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    Message.hideMsg();

                                    if ("windows64".equals(os.getText())) {
                                        winJRE64.setText(outputFile.getAbsolutePath());
                                    } else if ("linux64".equals(os.getText())) {
                                        linux64JRE.setText(outputFile.getAbsolutePath());
                                    } else if ("macOS-x86".equals(os.getText())) {
                                        osxJRE.setText(outputFile.getAbsolutePath());
                                    }
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

    @Override
    protected void ok() {

        final Stage stg = getStage();

        Message.showMsg(stg, "Generating package...", true);

        new Thread() {

            @Override
            public void run() {

                String msg;

                if (Ctx.project.getSelectedScene() == null) {
                    msg = "There are no scenes in this chapter.";
                    Message.showMsg(stg, msg, 3);
                    return;
                }

                Ctx.project.getProjectConfig().removeProperty(Config.CHAPTER_PROP);
                Ctx.project.getProjectConfig().removeProperty(Config.TEST_SCENE_PROP);
                setCurrentVersion(version.getText());

                try {
                    Ctx.project.saveProject();
                } catch (Exception ex) {
                    msg = "Something went wrong while saving the project.\n\n" + ex.getClass().getSimpleName() + " - "
                            + ex.getMessage();

                    Message.showMsgDialog(stg, "Error", msg);
                    return;
                }

                try {
                    msg = packageAdv();
                } catch (Exception e) {
                    msg = "Error Generating package\n\n" + e.getMessage();
                    e.printStackTrace();
                }

                Ctx.project.getEditorConfig().setProperty(ARCH_PROP, arch.getText());
                Ctx.project.getEditorConfig().setProperty(DIR_PROP, dir.getText());

                for (InputPanel i : options) {
                    if (i.getText() != null)
                        Ctx.project.getEditorConfig().setProperty("package." + i.getTitle(), i.getText());
                }

                // hide message
                Message.hideMsg();

                final String m = msg;
                Message.showMsgDialog(stg, "Result", m);
            }
        }.start();

    }

    private String packageAdv() throws IOException, CompressorException, ArchiveException {
        String msg = "Package generated SUCCESSFULLY";

        String projectName = getAppName();
        if (projectName == null)
            Ctx.project.getProjectDir().getName();

        String versionParam = "-Pversion=" + version.getText() + " ";
        Ctx.project.getProjectConfig().setProperty(Config.VERSION_PROP, version.getText());

        String customBuildParams = customBuildParameters.getText() == null ? "" : customBuildParameters.getText() + " ";

        if (arch.getText().equals("desktop")) {
            String error = createDesktop(projectName, versionParam, customBuildParams);
            return error == null ? msg : error;
        }

        if (arch.getText().equals("android")) {
            String error = createAndroid(projectName, versionParam, customBuildParams);
            return error == null ? msg : error;
        }

        if (arch.getText().equals("ios")) {
            String error = createIOS(projectName, customBuildParams);
            return error == null ? msg : error;
        }

        return msg;
    }

    private String createIOS(String projectName, String customBuildParams) throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            return "You need a MacOSX computer with XCode installed to generate the IOS package.";
        }

        // UPDATE 'robovm.properties'
        Properties p = new Properties();
        p.load(new FileReader(Ctx.project.getProjectDir().getAbsolutePath() + "/ios/robovm.properties"));
        p.setProperty("app.version", version.getText());
        p.setProperty("app.build", versionCode.getText());
        p.setProperty("app.name", Ctx.project.getTitle());
        p.store(new FileOutputStream(new File(Ctx.project.getProjectDir().getAbsolutePath(), "/ios/robovm.properties")),
                null);

        List<String> params = new ArrayList<>();

        if (iosSignIdentity.getText() != null)
            params.add("-Probovm.iosSignIdentity=" + iosSignIdentity.getText());

        if (iosProvisioningProfile.getText() != null)
            params.add("-Probovm.iosProvisioningProfile=" + iosProvisioningProfile.getText());

        if (customBuildParameters.getText() != null)
            params.add(customBuildParams);

        // Add clean target in IOS because the app. is not signing well if not cleaning.
        params.add("ios:clean");

        params.add("ios:createIPA");

        if (RunProccess.runGradle(Ctx.project.getProjectDir(), params)) {

            String apk = Ctx.project.getProjectDir().getAbsolutePath() + "/ios/build/robovm/IOSLauncher.ipa";

            File f = new File(apk);
            FileUtils.copyFile(f, new File(dir.getText(), projectName + "-" + version.getText() + ".ipa"));
        } else {
            return "Error Generating package";
        }

        return null;
    }

    private String createAndroid(String projectName, String versionParam, String customBuildParams)
            throws IOException {
        String params = versionParam + customBuildParams + "-PversionCode=" + versionCode.getText() + " "
                + "-Pkeystore=\"" + androidKeyStore.getText() + "\" " + "-PstorePassword="
                + androidKeyStorePassword.getText() + " " + "-Palias=" + androidKeyAlias.getText() + " "
                + "-PkeyPassword=" + androidKeyAliasPassword.getText() + " ";

        // UPDATE 'local.properties' with the android SDK location.
        if (androidSDK.getText() != null && !androidSDK.getText().trim().isEmpty()) {
            String sdk = androidSDK.getText();

            Properties p = new Properties();
            p.setProperty("sdk.dir", sdk);
            p.store(new FileOutputStream(new File(Ctx.project.getProjectDir().getAbsolutePath(), "local.properties")),
                    null);
        }

        if (!new File(Ctx.project.getProjectDir().getAbsolutePath(), "local.properties").exists()
                && System.getenv("ANDROID_HOME") == null) {
            return "You have to specify the Android SDK path or set the ANDROID_HOME environtment variable.";
        }

        boolean isAPK = androidType.getText().equals(ANDROID_TYPES[0]);

        String task = "android:assembleFullRelease";
        File pkgFile = new File(Ctx.project.getProjectDir().getAbsolutePath(),
                "android/build/outputs/apk/full/release/android-full-release.apk");

        File destPkgFile = new File(dir.getText(), projectName + "-" + version.getText() + ".apk");

        boolean genExpansion = Boolean.parseBoolean(expansionFile.getText());

        if (!isAPK) { // .aab
            task = "android:bundleFullRelease";
            pkgFile = new File(Ctx.project.getProjectDir().getAbsolutePath(),
                    "android/build/outputs/bundle/fullRelease/android-full-release.aab");
            destPkgFile = new File(dir.getText(), projectName + "-" + version.getText() + ".aab");
            genExpansion = false;
        }

        boolean newProjectStructure = new File(Ctx.project.getProjectDir().getAbsolutePath() + "/assets/").exists();

        if (!newProjectStructure && genExpansion)
            return "You need to update your project to the new layout to generate expansion files.";

        if (!newProjectStructure) {
            task = "android:assembleRelease";
            pkgFile = new File(Ctx.project.getProjectDir().getAbsolutePath(),
                    "android/build/outputs/apk/android-release.apk");
        }

        if (genExpansion) {
            task = "android:assembleExpansionRelease";
            pkgFile = new File(Ctx.project.getProjectDir().getAbsolutePath(),
                    "android/build/outputs/apk/expansion/release/android-expansion-release.apk");
        }

        if (!RunProccess.runGradle(Ctx.project.getProjectDir(), params + task)) {
            return "Error Generating package";
        }

        FileUtils.copyFile(pkgFile, destPkgFile);

        if (genExpansion) {
            File fExp = findObb(Ctx.project.getProjectDir().getAbsolutePath() + "/android/build/distributions/",
                    versionCode.getText());
            FileUtils.copyFile(fExp, new File(dir.getText(), fExp.getName()));
        }

        return null;
    }

    private String createDesktop(String projectName, String versionParam, String customBuildParams)
            throws IOException, CompressorException, ArchiveException {
        String jarDir = Ctx.project.getProjectDir().getAbsolutePath() + "/desktop/build/libs/";
        String jarName = "desktop-" + version.getText() + ".jar";

        String error = genDesktopJar(projectName, versionParam, jarDir, jarName, customBuildParams);

        if (error != null)
            return error;

        if (desktopType.getText().equals(DESKTOP_TYPES[0])) { // BUNDLE JRE
            String launcher = getDesktopMainClass();

            if (os.getText().equals("linux64")) {
                packr(Platform.Linux64, linux64JRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
            } else if (os.getText().equals("windows64")) {
                packr(Platform.Windows64, winJRE64.getText(), projectName, jarDir + jarName, launcher, dir.getText());
            } else if (os.getText().equals("macOS-x86")) {
                packr(Platform.MacOS, osxJRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
            }
        }

        return null;
    }

    private void archChanged() {
        for (InputPanel ip : options) {
            setVisible(ip, false);
        }

        setVisible(androidKeyStorePassword, false);
        setVisible(androidKeyAliasPassword, false);

        setVisible(version, true);

        String a = arch.getText();
        if (a.equals("desktop")) {
            setVisible(desktopType, true);
            desktopTypeChanged();
        } else if (a.equals("android")) {
            setVisible(androidType, true);
            setVisible(versionCode, true);
            setVisible(androidSDK, true);
            setVisible(expansionFile, true);
            setVisible(androidKeyStore, true);
            setVisible(androidKeyAlias, true);
            setVisible(androidKeyStorePassword, true);
            setVisible(androidKeyAliasPassword, true);
        } else if (a.equals("ios")) {
            setVisible(versionCode, true);
            setVisible(iosSignIdentity, true);
            setVisible(iosProvisioningProfile, true);
        }

        setVisible(customBuildParameters, true);
    }

    private void desktopTypeChanged() {
        if (desktopType.getText().equals(DESKTOP_TYPES[0])) {
            setVisible(os, true);
        } else {
            setVisible(os, false);
            setVisible(icon, false);
        }

        osChanged();
    }

    private void androidTypeChanged() {
        setVisible(expansionFile, androidType.getText().equals(ANDROID_TYPES[0]));
    }

    private void osChanged() {
        setVisible(icon, false);

        if (os.isVisible() && (os.getText().equals("windows64"))) {
            setVisible(winJRE64, true);
        } else {
            setVisible(icon, false);
            setVisible(winJRE64, false);
        }

        setVisible(linux64JRE, os.isVisible() && (os.getText().equals("linux64")));

        if (os.isVisible() && (os.getText().equals("macOS-x86"))) {
            setVisible(osxJRE, true);
            setVisible(icon, true);
        } else {
            setVisible(osxJRE, false);
        }
    }

    @Override
    protected boolean validateFields() {
        boolean ok = dir.validateField();

        for (InputPanel i : options) {
            if (i.isVisible() && !i.validateField())
                ok = false;
        }

        if (androidKeyStorePassword.isVisible() && !androidKeyStorePassword.validateField())
            ok = false;

        if (androidKeyAliasPassword.isVisible() && !androidKeyAliasPassword.validateField())
            ok = false;

        if (linux64JRE.isVisible()
                && (!new File(linux64JRE.getText()).exists() || (!linux64JRE.getText().toLowerCase()
                .endsWith(".zip") && !linux64JRE.getText().toLowerCase().endsWith(".tar.gz")))) {
            linux64JRE.setError(true);
            ok = false;
        }

        if (winJRE64.isVisible()
                && (!new File(winJRE64.getText()).exists() || (!winJRE64.getText().toLowerCase()
                .endsWith(".zip") && !winJRE64.getText().toLowerCase().endsWith(".tar.gz")))) {
            winJRE64.setError(true);
            ok = false;
        }

        if (osxJRE.isVisible()
                && (!new File(osxJRE.getText()).exists() || (!osxJRE.getText().toLowerCase()
                .endsWith(".zip") && !osxJRE.getText().toLowerCase().endsWith(".tar.gz")))) {
            osxJRE.setError(true);
            ok = false;
        }

        return ok;
    }

    private String genDesktopJar(String projectName, String versionParam, String jarDir, String jarName,
                                 String customBuildParams) throws IOException {
        String msg = null;

        if (RunProccess.runGradle(Ctx.project.getProjectDir(), versionParam + customBuildParams + "desktop:dist")) {
            File f = new File(jarDir + jarName);
            FileUtils.copyFileToDirectory(f, new File(dir.getText()));

            new File(jarDir, jarName).setExecutable(true);
            new File(dir.getText(), jarName).setExecutable(true);
        } else {
            msg = "Error Generating package";
        }

        return msg;
    }

    private void packr(Platform platform, String jdk, String exe, String jar, String mainClass, String outDir)
            throws IOException, CompressorException, ArchiveException {
        String suffix = null;

        switch (platform) {
            case Linux64:
                suffix = "lin64";
                break;
            case MacOS:
                suffix = "mac.app";
                break;
            case Windows64:
                suffix = "win64";
                break;

        }

        PackrConfig config = new PackrConfig();
        config.platform = platform;
        config.jdk = jdk;
        config.jrePath = "jre";
        config.executable = exe;
        config.classpath = Collections.singletonList(jar);
        config.mainClass = mainClass.replace('/', '.');
        config.vmArgs = Arrays.asList("-Xmx1G", "-Dsun.java2d.dpiaware=true");
        config.minimizeJre = "hard";

        config.outDir = new File(outDir + "/" + exe + "-" + suffix);

        new Packr().pack(config);

        // COPY MAC OS ICON
        if (platform == Platform.MacOS && icon.getText() != null && icon.getText().endsWith(".icns")) {
            FileUtils.copyFile(new File(icon.getText()),
                    new File(config.outDir.getAbsolutePath() + "/Contents/Resources/icons.icns"));
        }
    }

    /**
     * @return The appName from the file gradle.properties from the game
     */
    private String getAppName() {

        try {
            OrderedProperties prop = Ctx.project.getGradleProperties(Ctx.project.getProjectDir());
            return prop.getProperty("appName");
        } catch (IOException e) {
            Message.showMsg(getStage(), "Error reading file 'gradle.properties' from the game.", 3);
        }

        return null;
    }

    /**
     * @return The version from the file gradle.properties from the game
     */
    private String getCurrentVersion() {

        try {
            OrderedProperties prop = Ctx.project.getGradleProperties(Ctx.project.getProjectDir());
            return prop.getProperty("version");
        } catch (IOException e) {
            Message.showMsg(getStage(), "Error reading file 'gradle.properties' from the game.", 3);
        }

        return null;
    }

    /**
     * Saves the selected version
     */
    private void setCurrentVersion(String version) {

        try {
            OrderedProperties prop = Ctx.project.getGradleProperties(Ctx.project.getProjectDir());
            prop.setProperty("version", version);
            Ctx.project.saveGradleProperties(prop, Ctx.project.getProjectDir());
        } catch (IOException e) {
            Message.showMsg(getStage(), "Error reading file 'gradle.properties' from the game.", 3);
        }
    }

    /**
     * @return Search the desktop main class in the desktop folder
     */
    private String getDesktopMainClass() {

        File result = search(new File(Ctx.project.getProjectDir().getAbsolutePath() + "/desktop"));

        String absolutePath = result.getAbsolutePath().replace('\\', '/');

        int cutIdx = absolutePath.indexOf("src/main/java/");

        if (cutIdx != -1)
            cutIdx += 14;
        else
            cutIdx = absolutePath.indexOf("src/") + 4;

        return absolutePath.substring(cutIdx, absolutePath.length() - ".java".length());
    }

    private File search(File file) {
        File result = null;

        // do you have permission to read this directory?
        if (file.canRead()) {
            for (File temp : file.listFiles()) {
                if (temp.isDirectory()) {
                    result = search(temp);

                    if (result != null)
                        return result;
                } else {
                    if (temp.getName().equals(DESKTOP_LAUNCHER)) {
                        return temp;
                    }
                }
            }

        }

        return result;
    }

    private File findObb(String baseDir, final String versionCode) {
        File dir = new File(baseDir);

        File[] listFiles = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String arg) {
                return arg.startsWith("main." + versionCode + ".") && arg.endsWith(".obb");
            }

        });

        return listFiles == null || listFiles.length == 0 ? null : listFiles[0];
    }
}
