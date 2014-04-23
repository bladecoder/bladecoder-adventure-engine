package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditSoundDialog extends CreateEditElementDialog {
	
	private InputPanel[] inputs = {
		new InputPanel("Sound ID", "<html>The id of the sound</html>"),
		new InputPanel("Filename", "<html>Filename of the sound</html>", getSoundList()),
		new InputPanel("Loop", "<html>True if the sound is looping</html>", Param.Type.BOOLEAN, false),
		new InputPanel("Volume", "<html>Select the volume</html>")	
	};


	String attrs[] = { "id", "filename", "loop", "volume"};	

	public CreateEditSoundDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);
		
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
