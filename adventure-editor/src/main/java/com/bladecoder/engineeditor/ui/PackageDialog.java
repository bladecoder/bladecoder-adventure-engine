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
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.Packr.Platform;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.EditDialog;
import com.bladecoder.engineeditor.ui.components.FileInputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.utils.RunProccess;

public class PackageDialog extends EditDialog {
	private static final String ARCH_PROP = "package.arch";
	private static final String DIR_PROP = "package.dir";
	
	private static final String DESKTOP_LAUNCHER = "org/bladecoder/engine/desktop/DesktopLauncher";

	private static final String INFO = "Package the Adventure for distribution";
	private static final String[] ARCHS = { "desktop", "android", "ios", "html" };
	private static final String[] TYPES = { "Bundle JRE", "Runnable jar" };
	private static final String[] OSS = { "all", "windows", "linux64", "linux32", "macOSX" };

	private InputPanel arch;
	private InputPanel dir;
	private InputPanel type;
	private InputPanel os;
	private InputPanel linux64JRE;
	private InputPanel linux32JRE;
	private InputPanel winJRE;
	private InputPanel osxJRE;
	private InputPanel version;
	private InputPanel icon;
	private InputPanel androidSDK;
	private InputPanel androidKeyStore;
	private InputPanel androidKeyAlias;
	private InputPanel androidKeyStorePassword;
	private InputPanel androidKeyAliasPassword;

	private InputPanel[] options = new InputPanel[11];

	@SuppressWarnings("unchecked")
	public PackageDialog(Skin skin) {
		super("PACKAGE ADVENTURE", skin);

		arch = InputPanelFactory.createInputPanel(skin, "Architecture", "Select the target Architecture for the game", ARCHS, true);
		dir = new FileInputPanel(skin, "Output Directory", "Select the output directory to put the package", FileInputPanel.DialogType.DIRECTORY);
		type = InputPanelFactory.createInputPanel(skin, "Type", "Select the type of the package", TYPES, true);
		os = InputPanelFactory.createInputPanel(skin, "OS", "Select the OS of the package", OSS, true);
		linux64JRE = new FileInputPanel(skin, "JRE.Linux64", "Select the 64 bits Linux JRE Location to bundle. Must be a ZIP file", FileInputPanel.DialogType.OPEN_FILE);
		linux32JRE = new FileInputPanel(skin, "JRE.Linux32", "Select the 32 bits Linux JRE Location to bundle. Must be a ZIP file", FileInputPanel.DialogType.OPEN_FILE);
		winJRE = new FileInputPanel(skin, "JRE.Windows", "Select the Windows JRE Location to bundle. Must be a ZIP file", FileInputPanel.DialogType.OPEN_FILE);
		osxJRE = new FileInputPanel(skin, "JRE.OSX", "Select the OSX JRE Location to bundle. Must be a ZIP file", FileInputPanel.DialogType.OPEN_FILE);
		version = InputPanelFactory.createInputPanel(skin, "Version", "Select the version of the package");
		icon = new FileInputPanel(skin, "Icon", "The icon for the .exe file", FileInputPanel.DialogType.OPEN_FILE);
		androidSDK = new FileInputPanel(skin, "SDK", "Select the Android SDK Location", FileInputPanel.DialogType.DIRECTORY);
		androidKeyStore = new FileInputPanel(skin, "KeyStore", "Select the Key Store Location", FileInputPanel.DialogType.OPEN_FILE);
		androidKeyAlias = InputPanelFactory.createInputPanel(skin, "KeyAlias", "Select the Key Alias Location");
		androidKeyStorePassword = InputPanelFactory.createInputPanel(skin, "KeyStorePasswd", "Key Store Password", false);
		androidKeyAliasPassword = InputPanelFactory.createInputPanel(skin, "KeyAliasPasswd", "Key Alias Password", false);

		options[0] = type;
		options[1] = os;
		options[2] = linux64JRE;
		options[3] = linux32JRE;
		options[4] = winJRE;
		options[5] = osxJRE;
		options[6] = version;
		options[7] = icon;
		options[8] = androidSDK;
		options[9] = androidKeyStore;
		options[10] = androidKeyAlias;

		addInputPanel(arch);
		addInputPanel(dir);

		for (InputPanel i : options) {
			addInputPanel(i);
			i.setMandatory(true);
		}

		addInputPanel(androidKeyStorePassword);
		addInputPanel(androidKeyAliasPassword);

		dir.setMandatory(true);

		arch.setText(Ctx.project.getEditorConfig().getProperty(ARCH_PROP, ARCHS[0]));
		dir.setText(Ctx.project.getEditorConfig().getProperty(DIR_PROP, ""));

		for (InputPanel i : options) {
			String prop = Ctx.project.getEditorConfig().getProperty("package." + i.getTitle());

			if (prop != null && !prop.isEmpty())
				i.setText(prop);
		}


		setInfo(INFO);

		((SelectBox<String>) (arch.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				archChanged();
			}
		});

