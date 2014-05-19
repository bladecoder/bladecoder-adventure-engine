package org.bladecoder.engineeditor.ui.components.scene2d;

import java.io.File;

import javax.swing.JFileChooser;

import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class FileInputPanel extends InputPanel {
	
	File cd;
	
	boolean dirOnly = false;
	
	public FileInputPanel(Skin skin, String title, String desc, boolean dirOnly) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."), dirOnly);
	}

	public FileInputPanel(Skin skin, String title, String desc, File current, boolean dOnly) {
		super(skin, title, desc, new TextField(null, skin), null);
		
		this.cd = current;
		this.dirOnly = dOnly;

//		((TextField) getField()).setEditable(false);

		((TextField) getField()).addListener(new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				JFileChooser chooser = new JFileChooser(cd);
				chooser.setDialogTitle("Select folder");
				
				if(dirOnly)
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				chooser.setMultiSelectionEnabled(false);

				if (chooser.showDialog(null, "Ok") == JFileChooser.APPROVE_OPTION) {
					setText(chooser.getSelectedFile().getAbsolutePath());
					cd = chooser.getSelectedFile();
				}				
			}		
		});
	}
}
