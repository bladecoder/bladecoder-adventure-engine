package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.glcanvas.FACanvas;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;

@SuppressWarnings("serial")
public class CreateEditFADialog extends CreateEditElementDialog {
	public static final String INFO = "Define sprites and frame animations";
	
	private String atlasesList[] = getAtlasesList();

	private InputPanel[] inputs = {
			new InputPanel("Atlas", "<html>Select the atlas where the sprite is defined</html>", atlasesList),
			new InputPanel("Sprite ID", "<html>Select the id of the sprite</html>", getSprites(atlasesList[0])),
			new InputPanel("Animation type", "<html>Select the type of the animation</html>",
					SceneDocument.ANIMATION_TYPES),
			new InputPanel("Speed", "<html>Select the speed of the animation in secods</html>",
					Param.Type.FLOAT, true),
			new InputPanel("Delay", "<html>Select the delay between repeats in seconds</html>",
					Param.Type.FLOAT, false),
			new InputPanel("Count", "<html>Select the repeat times</html>", Param.Type.INTEGER, false),
			new InputPanel("In Dist",
					"<html>Select the distance in pixels to add to the actor position when the sprite is displayed</html>",
					Param.Type.VECTOR2, false),
			new InputPanel("Out Dist",
							"<html>Select the distance in pixels to add to the actor position when the sprite is changed</html>",
							Param.Type.VECTOR2, false),					
			new InputPanel("Sound",
					"<html>Select the sound ID that will be play when displayed</html>") };

	InputPanel typePanel = inputs[2];

	String attrs[] = { "atlas", "id", "animation_type", "speed",  "delay", "count", "inD",
			"outD", "sound" };
	
	FACanvas faCanvas = new FACanvas();

	@SuppressWarnings("unchecked")
	public CreateEditFADialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent, Element e) {
		super(parentWindow);

		setInfo(INFO);

		((JComboBox<String>) typePanel.getField()).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String type = typePanel.getText();

				if (type.equals("repeat") || type.equals("yoyo")) {
					inputs[4].setVisible(true);
					inputs[5].setVisible(true);
				} else {
					inputs[4].setVisible(false);
					inputs[5].setVisible(false);
				}
			}
		});
		
		((JComboBox<String>) inputs[0].getField()).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String atlas = (String) inputs[0].getText();
				JComboBox<String> cb = (JComboBox<String>) inputs[1].getField();
				cb.removeAllItems();
				String[] ids = getSprites(atlas);
				
				for(String s:ids)
					cb.addItem(s);
				
				
				setCanvasFA();
			}
		});
		
		((JComboBox<String>) inputs[1].getField()).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCanvasFA();
			}
		});
		
		((JTextField) inputs[3].getField()).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCanvasFA();
			}
		});

		inputs[4].setVisible(false);
		inputs[5].setVisible(false);


		init(inputs, attrs, doc, parent, "frame_animation", e);
		
		LwjglAWTCanvas canvas = new LwjglAWTCanvas(faCanvas);
		try{
			setInfoComponent(canvas.getCanvas());
		} catch(Exception ex) {
			EditorLogger.error("ERROR ADDING LIBGDX/OPENGL CANVAS");
		}
	}
	
	private void setCanvasFA() {
		String atlas = inputs[0].getText();
		String id = inputs[1].getText();
		String type = typePanel.getText();
		String speed =  inputs[3].getText();
		
		faCanvas.setFrameAnimation(atlas, id, speed, type);		
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

}
