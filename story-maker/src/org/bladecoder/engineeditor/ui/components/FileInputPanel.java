package org.bladecoder.engineeditor.ui.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.bladecoder.engineeditor.Ctx;

@SuppressWarnings("serial")
public class FileInputPanel extends InputPanel {
	
	File cd;
	
	boolean dirOnly = false;
	
	public FileInputPanel(String title, String desc, boolean dirOnly) {
		this(title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."), dirOnly);
	}

	public FileInputPanel(String title, String desc, File current, boolean dOnly) {
		super(title, desc, new JTextField(), null);
		
		this.cd = current;
		this.dirOnly = dOnly;

		((JTextField) getField()).setEditable(false);

		((JTextField) getField()).addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {				
				JFileChooser chooser = new JFileChooser(cd);
				chooser.setDialogTitle("Select folder");
				
				if(dirOnly)
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				chooser.setMultiSelectionEnabled(false);

				if (chooser.showDialog(Ctx.window, "Ok") == JFileChooser.APPROVE_OPTION) {
					setText(chooser.getSelectedFile().getAbsolutePath());
					cd = chooser.getSelectedFile();
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
		});
	}
}
