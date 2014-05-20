package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditSoundDialog extends EditElementDialog {
	
	private InputPanel[] inputs;


	String attrs[] = { "id", "filename", "loop", "volume"};	

	public EditSoundDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel [4];
		inputs[0] = new InputPanel(skin, "Sound ID", "The id of the sound");
		inputs[1] = new InputPanel(skin, "Filename", "Filename of the sound", getSoundList());
		inputs[2] = new InputPanel(skin, "Loop", "True if the sound is looping", Param.Type.BOOLEAN, false);
		inputs[3] = new InputPanel(skin, "Volume", "Select the volume");
		
		inputs[0].setMandatory(true);
		inputs[1].setMandatory(true);

		setInfo("Actors and scenes can have a list of sounds that can be associated to Sprites or played with the 'sound' action");

		init(inputs, attrs, doc, parent, "sound", e);
	}
	
	private String[] getSoundList() {
		String path = Ctx.project.getProjectPath() + Project.SOUND_PATH;

		File f = new File(path);

		String soundFiles[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".ogg") || arg1.endsWith(".wav") || arg1.endsWith(".mp3"))
					return true;

				return false;
			}
		});

		Arrays.sort(soundFiles);

		return soundFiles;
	}	
}
