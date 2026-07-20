package com.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class EditInkDialog extends EditDialog {

	private InputPanel inkStory;

	public EditInkDialog(Skin skin) {
		super("", skin);

		inkStory = InputPanelFactory.createInputPanel(skin, "Ink Story", "Ink story file for this chapter",
				getInkFiles(), false);

		setTitle("SET INK STORY FILE");
		setInfo("Each chapter have its own ink story file.\n\nStories are compiled Ink files with the '.ink.json' extension in the 'assets/model' folder.");

		addInputPanel(inkStory);

		inkStory.setText(Ctx.project.getWorld().getInkManager().getStoryName());
	}

	@Override
	protected boolean validateFields() {
		return inkStory.validateField();
	}

	@Override
	protected void ok() {
		Ctx.project.getWorld().getInkManager().setStoryName(inkStory.getText());
		Ctx.project.setModified();
	}

	private String[] getInkFiles() {
		String path = Ctx.project.getAssetPath() + Project.MODEL_PATH;

		File f = new File(path);

		String[] inkFiles = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(EngineAssetManager.INK_EXT))
					return true;

				return false;
			}
		});

		if (inkFiles == null)
			inkFiles = new String[0];

		for (int i = 0; i < inkFiles.length; i++)
			inkFiles[i] = inkFiles[i].substring(0, inkFiles[i].length() - EngineAssetManager.INK_EXT.length());

		Arrays.sort(inkFiles);

		return inkFiles;
	}
}
