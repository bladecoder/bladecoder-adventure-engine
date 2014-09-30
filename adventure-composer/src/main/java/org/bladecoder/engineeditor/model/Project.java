/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engine.util.Config;
import org.bladecoder.engineeditor.setup.Dependency;
import org.bladecoder.engineeditor.setup.DependencyBank;
import org.bladecoder.engineeditor.setup.BladeEngineSetup;
import org.bladecoder.engineeditor.setup.ProjectBuilder;
import org.bladecoder.engineeditor.setup.DependencyBank.ProjectDependency;
import org.bladecoder.engineeditor.setup.DependencyBank.ProjectType;
import org.bladecoder.engineeditor.utils.DinamicClassPath;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Project extends PropertyChange {
	public static final String PROP_PROJECTFILE = "projectFile";
	public static final String NOTIFY_SCENE_SELECTED = "SCENE_SELECTED";
	public static final String NOTIFY_ACTOR_SELECTED = "ACTOR_SELECTED";
	public static final String NOTIFY_FA_SELECTED = "FA_SELECTED";
	public static final String NOTIFY_VERB_SELECTED = "VERB_SELECTED";
	public static final String NOTIFY_PROJECT_LOADED = "PROJECT_LOADED";

	public static final String ASSETS_PATH = "/android/assets";
	public static final String MODEL_PATH = ASSETS_PATH + "/model";
	public static final String ATLASES_PATH = ASSETS_PATH + "/atlases";
	public static final String BACKGROUNDS_PATH = ASSETS_PATH + "/backgrounds";
	public static final String FONTS_PATH = ASSETS_PATH + "/fonts";
	public static final String MUSIC_PATH = ASSETS_PATH + "/music";
	public static final String SOUND_PATH = ASSETS_PATH + "/sounds";
	public static final String IMAGE_PATH = ASSETS_PATH + "/images";
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
			try {
				bgIconCache.put(s, createBgIcon(s));
			} catch (Exception e) {
				return null;
			}
			
			icon = bgIconCache.get(s);
		}
		
		return icon;
	}
	
	private TextureRegion createBgIcon(String bg) {
		return new TextureRegion(new Texture(Gdx.files.absolute(getProjectPath() + "/" + BACKGROUNDS_PATH + 
				"/1/" + bg)));
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
		return getTitle().replace(" ", "").replace("'", "");
	}

	public void createProject(String projectDir, String name, String sdkLocation) throws ParserConfigurationException,
			TransformerException, IOException, SAXException {
		createLibGdxProject(projectDir, name, "org.bladecoder.engine", "BladeEngine", sdkLocation);
		
		projectFile = new File(projectDir + "/" + name);
		
//		loadProject(projectFile);
	}
	
	private void createLibGdxProject(String projectDir, String name, String pkg, String mainClass, String sdkLocation) throws IOException {
		String sdk = "";
		if (System.getenv("ANDROID_HOME") != null && sdkLocation == null) {
			sdk = System.getenv("ANDROID_HOME");
		} else {
			sdk = sdkLocation;
		}

		DependencyBank bank = new DependencyBank();
		ProjectBuilder builder = new ProjectBuilder(bank);
		List<ProjectType> projects = new ArrayList<ProjectType>();
		projects.add(ProjectType.CORE);
		projects.add(ProjectType.DESKTOP);
		projects.add(ProjectType.ANDROID);
		projects.add(ProjectType.IOS);
		projects.add(ProjectType.HTML);

		List<Dependency> dependencies = new ArrayList<Dependency>();
		dependencies.add(bank.getDependency(ProjectDependency.GDX));
		dependencies.add(bank.getDependency(ProjectDependency.FREETYPE));

		builder.buildProject(projects, dependencies);
		builder.build();
		new BladeEngineSetup().build(builder, projectDir + "/" + name, name, pkg,mainClass,
			sdk, null);
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
			
			
			// Add 'bin' dir from project directory to classpath so we can get custom actions desc and params
			// WARNING: Previous 'bin' folders are not deleted from the classpath
			// That can not be a problem if the package of the custom actions is different
			// in the loaded project.
			try {
				DinamicClassPath.addFile(projectFile.getAbsolutePath() + "/bin");
				DinamicClassPath.addFile(projectFile.getAbsolutePath() + "/out");
				DinamicClassPath.addFile(projectFile.getAbsolutePath() + "/desktop/build/classes/main");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
			
			world.setModelPath(projectFile.getAbsolutePath() + "/" + MODEL_PATH);
			world.load();
			selectedChapter = world.loadChapter(world.getInitChapter());
			editorConfig.setProperty(LAST_PROJECT_PROP, projectFile.getAbsolutePath());
						
			projectConfig = new Properties();
			projectConfig.load(new FileInputStream(projectFile.getAbsolutePath()+ ASSETS_PATH + "/" + Config.PROPERTIES_FILENAME));
			firePropertyChange(NOTIFY_PROJECT_LOADED);
		} else {
			this.projectFile = null;
			throw new IOException("Project not found.");
		}	
	}

	public boolean checkProjectStructure() {
		if (!new File(getModelPath()).exists()) {
			projectFile = projectFile.getParentFile();
			if (new File(getModelPath()).exists())
				return true;
			else
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

	public List<String> getResolutions() {
		File atlasesPath = new File(projectFile.getAbsolutePath() + ATLASES_PATH);
		ArrayList<String> l = new ArrayList<String>();

		File[] list = atlasesPath.listFiles();

		for (int i = 0; i < list.length; i++) {
			String name = list[i].getName();

			if (list[i].isDirectory()) {
				try {
					Float.parseFloat(name);
				
					l.add(name);
				} catch (Exception e) {
					
				}
			}
		}

		return l;
	}

	public String getResDir() {
		return "1";
	}

	public void setProjectProperty(String titleProp, String value) {
		projectConfig.setProperty(titleProp, value);
	}

	public String getProjectProperty(String titleProp, String def) {
		return projectConfig.getProperty(titleProp, def);
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
