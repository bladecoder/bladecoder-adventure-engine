package org.bladecoder.engineeditor.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class AddAllFAFromAtlasDialog extends EditDialog {
	public static final String INFO = "Add all the sprites in the atlas to the selected character";
	
	private String atlasesList[] = getAtlasesList();

	private InputPanel atlases =
			new InputPanel("Atlas", "<html>Select the atlas</html>", atlasesList);


	
	BaseDocument doc;
	Element actor;
	
	public AddAllFAFromAtlasDialog(java.awt.Frame parentWindow, BaseDocument doc, Element actor) {
		super(parentWindow);
		
		this.doc = doc;
		this.actor = actor;

		centerPanel.add(atlases);

		setTitle("ADD ALL SPRITES FROM ATLAS");
		setInfo(INFO);

		init(parentWindow);
	}

	private String[] getAtlasesList() {
		String atlases_path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir();

		File f = new File(atlases_path);

		String atlases[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".atlas"))
					return true;

				return false;
			}
		});

		Arrays.sort(atlases);
		
		for(int i=0; i < atlases.length; i++)
			atlases[i] = atlases[i].substring(0, atlases[i].length() - 6);

		return atlases;
	}
	
	private String[] getSprites(String atlas) {
		String atlas_path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir() + "/" + atlas + ".atlas";
		
		FileInputStream fstream;
		BufferedReader br = null;
		Hashtable<String,String> list = null; // Hashtable to avoid duplicates
		
		try {
			fstream = new FileInputStream(atlas_path);
			br = new BufferedReader(new InputStreamReader(fstream));
			
			list = new Hashtable<String,String>();
			
			String strLine;

			br.readLine();
			br.readLine();
			while ((strLine = br.readLine()) != null)   {
				if(!strLine.endsWith(".png") && strLine.length() > 0 && !(strLine.charAt(0) == ' ') && !(strLine.contains(":") )) {
					list.put(strLine, strLine);
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(br!=null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}		
		
		if(list == null) return new String[0];
		
		String s[] = list.values().toArray(new String[0]);
		
		Arrays.sort(s);
		
		return s;
	}

	@Override
	protected boolean validateFields() {
		return true;
	}

	@Override
	protected void ok() {
		dispose();
		
		String atlas = atlases.getText();
		String sprites[] = getSprites(atlas);
		
		for(String sprite:sprites) {
			Element fa = doc.createElement(actor, "frame_animation");
			
			fa.setAttribute("atlas", atlas);
			fa.setAttribute("id", sprite);
			fa.setAttribute("animation_type", "repeat");
			fa.setAttribute("speed", "1.0");
			
			doc.setModified(fa);
		}
	}

}
