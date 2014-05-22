package org.bladecoder.engineeditor.ui.components;

import java.io.File;

import javax.swing.JFileChooser;

import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class FileInputPanel extends InputPanel {
	
	private File cd;
	
	private boolean dirOnly = false;
	
	public FileInputPanel(Skin skin, String title, String desc, boolean dirOnly) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."), dirOnly);
	}

	public FileInputPanel(Skin skin, String title, String desc, File current, boolean dOnly) {
		super(skin, title, desc, new TextButton("Select file", skin), null);
		
		this.cd = current;
		this.dirOnly = dOnly;

//		((TextField) getField()).setEditable(false);

		((TextButton) getField()).addListener(new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				JFileChooser chooser = new JFileChooser(cd);			
				
				if(dirOnly) {
					chooser.setDialogTitle("Select folder");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				} else {
					chooser.setDialogTitle("Select file");
				}
				
				chooser.setMultiSelectionEnabled(false);

				if (chooser.showDialog(null, "Ok") == JFileChooser.APPROVE_OPTION) {
					((TextButton) getField()).setText(chooser.getSelectedFile().getAbsolutePath());
					cd = chooser.getSelectedFile();
				}				
			}		
		});
	}
	
	public File getFile() {
		return cd;
	}
}
