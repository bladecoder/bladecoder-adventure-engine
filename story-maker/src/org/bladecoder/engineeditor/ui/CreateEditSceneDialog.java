package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class CreateEditSceneDialog extends CreateEditElementDialog {

	public static final String INFO = "<html><br/><br/>An adventure is composed of many scenes (screens).<br/><br/>" +
			"Inside a scene there are actors and a 'player'.<br/> The player/user can interact with the actors throught 'verbs'.<br/><br/>" +
			"</html>";

	WorldDocument w;
	
	private String bgList[] = getBgList();
	private String musicList[] = getMusicList();
	
	private InputPanel[] inputs = {
			new InputPanel("Scene ID",
					"<html>The ID is mandatory for scenes. <br/>IDs can not contain '.' or '_' characters.</html>"),
			new InputPanel("Background",
					"<html>The background for the scene</html>", bgList),
			new InputPanel("Lightmap",
							"<html>The lightmap for the scene</html>", bgList),					
			new InputPanel("Atlases",
					"<html>The list of atlases preloaded in the scene. <br/>The atlases list are comma separated.</html>"),
			new InputPanel("Depth Vector",
							"<html>X: the actor scale when y=0, Y: the actor scale when y=scene height .</html>", Param.Type.VECTOR2, false),					
			new InputPanel("Music Filename",
					"<html>The music for the scene</html>", musicList),
			new InputPanel("Loop Music",
					"<html>If the music is playing in looping</html>", Param.Type.BOOLEAN, false),
			new InputPanel("Initial music delay",
					"<html>The time to wait before playing</html>", Param.Type.FLOAT, false),
			new InputPanel("Repeat music delay",
					"<html>The time to wait before repetitions</html>", Param.Type.FLOAT, false),
					
	};
	
	String attrs[] = {"id", "background", "lightmap", "atlases", "depth_vector", "music", "loop_music", "initial_music_delay", "repeat_music_delay"};

	@SuppressWarnings("unchecked")
	public CreateEditSceneDialog(java.awt.Frame parentWindow, WorldDocument w, SceneDocument doc, Element parent,
				Element e) {
		
		super(parentWindow);
		
		this.w = w;
		
		setInfo(INFO);
		
		inputs[0].setMandatory(true);

		init(inputs, attrs, doc, parent, "scene", e);
		
		((JComboBox<String>) inputs[1].getField()).addActionListener(new ActionListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void actionPerformed(ActionEvent e) {
				String bg = inputs[1].getText();
				String bgPath = Ctx.project.getProjectPath() + Project.BACKGROUNDS_PATH + "/"
						+ Ctx.project.getResDir() + "/" + bg;

				File f = new File(bgPath);				

				try {
					setInfoIcon(f.toURL());
				} catch (MalformedURLException e1) {
					EditorLogger.error(e1.getMessage());
				}
			}
		});		
	}

	private String[] getBgList() {
		String bgPath = Ctx.project.getProjectPath() + Project.BACKGROUNDS_PATH + "/"
				+ Ctx.project.getResDir();

		File f = new File(bgPath);

		String bgs[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if ((arg1.matches("_[1-9]\\.")))
					return false;

				return true;
			}
		});

		Arrays.sort(bgs);
		
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(bgs));
		l.add(0,"");

		return l.toArray(new String[bgs.length + 1]);
	}
	
	private String[] getMusicList() {
		String path = Ctx.project.getProjectPath() + Project.MUSIC_PATH;

		File f = new File(path);

		String musicFiles[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".ogg") || arg1.endsWith(".mp3"))
					return true;

				return false;
			}
		});

		Arrays.sort(musicFiles);
		
		String musicFiles2[] = new String[musicFiles.length + 1];
		musicFiles2[0] = "";
		
		for(int i=0; i < musicFiles.length; i++)
			musicFiles2[i + 1] = musicFiles[i];

		return musicFiles2;
	}	
	
	@Override
	protected void create() {
		try {
			SceneDocument scn = w.createScene(inputs[0].getText());
			doc = scn;
			e = scn.getElement();
			
			inputs[0].setText(scn.getId());
		} catch (FileNotFoundException | TransformerException | ParserConfigurationException e) {
			EditorLogger.error(e.getMessage());
		}
	}
	
	@Override
	protected void fill() {
		if(!inputs[0].getText().equals(e.getAttribute("id"))) {
			
			try {
				w.renameScene((SceneDocument)doc, inputs[0].getText());
			} catch (FileNotFoundException | TransformerException | ParserConfigurationException e1) {
				EditorLogger.error(e1.getMessage());
			}
		}
		
		super.fill();
	}
}
