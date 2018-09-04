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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;
import com.badlogicgames.packr.PackrConfig.Platform;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.OrderedProperties;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.FileInputPanel;
import com.bladecoder.engineeditor.ui.panels.FilteredSelectBox;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;

public class PackageDialog extends EditDialog {
	private static final String ARCH_PROP = "package.arch";
	private static final String DIR_PROP = "package.dir";

	private static final String DESKTOP_LAUNCHER = "DesktopLauncher.java";

	private static final String INFO = "Package the Adventure for distribution";
	private static final String[] ARCHS = { "desktop", "android", "ios" };
	private static final String[] TYPES = { "Bundle JRE", "Runnable jar" };
	private static final String[] OSS = { "all", "windows32", "windows64", "linux64", "linux32", "macOSX" };

	private InputPanel arch;
	private InputPanel dir;
	private InputPanel type;
	private InputPanel os;
	private FileInputPanel linux64JRE;
	private FileInputPanel linux32JRE;
	private FileInputPanel winJRE;
	private FileInputPanel winJRE64;
	private FileInputPanel osxJRE;
	private InputPanel version;
	private InputPanel icon;
	private InputPanel versionCode;
	private InputPanel androidSDK;
	private InputPanel expansionFile;
	private InputPanel androidKeyStore;
	private InputPanel androidKeyAlias;
	private InputPanel androidKeyStorePassword;
	private InputPanel androidKeyAliasPassword;

	private InputPanel iosSignIdentity;
	private InputPanel iosProvisioningProfile;
	
	private InputPanel customBuildParameters;

	private InputPanel[] options;

	@SuppressWarnings("unchecked")
	public PackageDialog(Skin skin) {
		super("PACKAGE ADVENTURE", skin);

		arch = InputPanelFactory.createInputPanel(skin, "Architecture", "Select the target Architecture for the game",
				ARCHS, true);
		dir = new FileInputPanel(skin, "Output Directory", "Select the output directory to put the package",
				FileInputPanel.DialogType.DIRECTORY);
		type = InputPanelFactory.createInputPanel(skin, "Type", "Select the package type", TYPES, true);
		os = InputPanelFactory.createInputPanel(skin, "OS", "Select the OS of the package", OSS, true);

		FileTypeFilter typeFilter = new FileTypeFilter(true);
		typeFilter.addRule("Zip files (*.zip)", "zip");

		linux64JRE = new FileInputPanel(skin, "JRE.Linux64",
				"Select the 64 bits Linux JRE Location to bundle. Must be a ZIP file",
				FileInputPanel.DialogType.OPEN_FILE);
		linux64JRE.setFileTypeFilter(typeFilter);

		linux32JRE = new FileInputPanel(skin, "JRE.Linux32",
				"Select the 32 bits Linux JRE Location to bundle. Must be a ZIP file",
				FileInputPanel.DialogType.OPEN_FILE);
		linux32JRE.setFileTypeFilter(typeFilter);

		winJRE = new FileInputPanel(skin, "JRE.Windows32",
				"Select the Windows 32 bits JRE Location to bundle. Must be a ZIP file",
				FileInputPanel.DialogType.OPEN_FILE);
		winJRE.setFileTypeFilter(typeFilter);

		winJRE64 = new FileInputPanel(skin, "JRE.Windows64",
				"Select the Windows 64 bits JRE Location to bundle. Must be a ZIP file",
				FileInputPanel.DialogType.OPEN_FILE);
		winJRE64.setFileTypeFilter(typeFilter);

		osxJRE = new FileInputPanel(skin, "JRE.MACOS", "Select the MacOS JRE Location to bundle. Must be a ZIP file",
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
				"If your assets exceeds 100mb, you have to create an expansion file to upload the game to the Play Store.",
				Param.Type.BOOLEAN, true, "false");
		androidKeyStore = new FileInputPanel(skin, "KeyStore", "Select the Key Store Location",
				FileInputPanel.DialogType.OPEN_FILE);
		androidKeyAlias = InputPanelFactory.createInputPanel(skin, "KeyAlias", "Select the Key Alias Location", true);

		androidKeyStorePassword = InputPanelFactory.createInputPanel(skin, "KeyStorePasswd", "Key Store Password",
				true);
		androidKeyAliasPassword = InputPanelFactory.createInputPanel(skin, "KeyAliasPasswd", "Key Alias Password",
				true);

		iosSignIdentity = InputPanelFactory.createInputPanel(skin, "Sign Identity",
				"Empty for auto select.\nThis field matches against the start of the certificate name. Alternatively you can use a certificate fingerprint.\nIf the value is enclosed in / a regexp search will be done against the certificate name instead.\nRun the command 'security find-identity -v -p codesigning' or use theKeyChain Access OS X app to view your installed certificates.",
				false);

		iosProvisioningProfile = InputPanelFactory.createInputPanel(skin, "Provisioning Profile",
				"Empty for auto select.", false);
		
		customBuildParameters = InputPanelFactory.createInputPanel(skin, "Custom build parameters",
				"You can add extra build parameters for customized build scripts.", false);

		options = new InputPanel[] { type, os, linux64JRE, linux32JRE, winJRE, winJRE64, osxJRE, version, icon,
				versionCode, androidSDK, expansionFile, androidKeyStore, androidKeyAlias, iosSignIdentity,
				iosProvisioningProfile, customBuildParameters };

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

		((FilteredSelectBox<String>) (arch.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				archChanged();
			}
		});

