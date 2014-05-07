package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.glcanvas.FACanvas;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;

@SuppressWarnings("serial")
public class CreateEditFADialog extends CreateEditElementDialog {
	public static final String INFO = "Define sprites and frame animations";
	
	private InputPanel[] inputs = {
			new InputPanel("Source", "<html>Select the source where the sprite or animation is defined</html>", new String[0]),
			new InputPanel("ID", "<html>Select the id of the animation</html>", new String[0]),
			new InputPanel("Animation type", "<html>Select the type of the animation</html>",
					ChapterDocument.ANIMATION_TYPES),
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
					"<html>Select the sound ID that will be play when displayed</html>"),
			new InputPanel("Preload",
							"<html>Preload the animation when the scene is loaded</html>", Param.Type.BOOLEAN, true, "true", null),
			new InputPanel("Dispose When Played",
									"<html>Dispose de animation when the animation is played</html>",Param.Type.BOOLEAN,  true, "false", null)							
					};

	InputPanel typePanel = inputs[2];

	String attrs[] = { "source", "id", "animation_type", "speed",  "delay", "count", "inD",
			"outD", "sound", "preload", "disposed_when_played"};
	
	FACanvas faCanvas = new FACanvas(this);

	@SuppressWarnings("unchecked")
	public CreateEditFADialog(java.awt.Frame parentWindow, BaseDocument doc, Element p, Element e) {
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
				EditorLogger.debug("CreateEditFADialog.setSource():" +  inputs[0].getText());
				
				faCanvas.setSource(parent.getAttribute("type"), inputs[0].getText());
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


		init(inputs, attrs, doc, p, "frame_animation", e);
		
		LwjglAWTCanvas canvas = new LwjglAWTCanvas(faCanvas);
		try{
			setInfoComponent(canvas.getCanvas());
		} catch(Exception ex) {
			EditorLogger.error("ERROR ADDING LIBGDX/OPENGL CANVAS");
		}
		
		addSources();
	}
	
	private void setCanvasFA() {
		String id = inputs[1].getText();
		String type = typePanel.getText();
		String speed =  inputs[3].getText();
		
		@SuppressWarnings("unchecked")
		JComboBox<String> cb = (JComboBox<String>) inputs[1].getField();

		
		if (e != null || cb.getSelectedIndex() != 0)
			faCanvas.setFrameAnimation(id, speed, type);		
	}
	
	public void fillAnimations(String []ids) {
		EditorLogger.debug("CreateEditFADialog.fillAnimations()");
		
		@SuppressWarnings("unchecked")
		JComboBox<String> cb = (JComboBox<String>) inputs[1].getField();
		cb.removeAllItems();
		
		// When creating, give option to add all elements
		if(e == null)
			cb.addItem("<ADD ALL>");
		
		for(String s:ids)
			cb.addItem(s);
		
		
		setCanvasFA();
	}
	
	String ext;

	private void addSources() {
		@SuppressWarnings("unchecked")
		JComboBox<String> cb = (JComboBox<String>) inputs[0].getField();
		cb.removeAllItems();
		String[] src = getSources();
		
		for(String s:src)
			cb.addItem(s);

	}
	
	
	private String[] getSources() {
		String path = null;
		String type = parent.getAttribute("type");
		
		if(type.equals(ChapterDocument.FOREGROUND_ACTOR_TYPE) || type.equals(ChapterDocument.ATLAS_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir();
			ext = ".atlas";
		} else if(type.equals(ChapterDocument.SPRITE3D_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPRITE3D_PATH;
			ext = ".g3db";
		} else if(type.equals(ChapterDocument.SPINE_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPINE_PATH;
			ext = ".json";
		}
			

		File f = new File(path);

		String sources[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(ext))
					return true;

				return false;
			}
		});

		Arrays.sort(sources);
		
		for(int i=0; i < sources.length; i++)
			sources[i] = sources[i].substring(0, sources[i].length() - ext.length());

		return sources;
	}
	
	/**
	 * Override to append all animations if selected.
	 */
	@Override
	protected void ok() {
		@SuppressWarnings("unchecked")
		JComboBox<String> cb = (JComboBox<String>) inputs[1].getField();

		
		if (e == null && cb.getSelectedIndex() == 0) {
			for(int i = 1; i<cb.getItemCount(); i++) {
				create();
				fill();
				doc.setId(e, cb.getItemAt(i));
			}
			
			dispose();
		} else {
			super.ok();
		}
	}

}
