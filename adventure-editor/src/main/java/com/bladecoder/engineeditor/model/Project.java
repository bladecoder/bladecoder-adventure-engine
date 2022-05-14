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
package com.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.SerializationException;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.FolderClassLoader;
import com.bladecoder.engineeditor.common.OrderedProperties;
import com.bladecoder.engineeditor.common.OrderedProperties.OrderedPropertiesBuilder;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.common.Versions;
import com.bladecoder.engineeditor.setup.BladeEngineSetup;
import com.bladecoder.engineeditor.undo.UndoStack;

public class Project extends PropertyChange {
	public static final String PROP_PROJECTFILE = "projectFile";
	public static final String NOTIFY_SCENE_SELECTED = "SCENE_SELECTED";
	public static final String NOTIFY_ACTOR_SELECTED = "ACTOR_SELECTED";
	public static final String NOTIFY_ANIM_SELECTED = "ANIM_SELECTED";
	public static final String NOTIFY_VERB_SELECTED = "VERB_SELECTED";
	public static final String NOTIFY_PROJECT_LOADED = "PROJECT_LOADED";
	public static final String NOTIFY_PROJECT_SAVED = "PROJECT_SAVED";
	public static final String NOTIFY_CHAPTER_LOADED = "CHAPTER_LOADED";

	public static final String NOTIFY_ELEMENT_DELETED = "ELEMENT_DELETED";
	public static final String NOTIFY_ELEMENT_CREATED = "ELEMENT_CREATED";
	public static final String NOTIFY_MODEL_MODIFIED = "MODEL_MODIFIED";
	public static final String POSITION_PROPERTY = "pos";
	public static final String WIDTH_PROPERTY = "width";
	public static final String HEIGHT_PROPERTY = "height";
	public static final String CHAPTER_PROPERTY = "chapter";

	public static final String SPINE_RENDERER_STRING = "spine";
	public static final String ATLAS_RENDERER_STRING = "atlas";
	public static final String IMAGE_RENDERER_STRING = "image";
	public static final String S3D_RENDERER_STRING = "3d";
	public static final String PARTICLE_RENDERER_STRING = "particle";
	public static final String TEXT_RENDERER_STRING = "text";

	public static final String ASSETS_PATH = "/assets";
	public static final String MODEL_PATH = "/model";
	public static final String ATLASES_PATH = "/atlases";
	public static final String FONTS_PATH = "/fonts";
	public static final String MUSIC_PATH = "/music";
	public static final String SOUND_PATH = "/sounds";
	public static final String IMAGE_PATH = "/images";
	public static final String SPRITE3D_PATH = "/3d";
	public static final String SPINE_PATH = "/spine";
	public static final String PARTICLE_PATH = "/particles";
	public static final String VOICE_PATH = "/voices";
	public static final String UI_PATH = "/ui";
	public static final String FONT_PATH = UI_PATH + "/fonts";

	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;

	private static final String CONFIG_DIR = System.getProperty("user.home") + "/.AdventureEditor";
	private static final String CONFIG_FILENAME = "config.properties";

	public static final String LAST_PROJECT_PROP = "last_project";

	private final Properties editorConfig = new Properties();

	private File projectFile;

	private final UndoStack undoStack = new UndoStack();
	private OrderedProperties projectConfig;

	private I18NHandler i18n;
	private Chapter chapter;
	private Scene selectedScene;
	private BaseActor selectedActor;
	private String selectedFA;
	private boolean modified = false;
	private final World world = new World();
	private final HashSet<String> hidenActors = new HashSet<>();

	public Project() {
		loadConfig();
	}

	public World getWorld() {
		return world;
	}

	public String getAssetPath(String base) {
		return base + ASSETS_PATH;
	}

	public String getAssetPath() {
		return getAssetPath(getProjectPath());
	}

