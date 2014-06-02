package org.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engine.util.Config;
import org.bladecoder.engineeditor.utils.DinamicClassPath;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Project extends PropertyChange {
	public static final String PROP_PROJECTFILE = "projectFile";
	public static final String NOTIFY_SCENE_SELECTED = "SCENE_SELECTED";
	public static final String NOTIFY_ACTOR_SELECTED = "ACTOR_SELECTED";
	public static final String NOTIFY_FA_SELECTED = "FA_SELECTED";
	public static final String NOTIFY_VERB_SELECTED = "VERB_SELECTED";
	public static final String NOTIFY_PROJECT_LOADED = "PROJECT_LOADED";

	public static final String TMPL_JAR_FILENAME = "/res/data/project_tmpl.jar";

	public static final String ASSETS_PATH = "/assets";
	public static final String MODEL_PATH = ASSETS_PATH + "/model";
	public static final String ATLASES_PATH = ASSETS_PATH + "/atlases";
	public static final String BACKGROUNDS_PATH = ASSETS_PATH + "/backgrounds";
	public static final String FONTS_PATH = ASSETS_PATH + "/fonts";
	public static final String MUSIC_PATH = ASSETS_PATH + "/music";
	public static final String SOUND_PATH = ASSETS_PATH + "/sounds";
	public static final String OVERLAYS_PATH = ASSETS_PATH + "/overlays";
	public static final String SPRITE3D_PATH = ASSETS_PATH + "/3d";
	public static final String SPINE_PATH = ASSETS_PATH + "/spine";
	public static final String UI_PATH = ASSETS_PATH + "/ui";

	private static final String CONFIG_DIR = System.getProperty("user.home") + "/.AdventureComposer";
	private static final String CONFIG_FILENAME = "config.properties";
	
	public static final String LAST_PROJECT_PROP = "last_project";

	private final Properties editorConfig = new Properties();

	private File projectFile;

	private final WorldDocument world = new WorldDocument();	
	private Properties projectConfig;

	private ChapterDocument selectedChapter;
	private Element selectedScene;
	private Element selectedActor;
	private String selectedFA;
	
	private HashMap<String, TextureRegion> bgIconCache = new HashMap<String, TextureRegion>();

	final PropertyChangeListener modelChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		}

	};

	public Project() {
		world.addPropertyChangeListener(modelChangeListener);
		loadConfig();
	}
	
	public TextureRegion getBgIcon(String s) {
		TextureRegion icon = bgIconCache.get(s);
		
		if(icon == null) {
			bgIconCache.put(s, createBgIcon(s));
			icon = bgIconCache.get(s);
		}
		
		return icon;
	}
	
	private TextureRegion createBgIcon(String bg) {
		return new TextureRegion(new Texture(Gdx.files.absolute(getProjectPath() + "/" + BACKGROUNDS_PATH + 
				"/" + getResolutions().get(0).suffix + "/" + bg)));
	}

	private void loadConfig() {
		File dir = new File(CONFIG_DIR);
		File f = new File(CONFIG_DIR + "/" + CONFIG_FILENAME);

		if (!dir.exists())
			dir.mkdirs();

		try {
			if (!f.exists()) {
				f.createNewFile();
			} else {
				editorConfig.load(new FileInputStream(f));
			}
		} catch (IOException e) {
			EditorLogger.error(e.getMessage());
		}
	}
	
	public void saveConfig() {
		File f = new File(CONFIG_DIR + "/" + CONFIG_FILENAME);
		
		try {
			editorConfig.store(new FileOutputStream(f), null);
		} catch (IOException e) {
			EditorLogger.error(e.getMessage());
		}
	}
	
	public Properties getConfig() {
		return editorConfig;
	}

	public void setSelectedScene(Element scn) {
		Element old = null;

		old = selectedScene;

		selectedScene = scn;
		selectedActor = null;
		selectedFA = null;

		firePropertyChange(NOTIFY_SCENE_SELECTED, old, selectedScene);
	}

	public void setSelectedActor(Element a) {
		Element old = null;

		old = selectedActor;

		selectedActor = a;

		selectedFA = null;

		firePropertyChange(NOTIFY_ACTOR_SELECTED, old, selectedActor);
	}
	
	public ChapterDocument getSelectedChapter() {
		return selectedChapter;
	}

	public Element getSelectedScene() {
		return selectedScene;
	}

	public Element getSelectedActor() {
		return selectedActor;
	}

	public String getSelectedFA() {
		return selectedFA;
	}

	public void setSelectedFA(String id) {
		String old = selectedFA;

		selectedFA = id;

		firePropertyChange(NOTIFY_FA_SELECTED, old, selectedFA);
	}

	public String getModelPath() {
		return projectFile.getAbsolutePath() + MODEL_PATH;
	}

	public String getProjectPath() {
		return projectFile.getAbsolutePath();
	}

	public File getProjectDir() {
		return projectFile;
	}
	
	public String getTitle() {
		if(projectConfig == null) return null;
		
		return projectConfig.getProperty(Config.TITLE_PROP, getProjectDir().getName());
	}
	
	public String getPackageTitle() {
		return getTitle().replaceAll(" ", "");
	}

	public void createProject(File projectFile) throws ParserConfigurationException,
			TransformerException, IOException, SAXException {
		projectFile.mkdir();

		extractTMPL(projectFile);
//		loadProject(projectFile);
	}

	private void extractTMPL(File projectFile) throws IOException {

		URL u = getClass().getResource(TMPL_JAR_FILENAME);

		JarFile jar = new java.util.jar.JarFile(u.getFile());

		Enumeration<JarEntry> e = jar.entries();

		while (e.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) e.nextElement();
			java.io.File f = new java.io.File(projectFile.getAbsolutePath()
					+ java.io.File.separator + file.getName());

			if (file.isDirectory()) { // if its a directory, create it
				f.mkdir();
				continue;
			}

			java.io.InputStream is = jar.getInputStream(file); // get the input
																// stream

			java.io.FileOutputStream fos = new java.io.FileOutputStream(f);

			while (is.available() > 0) { // write contents of 'is' to 'fos'
				fos.write(is.read());
			}

			fos.close();
			is.close();
		}

		jar.close();
	}

	public void saveProject() throws IOException, TransformerException {
		if (projectFile != null) {
			world.save();
			selectedChapter.save();
			
			projectConfig.store(new FileOutputStream(projectFile.getAbsolutePath()+ "/" + ASSETS_PATH + "/" + Config.PROPERTIES_FILENAME), null);
		}
	}
	
	public void closeProject() {
		this.projectFile = null;
	}

	public void loadProject(File projectFile) throws IOException, ParserConfigurationException,
			SAXException {
		this.projectFile = projectFile;

		if (checkProjectStructure()) {
			
			
			// Add 'bin' dir from project directory to classpath
			// WARNING: Previous 'bin' folders are not deleted from the classpath
			// That can not be a problem if the package of the custom actions is different
			// in the loaded project.
			for (File f : projectFile.listFiles()) {
				if (f.getName().equals("bin"))
					DinamicClassPath.addFile(f);
			}
			
			world.setModelPath(projectFile.getAbsolutePath() + "/" + MODEL_PATH);
			world.load();
			selectedChapter = world.loadChapter(world.getInitChapter());
			editorConfig.setProperty(LAST_PROJECT_PROP, projectFile.getAbsolutePath());
						
			projectConfig = new Properties();
			projectConfig.load(new FileInputStream(projectFile.getAbsolutePath()+ "/" + ASSETS_PATH + "/" + Config.PROPERTIES_FILENAME));
			firePropertyChange(NOTIFY_PROJECT_LOADED);
		} else {
			this.projectFile = null;
			throw new IOException("Project not found.");
		}	
	}

	public boolean checkProjectStructure() {
		if (!new File(getModelPath()).exists()) {
			if (projectFile.getName().equals("assets")) {
				projectFile = projectFile.getParentFile();
				if (!new File(getModelPath()).exists())
					return false;
			} else if (projectFile.getName().equals("model")) {
				projectFile = projectFile.getParentFile().getParentFile();
				if (!new File(getModelPath()).exists())
					return false;
			} else
				return false;
		}

		return true;
	}

	public WorldDocument getWorld() {
		return world;
	}

	public Element getActor(String id) {
		return selectedChapter.getActor(selectedScene, id);
	}

	public List<Resolution> getResolutions() {
		File atlasesPath = new File(projectFile.getAbsolutePath() + ATLASES_PATH);
		ArrayList<Resolution> l = new ArrayList<Resolution>();

		File[] list = atlasesPath.listFiles();

		for (int i = 0; i < list.length; i++) {
			String name = list[i].getName();

			if (list[i].isDirectory() && name.contains("_")) {
				String s[] = name.split("_");
				if (s.length < 2) {
					continue;
				}

				int width = Integer.parseInt(s[0]);
				int height = Integer.parseInt(s[1]);

				l.add(new Resolution(width, height, name));
			}
		}

		return l;
	}

	public String getResDir() {
		int width = (world == null ? WorldDocument.DEFAULT_WIDTH : world.getWidth());
		int height = (world == null ? WorldDocument.DEFAULT_HEIGHT : world.getHeight());

		return width + "_" + height;
	}

	public void setProjectProperty(String titleProp, String value) {
		projectConfig.setProperty(titleProp, value);
	}

	public String getProjectProperty(String titleProp) {
		return projectConfig.getProperty(titleProp);
	}

	public void loadChapter(String selChapter) throws ParserConfigurationException, SAXException, IOException {
		selectedChapter = world.loadChapter(selChapter);
		
//		if(selectedChapter != null) {
//			NodeList scenes = selectedChapter.getScenes();
//			if(scenes.getLength()>0)
//				setSelectedScene((Element)scenes.item(0));
//			else
//				setSelectedScene(null);
//		}
	}
	
	public void dispose() {
		for(TextureRegion r:bgIconCache.values())
			r.getTexture().dispose();
	}
}