		((SelectBox<String>) (type.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				typeChanged();
			}
		});

		((SelectBox<String>) (os.getField())).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				osChanged();
			}
		});

		archChanged();
	}

	@Override
	protected void ok() {
		
		new Thread(new Runnable() {
			Stage stage = getStage();
			
			@Override
			public void run() {
				Ctx.msg.show(stage, "Generating package...",true);
				String msg;
				
				try {
					msg = packageAdv();
				} catch (Exception e) {
					msg = "Error Generating package\n\n" + e.getMessage();
				}

				Ctx.project.getEditorConfig().setProperty(ARCH_PROP, arch.getText());
				Ctx.project.getEditorConfig().setProperty(DIR_PROP, dir.getText());

				for (InputPanel i : options) {
					Ctx.project.getEditorConfig().setProperty("package." + i.getTitle(), i.getText());
				}
				
				Ctx.msg.hide();
				
				if(msg != null)
					Ctx.msg.show(stage, msg, 3);
			}
		}).start();			

	}

	private String packageAdv() throws IOException {
		String msg = "Package generated SUCCESSFULLY";
		
		String projectName = Ctx.project.getProjectDir().getName();
		String versionParam = "-Ppassed_version=" + version.getText() + " ";
		
		if (arch.getText().equals("desktop")) {			
			String jar = Ctx.project.getProjectDir().getAbsolutePath() +
					"/desktop/build/libs/" + projectName + "-desktop-" + version.getText() + ".jar";
			
			msg = genDesktopJar(projectName, versionParam, jar);

			if (type.getText().equals(TYPES[0])) { // BUNDLE JRE
				if (os.getText().equals("linux64")) {					
					packr(Platform.linux64, linux64JRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
				} else if (os.getText().equals("linux32")) {
					packr(Platform.linux32, linux32JRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
				} else if (os.getText().equals("windows")) {
					packr(Platform.windows, winJRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
				} else if (os.getText().equals("macOSX")) {
					packr(Platform.mac, osxJRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
				} else if (os.getText().equals("all")) {
					packr(Platform.linux64, linux64JRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
					packr(Platform.linux32, linux32JRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
					packr(Platform.windows, winJRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
					packr(Platform.mac, osxJRE.getText(), projectName, jar, DESKTOP_LAUNCHER, dir.getText());
				}
			}
		} else if (arch.getText().equals("android")) {			
			String params = versionParam + 
					"-Pkeystore=" +  androidKeyStore.getText() + " " +
					"-PstorePassword=" + androidKeyStorePassword.getText() + " " +
					"-Palias=" + androidKeyAlias.getText() + " " +
					"-PkeyPassword=" + androidKeyAliasPassword.getText() + " ";
			
			if(RunProccess.runGradle(Ctx.project.getProjectDir(), params + "android:assembleRelease")) {
				String apk = Ctx.project.getProjectDir().getAbsolutePath() +
						"/android/build/outputs/apk/android-release.apk";
				File f = new File(apk);
//				FileUtils.copyFileToDirectory(f, new File(dir.getText()));
				FileUtils.copyFile(f, new File(dir.getText(),projectName + "-" + version.getText() + ".apk"));
			} else {
				msg = "Error Generating package" ;
			}
		} else if (arch.getText().equals("ios")) {
			if(RunProccess.runGradle(Ctx.project.getProjectDir(), "ios:createIPA")) {
				FileUtils.copyDirectory(new File(Ctx.project.getProjectDir().getAbsolutePath() +
							"/ios/build/robovm/")
							, new File(dir.getText()));
			} else {
				msg = "Error Generating package" ;
			}			
		} else if (arch.getText().equals("html")) {
			if(RunProccess.runGradle(Ctx.project.getProjectDir(), "html:dist")) {
				FileUtils.copyDirectory(new File(Ctx.project.getProjectDir().getAbsolutePath() +
							"/html/build/dist")
							, new File(dir.getText()));
			} else {
				msg = "Error Generating package" ;
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
			setVisible(androidSDK, true);
			setVisible(androidKeyStore, true);
			setVisible(androidKeyAlias, true);
			setVisible(androidKeyStorePassword, true);
			setVisible(androidKeyAliasPassword, true);
		}
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
		if (os.isVisible() && (os.getText().equals("windows") || os.getText().equals("all"))) {
			setVisible(icon, true);
			setVisible(winJRE, true);
		} else {
			setVisible(icon, false);
			setVisible(winJRE, false);
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

//		if (icon.isVisible() && !icon.getText().endsWith(".ico")) {
//			icon.setError(true);
//			ok = false;
//		}

		if (linux32JRE.isVisible() && (!new File(linux32JRE.getText()).exists() || !linux32JRE.getText().toLowerCase().endsWith(".zip"))) {
			linux32JRE.setError(true);
			ok = false;
		}

		if (linux64JRE.isVisible() && (!new File(linux64JRE.getText()).exists() || !linux64JRE.getText().toLowerCase().endsWith(".zip"))){
			linux64JRE.setError(true);
			ok = false;
		}

		if (winJRE.isVisible() && (!new File(winJRE.getText()).exists() || !winJRE.getText().toLowerCase().endsWith(".zip"))){
			winJRE.setError(true);
			ok = false;
		}
		
		if (osxJRE.isVisible() && (!new File(osxJRE.getText()).exists() || !osxJRE.getText().toLowerCase().endsWith(".zip"))){
			osxJRE.setError(true);
			ok = false;
		}

		return ok;
	}
	
	private String genDesktopJar(String projectName, String versionParam, String jar) throws IOException {
		String msg = null;
		
		if(RunProccess.runGradle(Ctx.project.getProjectDir(), versionParam + "desktop:dist")) {
			File f = new File(jar);
			FileUtils.copyFileToDirectory(f, new File(dir.getText()));					
			new File(dir.getText() + "/" + projectName + "-desktop-" + version.getText() + ".jar").setExecutable(true);
		} else {
			msg = "Error Generating package" ;
		}
		
		return msg;
	}
	
	private void packr(Platform platform, String jdk, String exe, String jar, String mainClass, String outDir) throws IOException {
		String suffix = null;;
		
		switch(platform) {
		case linux32:
			suffix = "lin32";
			break;
		case linux64:
			suffix = "lin64";
			break;
		case mac:
			suffix = "mac";
			break;
		case windows:
			suffix = "win";
			break;
		
		}
		
		Packr.Config config = new Packr.Config();
		config.platform = platform;
		config.jdk = jdk;
		config.executable = exe;
		config.jar = jar;
		config.mainClass = mainClass;
		config.vmArgs = Arrays.asList("-Xmx1G");
		config.minimizeJre = new String[] { "jre/lib/rt/com/sun/corba", "jre/lib/rt/com/sun/jndi" };
		config.outDir = outDir + "/" + exe + "-" + suffix;

		new Packr().pack(config);
	}
}