	public UndoStack getUndoStack() {
		return undoStack;
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

	public Properties getEditorConfig() {
		return editorConfig;
	}

	public OrderedProperties getProjectConfig() {
		return projectConfig;
	}

	public I18NHandler getI18N() {
		return i18n;
	}

	public String translate(String key) {
		return i18n.getTranslation(key);
	}

	public void setModified(Object source, String property, Object oldValue, Object newValue) {
		modified = true;
		PropertyChangeEvent evt = new PropertyChangeEvent(source, property, oldValue, newValue);
		firePropertyChange(evt);
	}

	public void notifyPropertyChange(String property) {
		firePropertyChange(property);
	}

	public void setSelectedScene(Scene scn) {
		selectedScene = scn;
		selectedActor = null;
		selectedFA = null;

		if (scn != null)
			getEditorConfig().setProperty("project.selectedScene", scn.getId());

		firePropertyChange(NOTIFY_SCENE_SELECTED, null, selectedScene);
	}

	public void setSelectedActor(BaseActor a) {
		BaseActor old = null;

		old = selectedActor;

		selectedActor = a;
		selectedFA = null;

		firePropertyChange(NOTIFY_ACTOR_SELECTED, old, selectedActor);
	}

	public Chapter getChapter() {
		return chapter;
	}

	public Scene getSelectedScene() {
		return selectedScene;
	}

	public BaseActor getSelectedActor() {
		return selectedActor;
	}

	public String getSelectedFA() {
		return selectedFA;
	}

	public void setSelectedFA(String id) {
		String old = selectedFA;

		selectedFA = id;

		firePropertyChange(NOTIFY_ANIM_SELECTED, old, selectedFA);
	}

	public String getModelPath() {
		return getAssetPath() + MODEL_PATH;
	}

	public String getProjectPath() {
		return projectFile.getAbsolutePath();
	}

	public File getProjectDir() {
		return projectFile;
	}

	public String getTitle() {
		if (projectConfig == null)
			return null;

		return projectConfig.getProperty(Config.TITLE_PROP, getProjectDir().getName());
	}

	public boolean isLoaded() {
		return Ctx.project.getProjectDir() != null;
	}

	public String getPackageTitle() {
		return getTitle().replace(" ", "").replace("'", "");
	}

	public void createProject(String projectDir, String name, String pkg, String sdkLocation, boolean spinePlugin)
			throws IOException {
		closeProject();

		createLibGdxProject(projectDir, name, pkg, "BladeEngine", sdkLocation, spinePlugin);

		projectFile = new File(projectDir + "/" + name);

		loadProject(projectFile);
	}

	private void createLibGdxProject(String projectDir, String name, String pkg, String mainClass, String sdkLocation,
			boolean spinePlugin) throws IOException {
		String sdk = null;

		if (sdkLocation != null && !sdkLocation.isEmpty()) {
			sdk = sdkLocation;
		} else if (System.getenv("ANDROID_HOME") != null) {
			sdk = System.getenv("ANDROID_HOME");
		}

		new BladeEngineSetup().build(projectDir + "/" + name, name, pkg, mainClass, sdk, spinePlugin);
	}

	public void saveProject() throws IOException {
		if (projectFile != null && chapter.getId() != null && modified) {

			EngineLogger.setDebug();

			// 1.- SAVE world
			world.saveWorldDesc(
					new FileHandle(new File(getAssetPath() + MODEL_PATH + "/" + EngineAssetManager.WORLD_FILENAME)));

			// 2.- SAVE .chapter
			chapter.save();

			// 3.- SAVE BladeEngine.properties
			List<String> resolutions = getResolutions();
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < resolutions.size(); i++) {
				sb.append(resolutions.get(i));

				if (i < resolutions.size() - 1)
					sb.append(',');
			}

			projectConfig.setProperty(Config.RESOLUTIONS, sb.toString());
			projectConfig.store(new FileOutputStream(getAssetPath() + "/" + Config.PROPERTIES_FILENAME), null);

			// 4.- SAVE I18N
			i18n.save();

			modified = false;
			firePropertyChange(NOTIFY_PROJECT_SAVED);
		}
	}

