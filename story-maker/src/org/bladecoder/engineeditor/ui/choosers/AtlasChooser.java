package org.bladecoder.engineeditor.ui.choosers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditDialog;

@SuppressWarnings("serial")
public class AtlasChooser extends EditDialog {

	public static final String INFO = "<html>Select the atlas and the sprite</html>";

	private String atlas;
	private String id;
	private String atlases_path;
	
	JList<String> atlasList;
	JList<String> spriteList;

	public AtlasChooser(java.awt.Frame parent) {
		super(parent);

		setInfo(INFO);
		
		atlasList = new JList<String>(new DefaultListModel<String>());
		spriteList = new JList<String>(new DefaultListModel<String>());
		
		atlases_path = Ctx.project.getProjectPath() + Project.ATLASES_PATH;
		
		atlases_path += "/" + Ctx.project.getResDir();
		
		addAtlases();
		

//		projectName = new InputPanel("Project Name",
//				"<html>Select the name of the project</html>");
//
//		location = new InputPanel("Location",
//				"<html>Select the folder location for the project</html>");
		

		setTitle("SPRITE CHOOSER");

		centerPanel.add(atlasList);
		centerPanel.add(spriteList);
		
		init(parent);
	}
	
	private void addAtlases() {
		if (Ctx.project.getProjectDir() != null) {
			
			File f = new File(atlases_path);
			
			String atlases [] = f.list(new FilenameFilter()  {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					if(arg1.endsWith(".atlas"))
						return true;
					
					return false;
				}
			});

			DefaultListModel<String> lm = (DefaultListModel<String>) atlasList.getModel();

			lm.clear();
			
			Arrays.sort(atlases);

			for (String s : atlases)
				lm.addElement(s);

			if (lm.size() > 0) {
				atlasList.setSelectedIndex(0);
			}
		}
	}	

	@Override
	protected void ok() {
		atlas = atlasList.getSelectedValue();
		id = spriteList.getSelectedValue();
	}
	
	public String getAtlas() {
		return atlas;
	}
	
	public String getFAId() {
		return id;
	}


	@Override
	protected boolean validateFields() {
		boolean isOk = true;
		
				
		return isOk;
	}
}
