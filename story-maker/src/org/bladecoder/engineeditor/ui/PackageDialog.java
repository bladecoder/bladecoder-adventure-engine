package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JComboBox;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.FileInputPanel;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.RunProccess;

@SuppressWarnings("serial")
public class PackageDialog extends EditDialog {
	private static final String ARCH_PROP = "package.arch";
	private static final String DIR_PROP = "package.dir";
	
	private static final String INFO = "Package the Adventure for distribution";
	private static final String[] ARCHS = { "desktop", "android", "html", "ios" };
	private static final String[] TYPES = { "Bundle JRE", "Runnable jar" };
	private static final String[] OSS = { "all", "windows", "linux64", "linux32", "macOSX" };

	private InputPanel arch = new InputPanel("Architecture", "Select the target OS for the game",
			ARCHS);
	private InputPanel dir = new FileInputPanel("Output Directory",
			"Select the output directory to put the package", true);
	private InputPanel type = new InputPanel("Type", "Select the type of the package", TYPES);
	private InputPanel os = new InputPanel("OS", "Select the OS of the package", OSS);
	private InputPanel linux64JRE = new FileInputPanel("JRE.Linux64", "Select the 64 bits Linux JRE Location to bundle", true);
	private InputPanel linux32JRE = new FileInputPanel("JRE.Linux32", "Select the 32 bits Linux JRE Location to bundle", true);
	private InputPanel winJRE = new FileInputPanel("JRE.Windows", "Select the Windows JRE Location to bundle", true);
	private InputPanel version = new InputPanel("Version", "Select the version of the package");
	private InputPanel icon = new FileInputPanel("Icon", "The icon for the .exe file", false);
	private InputPanel androidSDK = new FileInputPanel("SDK", "Select the Android SDK Location", true);
	private InputPanel androidKeyStore = new FileInputPanel("KeyStore", "Select the Key Store Location", false);
	private InputPanel androidKeyAlias = new InputPanel("KeyAlias", "Select the Key Alias Location");
	private InputPanel androidKeyStorePassword = new InputPanel("KeyStorePasswd", "Key Store Password", false);
	private InputPanel androidKeyAliasPassword = new InputPanel("KeyAliasPasswd", "Key Alias Password", false);	

	private InputPanel[] options = { type, os, linux64JRE, linux32JRE, winJRE, version, icon, androidSDK, androidKeyStore, androidKeyAlias };

	@SuppressWarnings("unchecked")
	public PackageDialog(java.awt.Frame parent) {
		super(parent);

		centerPanel.add(arch);
		centerPanel.add(dir);
		
		for(InputPanel i: options) {
			centerPanel.add(i);
			i.setMandatory(true);
		}
		
		centerPanel.add(androidKeyStorePassword);
		centerPanel.add(androidKeyAliasPassword);
		
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

		setTitle("PACKAGE ADVENTURE");
		setInfo(INFO);

		((JComboBox<String>) (arch.getField())).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				archChanged();
			}
		});

		((JComboBox<String>) (type.getField())).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				typeChanged();
			}
		});

		((JComboBox<String>) (os.getField())).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				osChanged();
			}
		});

		init(parent);
		
		archChanged();
	}

	@Override
	protected void ok() {
		dispose();

		Ctx.window.getScnCanvas().setMsg("Generating package...");

		new Thread(new Runnable() {
			@Override
			public void run() {
				String msg = packageAdv();
				Ctx.window.getScnCanvas().setMsgWithTimer(msg, 2000);
			}
		}).start();
		
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
