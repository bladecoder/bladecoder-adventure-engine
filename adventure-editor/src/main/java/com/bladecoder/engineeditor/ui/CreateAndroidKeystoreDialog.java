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

import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.FileInputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class CreateAndroidKeystoreDialog extends EditDialog {

	private static final String INFO = "Create the keystore needed to sign Android packages.";

	private InputPanel keyStoreFile;
	private InputPanel androidKeyAlias;
	private InputPanel androidKeyStorePassword;
	private InputPanel androidKeyAliasPassword;

	protected ChangeListener listener;

	public CreateAndroidKeystoreDialog(Skin skin) {
		super("CREATE KEY STORE FOR ANDROID", skin);

		keyStoreFile = new FileInputPanel(skin, "Select the key store", "Select the key store file name and location",
				FileInputPanel.DialogType.SAVE_FILE);

		androidKeyAlias = InputPanelFactory.createInputPanel(skin, "KeyAlias", "Select the Key ID/Alias", true);

		androidKeyStorePassword = InputPanelFactory.createInputPanel(skin, "KeyStorePasswd", "Key Store Password",
				true);
		androidKeyAliasPassword = InputPanelFactory.createInputPanel(skin, "KeyAliasPasswd", "Key Alias Password",
				true);

		addInputPanel(keyStoreFile);
		addInputPanel(androidKeyAlias);
		addInputPanel(androidKeyStorePassword);
		addInputPanel(androidKeyAliasPassword);

		setInfo(INFO);
	}

	@Override
	protected void ok() {
		createKeyStore();
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!keyStoreFile.validateField())
			ok = false;

		if (!androidKeyAlias.validateField())
			ok = false;

		if (androidKeyStorePassword.getText() == null || androidKeyStorePassword.getText().length() < 6) {
			Message.showMsgDialog(getStage(), "Error", "Keystore password must be at least 6 character long");
			ok = false;

			return false;
		}

		if (androidKeyAliasPassword.getText() == null || androidKeyAliasPassword.getText().length() < 6) {
			Message.showMsgDialog(getStage(), "Error", "Key password must be at least 6 character long");
			ok = false;
		}

		return ok;
	}

	private void createKeyStore() {
		// keytool -genkey -v -keystore my-release-key.keystore -alias alias_name
		// -keyalg RSA -keysize 2048 -validity 10000

		String[] args = { "-genkey", "-noprompt", "-v", "-keystore", getKeyStorePath(), "-alias", getKeyAlias(),
				"-keyalg", "RSA", "-keysize", "2048", "-validity", "10000", "-storepass", getKeyStorePassword(),
				"-dname", "CN=bladeengine.com", "-keypass", getKeyAliasPassword() };

		try {
			Process p = RunProccess.runJavaHomeBin("keytool", Arrays.asList(args));
			p.waitFor();

			if (p.exitValue() == 0) {
				if (listener != null)
					listener.changed(new ChangeEvent(), this);
			} else {
				Message.showMsgDialog(getStage(), "Error", "Error generating key");
				cancel();
			}
		} catch (Exception e) {
			Message.showMsgDialog(getStage(), "Error", e.getMessage());
			cancel();
		}

	}

	public String getKeyStorePath() {
		return keyStoreFile.getText();
	}

	public String getKeyAlias() {
		return androidKeyAlias.getText();
	}

	public String getKeyStorePassword() {
		return androidKeyStorePassword.getText();
	}

	public String getKeyAliasPassword() {
		return androidKeyAliasPassword.getText();
	}

	public void setListener(ChangeListener l) {
		listener = l;
	}
}