	public void closeProject() {
		setSelectedScene(null);
		this.projectFile = null;
		this.projectConfig = null;
		firePropertyChange(NOTIFY_PROJECT_LOADED);
	}

	public void loadProject(File projectToLoad) throws IOException {

		projectToLoad = checkProjectStructure(projectToLoad);

		if (projectToLoad != null) {
			// dispose the current project
			closeProject();

			this.projectFile = projectToLoad;

			// Use FolderClassLoader for loading CUSTOM actions.
			// TODO Add 'core/bin' and '/core/out' folders???
			FolderClassLoader folderClassLoader = null;

			if (new File(projectFile, "/assets").exists()) {
				folderClassLoader = new FolderClassLoader(
						projectFile.getAbsolutePath() + "/core/build/classes/java/main");
			} else {
				folderClassLoader = new FolderClassLoader(projectFile.getAbsolutePath() + "/core/build/classes/main");
			}

			ActionFactory.setActionClassLoader(folderClassLoader);
			EngineAssetManager.createEditInstance(getAssetPath());

			try {
				// Clear last project to avoid reloading if the project fails.
				getEditorConfig().remove(LAST_PROJECT_PROP);
				saveConfig();

				world.loadWorldDesc();
			} catch (SerializationException ex) {
				// check for not compiled custom actions
				if (ex.getCause() != null && ex.getCause() instanceof ClassNotFoundException) {
					EditorLogger.msg("Custom action class not found. Trying to compile...");
					if (RunProccess.runGradle(getProjectDir(), "desktop:compileJava")) {
						folderClassLoader.reload();
						world.loadWorldDesc();
					} else {
						this.projectFile = null;
						throw new IOException("Failed to run Gradle.");
					}
				} else {
					this.projectFile = null;
					throw ex;
				}
			}

			chapter = new Chapter(getAssetPath() + Project.MODEL_PATH);
			i18n = new I18NHandler(getAssetPath() + Project.MODEL_PATH);

			// No need to load the chapter. It's loaded by the chapter combo.
			// loadChapter(world.getInitChapter());

			projectConfig = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrderingCaseSensitive()
					.build();
			projectConfig.load(new FileInputStream(getAssetPath() + "/" + Config.PROPERTIES_FILENAME));
			modified = false;

			Lwjgl3Window window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
			window.setTitle("Adventure Editor v" + Versions.getVersion() + " - " + projectFile.getAbsolutePath());

			firePropertyChange(NOTIFY_PROJECT_LOADED);
		} else {
			closeProject();
			throw new IOException("Project not found.");
		}
	}

	public boolean checkVersion(File projectPath) throws FileNotFoundException, IOException {
		String editorVersion = getEditorBladeEngineVersion();
		String projectVersion = getProjectBladeEngineVersion(projectPath);

		if (editorVersion.equals(projectVersion) || editorVersion.indexOf('.') == -1)
			return true;

		if (parseVersion(editorVersion) <= parseVersion(projectVersion))
			return true;

		return false;
	}

	private int parseVersion(String v) {
		int number = 1; // 1 -> release, 0 -> snapshot

		if (v.endsWith("-SNAPSHOT")) {
			number = 0;
			v = v.substring(0, v.length() - "-SNAPSHOT".length());
		}

		String[] split = v.split("\\.");

		try {
			for (int i = 0; i < split.length; i++) {
				number += Math.pow(10, (split.length - i) * 2) * Integer.parseInt(split[i]);
			}
		} catch (NumberFormatException e) {
		}

		return number;
	}

	public String getProjectBladeEngineVersion(File projectPath) throws FileNotFoundException, IOException {
		OrderedProperties properties = getGradleProperties(projectPath);

		return properties.getProperty(Config.BLADE_ENGINE_VERSION_PROP, "default");
	}

