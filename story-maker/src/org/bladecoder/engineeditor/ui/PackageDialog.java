package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.FileInputPanel;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.RunProccess;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class PackageDialog extends EditDialog {
	private static final String ARCH_PROP = "package.arch";
	private static final String DIR_PROP = "package.dir";
	
	private static final String INFO = "Package the Adventure for distribution";
	private static final String[] ARCHS = { "desktop", "android", "html", "ios" };
	private static final String[] TYPES = { "Bundle JRE", "Runnable jar" };
	private static final String[] OSS = { "all", "windows", "linux64", "linux32", "macOSX" };

	private InputPanel arch;
	private InputPanel dir;
	private InputPanel type;
	private InputPanel os;
	private InputPanel linux64JRE;
	private InputPanel linux32JRE;
	private InputPanel winJRE;
	private InputPanel version;
	private InputPanel icon;
	private InputPanel androidSDK;
	private InputPanel androidKeyStore;
	private InputPanel androidKeyAlias;
	private InputPanel androidKeyStorePassword;
	private InputPanel androidKeyAliasPassword;	

	private InputPanel[] options = { type, os, linux64JRE, linux32JRE, winJRE, version, icon, androidSDK, androidKeyStore, androidKeyAlias };

	public PackageDialog(Skin skin) {
		super("PACKAGE ADVENTURE", skin);
		
		arch = new InputPanel(skin, "Architecture", "Select the target OS for the game",
				ARCHS);
		dir = new FileInputPanel(skin, "Output Directory",
				"Select the output directory to put the package", true);
		type = new InputPanel(skin, "Type", "Select the type of the package", TYPES);
		os = new InputPanel(skin, "OS", "Select the OS of the package", OSS);
		linux64JRE = new FileInputPanel(skin, "JRE.Linux64", "Select the 64 bits Linux JRE Location to bundle", true);
		linux32JRE = new FileInputPanel(skin, "JRE.Linux32", "Select the 32 bits Linux JRE Location to bundle", true);
		winJRE = new FileInputPanel(skin, "JRE.Windows", "Select the Windows JRE Location to bundle", true);
		version = new InputPanel(skin, "Version", "Select the version of the package");
		icon = new FileInputPanel(skin, "Icon", "The icon for the .exe file", false);
		androidSDK = new FileInputPanel(skin, "SDK", "Select the Android SDK Location", true);
		androidKeyStore = new FileInputPanel(skin, "KeyStore", "Select the Key Store Location", false);
		androidKeyAlias = new InputPanel(skin, "KeyAlias", "Select the Key Alias Location");
		androidKeyStorePassword = new InputPanel(skin, "KeyStorePasswd", "Key Store Password", false);
		androidKeyAliasPassword = new InputPanel(skin, "KeyAliasPasswd", "Key Alias Password", false);
		
		getCenterPanel().add(arch);
		getCenterPanel().row().fill().expandX();
		getCenterPanel().add(dir);


		
		for(InputPanel i: options) {
			getCenterPanel().row().fill().expandX();
			getCenterPanel().add(i);
			i.setMandatory(true);
		}
		
		getCenterPanel().row().fill().expandX();
		getCenterPanel().add(androidKeyStorePassword);
		getCenterPanel().row().fill().expandX();
		getCenterPanel().add(androidKeyAliasPassword);
		
		dir.setMandatory(true);
		
		arch.setText(Ctx.project.getConfig().getProperty(ARCH_PROP, ARCHS[0]));
		dir.setText(Ctx.project.getConfig().getProperty(DIR_PROP, ""));
		
		
		for(InputPanel i: options) {
			String prop = Ctx.project.getConfig().getProperty("package." + i.getTitle());
			
			if(prop != null && !prop.isEmpty())
				i.setText(prop);
		}
		
		if(linux64JRE.getText().isEmpty()) {
			if(new File("./jre-linux64").exists()) {
				linux64JRE.setText(new File("./jre-linux64").getAbsolutePath());
			} else if(new File("../engine-dist/jre-linux64").exists()) {
				linux64JRE.setText(new File("../engine-dist/jre-linux64").getAbsolutePath());
			}
		}
		
		if(linux32JRE.getText().isEmpty()) {
			if(new File("./jre-linux32").exists()) {
				linux32JRE.setText(new File("./jre-linux32").getAbsolutePath());
			} else if(new File("../engine-dist/jre-linux32").exists()) {
				linux32JRE.setText(new File("../engine-dist/jre-linux32").getAbsolutePath());
			}
		}
		
		if(winJRE.getText().isEmpty()) {
			if(new File("./jre-win").exists()) {
				winJRE.setText(new File("./jre-win").getAbsolutePath());
			} else if(new File("../engine-dist/jre-win").exists()) {
				winJRE.setText(new File("../engine-dist/jre-win").getAbsolutePath());
			}
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

		init();
		
		archChanged();
	}

	@Override
	protected void ok() {

		Ctx.msg.show(getStage(), "Generating package...");
	
		String msg = packageAdv();
		Ctx.msg.show(getStage(), msg, 2000);

		Ctx.project.getConfig().setProperty(ARCH_PROP, arch.getText());
		Ctx.project.getConfig().setProperty(DIR_PROP, dir.getText());
		
		for(InputPanel i: options) {
			Ctx.project.getConfig().setProperty("package." + i.getTitle(), i.getText());
		}

	}

	private String packageAdv() {
		String antBuild = null;
		String antTarget = null;
		Properties props = new Properties();
		String msg = "Package generated SUCCESSFULLY";
		
		props.setProperty("title", Ctx.project.getTitle());
		props.setProperty("name", Ctx.project.getPackageTitle());
		props.setProperty("version", version.getText());
		

		if (arch.getText().equals("desktop")) {
			antBuild = "desktop.ant.xml";

			if (type.getText().equals(TYPES[0])) { // RUNNABLE JAR
				if (os.getText().equals("linux64")) {
					antTarget = "linux64";
					props.setProperty("linux64.jre", linux64JRE.getText());
				} else if (os.getText().equals("linux32")) {
					antTarget = "linux32";
					props.setProperty("linux32.jre", linux32JRE.getText());
				} else if (os.getText().equals("windows")) {
					antTarget = "win";
					props.setProperty("icon", icon.getText());
					props.setProperty("win.jre", winJRE.getText());
				} else if (os.getText().equals("all")) {
					antTarget = "dist";
					props.setProperty("icon", icon.getText());
					props.setProperty("linux64.jre", linux64JRE.getText());
					props.setProperty("linux32.jre", linux32JRE.getText());
					props.setProperty("win.jre", winJRE.getText());
				}
			} else {
				antTarget = "jar";
			}
		} else if (arch.getText().equals("android")) {
			antBuild = "android.ant.xml";
			antTarget = "dist";
			props.setProperty("sdk.dir", androidSDK.getText());
			props.setProperty("target", "android-15");
			props.setProperty("project.app.package", "org.bladecoder." + Ctx.project.getPackageTitle().toLowerCase()); // ANDROID PACKAGE
			props.setProperty("version.code", "10"); // TODO
			
			props.setProperty("key.store", androidKeyStore.getText());
			props.setProperty("key.alias", androidKeyAlias.getText());
			props.setProperty("key.store.password", androidKeyStorePassword.getText());
			props.setProperty("key.alias.password", androidKeyAliasPassword.getText());
//			props.setProperty("aapt.ignore.assets=1920_1080:tests");		
		}

		if (antTarget != null) {

			try {
				RunProccess.runAnt(antBuild, antTarget, dir.getText(),
						Ctx.project.getProjectPath(), props);
			} catch (IOException e) {
				msg = "Error Generating package\n\n" + e.getMessage();
			}
		} else {
			msg = "Packaging option NOT implemented yet.\n\n";
		}

		return msg;
	}

	private void archChanged() {
		for (InputPanel ip : options) {
			ip.setVisible(false);
		}
		
		androidKeyStorePassword.setVisible(false);
		androidKeyAliasPassword.setVisible(false);
		version.setVisible(true);

		String a = arch.getText();
		if (a.equals("desktop")) {
			type.setVisible(true);
			typeChanged();
		} else if (a.equals("android")) {
			androidSDK.setVisible(true);
			androidKeyStore.setVisible(true);
			androidKeyAlias.setVisible(true);
			androidKeyStorePassword.setVisible(true);
			androidKeyAliasPassword.setVisible(true);
		}
	}

	private void typeChanged() {
		if (type.getText().equals(TYPES[0])) {
			os.setVisible(true);
			osChanged();
		} else {
			os.setVisible(false);
			icon.setVisible(false);
		}
	}

	private void osChanged() {
		if (os.getText().equals("windows") || os.getText().equals("all")) {
			icon.setVisible(true);
			winJRE.setVisible(true);
		} else {
			icon.setVisible(false);
			winJRE.setVisible(false);
		}
		
		if (os.getText().equals("linux32") || os.getText().equals("all")) {
			linux32JRE.setVisible(true);
		} else {
			linux32JRE.setVisible(false);
		}
		
		if (os.getText().equals("linux64") || os.getText().equals("all")) {
			linux64JRE.setVisible(true);
		} else {
			linux64JRE.setVisible(false);
		}
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!dir.validateField())
			ok = false;

		for(InputPanel i: options) {
			if (i.isVisible() && !i.validateField())
				ok = false;
		}
		
		if (androidKeyStorePassword.isVisible() && !androidKeyStorePassword.validateField())
			ok = false;

		if (androidKeyAliasPassword.isVisible() && !androidKeyAliasPassword.validateField())
			ok = false;		

		if(icon.isVisible() && !icon.getText().endsWith(".ico")) {
			icon.setError(true);
			ok = false;
		}
		
		if(!new File(linux32JRE.getText() + "/bin/java").exists())
			ok = false;
		
		if(!new File(linux64JRE.getText() + "/bin/java").exists())
			ok = false;

		if(!new File(winJRE.getText() + "/bin/javaw.exe").exists())
			ok = false;
		
		return ok;
	}
}