		((FilteredSelectBox<String>) (type.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				typeChanged();
			}
		});

		((FilteredSelectBox<String>) (os.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				osChanged();
			}
		});

		archChanged();
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

				if (msg != null) {
					final String m = msg;
					Message.showMsgDialog(stg, "Result", m);
				}				
			}
		}.start();

	}

	private String packageAdv() throws IOException {
		String msg = "Package generated SUCCESSFULLY";

		String projectName = getAppName();
		if (projectName == null)
			Ctx.project.getProjectDir().getName();

		String versionParam = "-Pversion=" + version.getText() + " ";
		Ctx.project.getProjectConfig().setProperty(Config.VERSION_PROP, version.getText());
		
		String customBuildParams = customBuildParameters.getText() == null? "": customBuildParameters.getText() + " ";

		if (arch.getText().equals("desktop")) {
			String jarDir = Ctx.project.getProjectDir().getAbsolutePath() + "/desktop/build/libs/";
			String jarName = projectName + "-desktop-" + version.getText() + ".jar";

			String error = genDesktopJar(projectName, versionParam, jarDir, jarName, customBuildParams);

			if (error != null)
				msg = error;

			if (type.getText().equals(TYPES[0])) { // BUNDLE JRE
				String launcher = getDesktopMainClass();

				if (os.getText().equals("linux64")) {
					packr(Platform.Linux64, linux64JRE.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
				} else if (os.getText().equals("linux32")) {
					packr(Platform.Linux32, linux32JRE.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
				} else if (os.getText().equals("windows32")) {
					packr(Platform.Windows32, winJRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
				} else if (os.getText().equals("windows64")) {
					packr(Platform.Windows64, winJRE64.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
				} else if (os.getText().equals("macOSX")) {
					packr(Platform.MacOS, osxJRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
				} else if (os.getText().equals("all")) {
					packr(Platform.Linux64, linux64JRE.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
					packr(Platform.Linux32, linux32JRE.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
					packr(Platform.Windows32, winJRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
					packr(Platform.Windows64, winJRE64.getText(), projectName, jarDir + jarName, launcher,
							dir.getText());
					packr(Platform.MacOS, osxJRE.getText(), projectName, jarDir + jarName, launcher, dir.getText());
				}
			}
		} else if (arch.getText().equals("android")) {
			String params = versionParam + customBuildParams + "-PversionCode=" + versionCode.getText() + " " + "-Pkeystore="
					+ androidKeyStore.getText() + " " + "-PstorePassword=" + androidKeyStorePassword.getText() + " "
					+ "-Palias=" + androidKeyAlias.getText() + " " + "-PkeyPassword="
					+ androidKeyAliasPassword.getText() + " ";

			// UPDATE 'local.properties' with the android SDK location.
			if (androidSDK.getText() != null && !androidSDK.getText().trim().isEmpty()) {
				String sdk = androidSDK.getText();

				Properties p = new Properties();
				p.setProperty("sdk.dir", sdk);
				p.store(new FileOutputStream(
						new File(Ctx.project.getProjectDir().getAbsolutePath(), "local.properties")), null);
			}

			String task = "android:assembleFullRelease";
			String apk = Ctx.project.getProjectDir().getAbsolutePath()
					+ "/android/build/outputs/apk/full/release/android-full-release.apk";

			boolean genExpansion = Boolean.parseBoolean(expansionFile.getText());
			boolean newProjectStructure = new File(Ctx.project.getProjectDir().getAbsolutePath()
					+ "/assets/").exists();
			
			if(!newProjectStructure && genExpansion)
				return "You need to update your project to the new layout to generate expansion files.";
			
			if(!newProjectStructure) {
				task = "android:assembleRelease";
				apk = Ctx.project.getProjectDir().getAbsolutePath()
						+ "/android/build/outputs/apk/android-release.apk";
			}

			if (genExpansion) {
				task = "android:assembleExpansionRelease";
				apk = Ctx.project.getProjectDir().getAbsolutePath()
						+ "/android/build/outputs/apk/expansion/release/android-expansion-release.apk";
			}

			if (RunProccess.runGradle(Ctx.project.getProjectDir(), params + task)) {
				File f = new File(apk);
				FileUtils.copyFile(f, new File(dir.getText(), projectName + "-" + version.getText() + ".apk"));

				if (genExpansion) {
					File fExp = findObb(Ctx.project.getProjectDir().getAbsolutePath() + "/android/build/distributions/",
							versionCode.getText());
					FileUtils.copyFile(fExp, new File(dir.getText(), fExp.getName()));
				}

			} else {
				msg = "Error Generating package";
			}
		} else if (arch.getText().equals("ios")) {

			if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
				return "You need a MacOSX computer with XCode installed to generate the IOS package.";
			}

			// UPDATE 'robovm.properties'
			Properties p = new Properties();
			p.load(new FileReader(Ctx.project.getProjectDir().getAbsolutePath() + "/ios/robovm.properties"));
			p.setProperty("app.version", version.getText());
			p.setProperty("app.build", versionCode.getText());
			p.setProperty("app.name", Ctx.project.getTitle());
			p.store(new FileOutputStream(
					new File(Ctx.project.getProjectDir().getAbsolutePath(), "/ios/robovm.properties")), null);

			List<String> params = new ArrayList<String>();

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
				msg = "Error Generating package";
			}
		}

		return msg;
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
			setVisible(type, true);
			typeChanged();
		} else if (a.equals("android")) {
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

	private void typeChanged() {
		if (type.getText().equals(TYPES[0])) {
			setVisible(os, true);
		} else {
			setVisible(os, false);
			setVisible(icon, false);
		}

		osChanged();
	}

	private void osChanged() {
		setVisible(icon, false);

		if (os.isVisible() && (os.getText().equals("windows32") || os.getText().equals("all"))) {
			setVisible(winJRE, true);
		} else {
			setVisible(icon, false);
			setVisible(winJRE, false);
		}

		if (os.isVisible() && (os.getText().equals("windows64") || os.getText().equals("all"))) {
			setVisible(winJRE64, true);
		} else {
			setVisible(icon, false);
			setVisible(winJRE64, false);
		}

		if (os.isVisible() && (os.getText().equals("linux32") || os.getText().equals("all"))) {
			setVisible(linux32JRE, true);
		} else {
			setVisible(linux32JRE, false);
		}

		if (os.isVisible() && (os.getText().equals("linux64") || os.getText().equals("all"))) {
			setVisible(linux64JRE, true);
		} else {
			setVisible(linux64JRE, false);
		}

		if (os.isVisible() && (os.getText().equals("macOSX") || os.getText().equals("all"))) {
			setVisible(osxJRE, true);
			setVisible(icon, true);
		} else {
			setVisible(osxJRE, false);
		}
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!dir.validateField())
			ok = false;

		for (InputPanel i : options) {
			if (i.isVisible() && !i.validateField())
				ok = false;
		}

		if (androidKeyStorePassword.isVisible() && !androidKeyStorePassword.validateField())
			ok = false;

		if (androidKeyAliasPassword.isVisible() && !androidKeyAliasPassword.validateField())
			ok = false;

		// if (icon.isVisible() && !icon.getText().endsWith(".ico")) {
		// icon.setError(true);
		// ok = false;
		// }

		if (linux32JRE.isVisible()
				&& (!new File(linux32JRE.getText()).exists() || !linux32JRE.getText().toLowerCase().endsWith(".zip"))) {
			linux32JRE.setError(true);
			ok = false;
		}

		if (linux64JRE.isVisible()
				&& (!new File(linux64JRE.getText()).exists() || !linux64JRE.getText().toLowerCase().endsWith(".zip"))) {
			linux64JRE.setError(true);
			ok = false;
		}

		if (winJRE.isVisible()
				&& (!new File(winJRE.getText()).exists() || !winJRE.getText().toLowerCase().endsWith(".zip"))) {
			winJRE.setError(true);
			ok = false;
		}

		if (winJRE64.isVisible()
				&& (!new File(winJRE64.getText()).exists() || !winJRE64.getText().toLowerCase().endsWith(".zip"))) {
			winJRE64.setError(true);
			ok = false;
		}

		if (osxJRE.isVisible()
				&& (!new File(osxJRE.getText()).exists() || !osxJRE.getText().toLowerCase().endsWith(".zip"))) {
			osxJRE.setError(true);
			ok = false;
		}

		return ok;
	}

	private String genDesktopJar(String projectName, String versionParam, String jarDir, String jarName, String customBuildParams)
			throws IOException {
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
			throws IOException {
		String suffix = null;

		switch (platform) {
		case Linux32:
			suffix = "lin32";
			break;
		case Linux64:
			suffix = "lin64";
			break;
		case MacOS:
			suffix = "mac.app";
			break;
		case Windows64:
			suffix = "win64";
			break;
		case Windows32:
			suffix = "win";
			break;

		}

		PackrConfig config = new PackrConfig();
		config.platform = platform;
		config.jdk = jdk;
		config.executable = exe;
		config.classpath = Arrays.asList(jar);
		config.mainClass = mainClass.replace('/', '.');
		config.vmArgs = Arrays.asList("-Xmx1G");
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

		int cutIdx =  absolutePath.indexOf("src/main/java/");
		
		if(cutIdx != -1)
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
				if (arg.startsWith("main." + versionCode + ".") && arg.endsWith(".obb")) {
					return true;
				}

				return false;
			}

		});

		return listFiles == null || listFiles.length == 0 ? null : listFiles[0];
	}
}