	public String getEditorBladeEngineVersion() {
		return Versions.getVersion();
	}

	public void updateEngineVersion(File projectPath) throws FileNotFoundException, IOException {
		OrderedProperties prop = getGradleProperties(projectPath);

		prop.setProperty(Config.BLADE_ENGINE_VERSION_PROP, Versions.getVersion());
		prop.setProperty("gdxVersion", Versions.getLibgdxVersion());
		prop.setProperty("roboVMVersion", Versions.getRoboVMVersion());

		prop.setProperty("roboVMGradlePluginVersion", Versions.getROBOVMGradlePluginVersion());
		prop.setProperty("androidGradlePluginVersion", Versions.getAndroidGradlePluginVersion());
		prop.setProperty("bladeInkVersion", Versions.getBladeInkVersion());

		saveGradleProperties(prop, projectPath);
	}

	/**
	 * Checks if the model folder exists in the passed folder or in his parent.
	 * 
	 * @return The correct project folder or null if the model folder is not found.
	 */
	private File checkProjectStructure(File folder) {
		File projectFolder = folder;

		if (!new File(getAssetPath(projectFolder.getAbsolutePath()) + MODEL_PATH).exists()) {
			projectFolder = projectFolder.getParentFile();

			if (!new File(getAssetPath(projectFolder.getAbsolutePath()) + MODEL_PATH).exists())
				return null;
		}

		return projectFolder;
	}

	public BaseActor getActor(String id) {
		return selectedScene.getActor(id, false);
	}

	public List<String> getResolutions() {
		File atlasesPath = new File(getAssetPath() + UI_PATH);
		ArrayList<String> l = new ArrayList<>();

		File[] list = atlasesPath.listFiles();

		if (list == null)
			return l;

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

	public void loadChapter(String selChapter) throws IOException {
		undoStack.clear();

		setSelectedScene(null);

		try {
			chapter.load(selChapter);
			firePropertyChange(NOTIFY_CHAPTER_LOADED);
			getEditorConfig().setProperty(LAST_PROJECT_PROP, projectFile.getAbsolutePath());
			getEditorConfig().setProperty("project.selectedChapter", selChapter);
		} catch (SerializationException ex) {
			// check for not compiled custom actions
			if (ex.getCause() != null && ex.getCause() instanceof ClassNotFoundException) {
				EditorLogger.msg("Custom action class not found. Trying to compile...");
				if (RunProccess.runGradle(getProjectDir(), "desktop:compileJava")) {
					((FolderClassLoader) ActionFactory.getActionClassLoader()).reload();
					chapter.load(selChapter);
				} else {
					throw new IOException("Failed to run Gradle.");
				}
			} else {
				throw ex;
			}
		}

		i18n.load(selChapter);
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified() {
		modified = true;
		firePropertyChange(NOTIFY_MODEL_MODIFIED);
	}

	public OrderedProperties getGradleProperties(File projectPath) throws FileNotFoundException, IOException {
		OrderedProperties prop = new OrderedPropertiesBuilder().withSuppressDateInComment(true)
				.withOrderingCaseSensitive().build();

		prop.load(new FileReader(projectPath.getAbsolutePath() + "/gradle.properties"));

		return prop;
	}

	public void saveGradleProperties(OrderedProperties prop, File projectPath) throws IOException {
		FileOutputStream os = new FileOutputStream(projectPath.getAbsolutePath() + "/gradle.properties");

		prop.store(os, null);
	}

	public void toggleEditorVisibility(BaseActor a) {
		String name = a.getInitScene() + "." + a.getId();

		if (hidenActors.contains(name)) {
			hidenActors.remove(name);
		} else {
			hidenActors.add(name);
		}
	}

	public boolean isEditorVisible(BaseActor a) {
		String name = a.getInitScene() + "." + a.getId();

		return !hidenActors.contains(name);
	}
}
