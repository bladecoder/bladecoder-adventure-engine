package com.bladecoder.engine.serialization;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.PlaySoundAction;
import com.bladecoder.engine.actions.SoundAction;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.WorldProperties;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;

@SuppressWarnings("deprecation")
public class WorldSerialization implements Serializable {
	public static final String GAMESTATE_EXT = ".gamestate.v13";

	private static final int SCREENSHOT_DEFAULT_WIDTH = 300;

	private final World w;

	public WorldSerialization(World w) {
		this.w = w;
	}

	/**
	 * Load the world description in 'world.json'.
	 * 
	 * @throws IOException
	 */
	public void loadWorldDesc() throws IOException {

		String worldFilename = EngineAssetManager.WORLD_FILENAME;

		if (!EngineAssetManager.getInstance().getModelFile(worldFilename).exists()) {

			// Search the world file with ".json" ext if not found.
			worldFilename = EngineAssetManager.WORLD_FILENAME + ".json";

			if (!EngineAssetManager.getInstance().getModelFile(worldFilename).exists()) {
				EngineLogger.error("ERROR LOADING WORLD: world file not found.");
				w.dispose();
				throw new IOException("ERROR LOADING WORLD: world file not found.");
			}
		}

		JsonValue root = new JsonReader()
				.parse(EngineAssetManager.getInstance().getModelFile(worldFilename).reader("UTF-8"));

		Json json = new BladeJson(w, Mode.MODEL);
		json.setIgnoreUnknownFields(true);

		int width = json.readValue("width", Integer.class, root);
		int height = json.readValue("height", Integer.class, root);

		// We know the world width, so we can set the scale
		EngineAssetManager.getInstance().setScale(width, height);
		float scale = EngineAssetManager.getInstance().getScale();

		w.setWidth((int) (width * scale));
		w.setHeight((int) (height * scale));
		w.setInitChapter(json.readValue("initChapter", String.class, root));
		w.getVerbManager().read(json, root);
		I18N.loadWorld(EngineAssetManager.MODEL_DIR + EngineAssetManager.WORLD_FILENAME);
	}

	public void saveWorldDesc(FileHandle file) throws IOException {

		float scale = EngineAssetManager.getInstance().getScale();

		Json json = new BladeJson(w, Mode.MODEL);
		json.setOutputType(OutputType.javascript);

		json.setWriter(new StringWriter());

		json.writeObjectStart();
		json.writeValue("width", w.getWidth() / scale);
		json.writeValue("height", w.getHeight() / scale);
		json.writeValue("initChapter", w.getInitChapter());
		w.getVerbManager().write(json);
		json.writeObjectEnd();

		String s = null;

		if (EngineLogger.debugMode())
			s = json.prettyPrint(json.getWriter().getWriter().toString());
		else
			s = json.getWriter().getWriter().toString();

		Writer w = file.writer(false, "UTF-8");
		w.write(s);
		w.close();
	}

	public void loadChapter() throws IOException {
		loadChapter(null, null, true);
	}

	/**
	 * Loads a JSON chapter file.
	 * 
	 * @param chapterName filename without path and extension.
	 * @param scene       the init scene. null to use the chapter defined init
	 *                    scene.
	 * @param initScene   false only when it comes from loading a saved game.
	 * @throws IOException
	 */
	public void loadChapter(String chapterName, String scene, boolean initScene) throws IOException {
		if (!w.isDisposed())
			w.dispose();

		long initTime = System.currentTimeMillis();

		if (chapterName == null)
			chapterName = w.getInitChapter();

		w.setChapter(chapterName);

		if (EngineAssetManager.getInstance().getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).exists()) {

			JsonValue root = new JsonReader().parse(EngineAssetManager.getInstance()
					.getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).reader("UTF-8"));

			Json json = new BladeJson(w, Mode.MODEL, initScene);
			json.setIgnoreUnknownFields(true);

			read(json, root);

			if (scene == null)
				w.setCurrentScene(w.getScenes().get(w.getInitScene()), initScene);
			else
				w.setCurrentScene(w.getScenes().get(scene), initScene);

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + chapterName);

			w.getCustomProperties().put(WorldProperties.CURRENT_CHAPTER.toString(), chapterName);
			w.getCustomProperties().put(WorldProperties.PLATFORM.toString(), Gdx.app.getType().toString());
		} else {
			EngineLogger.error(
					"ERROR LOADING CHAPTER: " + chapterName + EngineAssetManager.CHAPTER_EXT + " doesn't exists.");
			w.dispose();
			throw new IOException(
					"ERROR LOADING CHAPTER: " + chapterName + EngineAssetManager.CHAPTER_EXT + " doesn't exists.");
		}

		EngineLogger.debug("MODEL LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void saveModel(String chapterId) throws IOException {
		EngineLogger.debug("SAVING GAME MODEL");

		if (w.isDisposed())
			return;

		Json json = new BladeJson(w, Mode.MODEL);
		json.setOutputType(OutputType.javascript);

		String s = null;

		if (EngineLogger.debugMode())
			s = json.prettyPrint(this);
		else
			s = json.toJson(this);

		Writer w = EngineAssetManager.getInstance().getModelFile(chapterId + EngineAssetManager.CHAPTER_EXT)
				.writer(false, "UTF-8");

		try {
			w.write(s);
			w.flush();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING MODEL", e);
		} finally {
			w.close();
		}
	}

	public void loadGameState(FileHandle savedFile) throws IOException {
		EngineLogger.debug("LOADING GAME STATE");

		if (savedFile.exists()) {

			JsonValue root = new JsonReader().parse(savedFile.reader("UTF-8"));

			Json json = new BladeJson(w, Mode.STATE);
			json.setIgnoreUnknownFields(true);

			read(json, root);

		} else {
			throw new IOException("LOADGAMESTATE: no saved game exists");
		}
	}

	public void saveGameState(String filename, boolean screenshot) throws IOException {
		EngineLogger.debug("SAVING GAME STATE");

		if (w.isDisposed())
			return;

		Json json = new BladeJson(w, Mode.STATE);
		json.setOutputType(OutputType.javascript);

		String s = null;

		if (EngineLogger.debugMode())
			s = json.prettyPrint(this);
		else
			s = json.toJson(this);

		Writer writer = EngineAssetManager.getInstance().getUserFile(filename).writer(false, "UTF-8");

		try {
			writer.write(s);
			writer.flush();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING GAME", e);
		} finally {
			writer.close();
		}

		// Save Screenshot
		if (screenshot)
			w.takeScreenshot(filename + ".png", SCREENSHOT_DEFAULT_WIDTH);
	}

	@Override
	public void write(Json json) {
		BladeJson bjson = (BladeJson) json;

		json.writeValue(Config.BLADE_ENGINE_VERSION_PROP, Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, null));

		if (bjson.getMode() == Mode.MODEL) {
			json.writeValue("sounds", w.getSounds(), w.getSounds().getClass(), SoundDesc.class);
			json.writeValue("scenes", w.getScenes(), w.getScenes().getClass(), Scene.class);
			json.writeValue("initScene", w.getInitScene());

		} else {
			json.writeValue(Config.VERSION_PROP, Config.getProperty(Config.VERSION_PROP, null));
			json.writeValue("scenes", w.getScenes(), w.getScenes().getClass(), Scene.class);
			json.writeValue("currentScene", w.getCurrentScene().getId());
			json.writeValue("inventories", w.getInventories());
			json.writeValue("currentInventory", w.getCurrentInventory());
			json.writeValue("timeOfGame", w.getTimeOfGame());
			json.writeValue("cutmode", w.inCutMode());
			w.getVerbManager().write(json);
			json.writeValue("customProperties", w.getCustomProperties(), w.getCustomProperties().getClass(),
					String.class);

			if (w.getCurrentDialog() != null) {
				json.writeValue("dialogActor", w.getCurrentDialog().getActor().getId());
				json.writeValue("currentDialog", w.getCurrentDialog().getId());
			}

			if (w.getTransition() != null)
				json.writeValue("transition", w.getTransition());

			json.writeValue("chapter", w.getCurrentChapter());
			json.writeValue("musicEngine", w.getMusicManager());

			if (!w.getUIActors().getActors().isEmpty())
				json.writeValue("uiActors", w.getUIActors());
		}

		if (w.getInkManager() != null && w.getInkManager().getStoryName() != null)
			json.writeValue("inkManager", w.getInkManager());
	}

	@Override
	public void read(Json json, JsonValue jsonData) {

		String bladeVersion = json.readValue(Config.BLADE_ENGINE_VERSION_PROP, String.class, jsonData);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			if (bladeVersion != null
					&& !bladeVersion.equals(Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""))) {
				EngineLogger.debug("Model Engine Version v" + bladeVersion + " differs from Current Engine Version v"
						+ Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""));
			}

			// SOUNDS
			JsonValue jsonSounds = jsonData.get("sounds");
			HashMap<String, SoundDesc> sounds = w.getSounds();

			if (jsonSounds != null) {
				for (int i = 0; i < jsonSounds.size; i++) {
					JsonValue jsonValue = jsonSounds.get(i);
					SoundDesc s = json.readValue(SoundDesc.class, jsonValue);
					sounds.put(jsonValue.name, s);
				}
			}

			// SCENES
			JsonValue jsonScenes = jsonData.get("scenes");
			Map<String, Scene> scenes = w.getScenes();

			for (int i = 0; i < jsonScenes.size; i++) {
				JsonValue jsonValue = jsonScenes.get(i);
				Scene s = new Scene(w);
				scenes.put(jsonValue.name, s);
				s.read(json, jsonValue);
			}

			w.setInitScene(json.readValue("initScene", String.class, jsonData));

			if (w.getInitScene() == null && w.getScenes().size() > 0) {
				w.setInitScene(w.getScenes().keySet().toArray(new String[0])[0]);
			}

			for (Scene s : w.getScenes().values()) {
				s.resetCamera(w.getWidth(), w.getHeight());
			}

			// Load Ink story
			if (jsonData.get("inkManager") != null) {
				w.getInkManager().read(json, jsonData.get("inkManager"));
			}

			// Add sounds to cache
			cacheSounds();
		} else {
			if (bladeVersion != null
					&& !bladeVersion.equals(Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""))) {
				EngineLogger
						.debug("Saved Game Engine Version v" + bladeVersion + " differs from Current Engine Version v"
								+ Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""));
			}

			String currentChapter = json.readValue("chapter", String.class, jsonData);
			String currentScene = json.readValue("currentScene", String.class, jsonData);

			try {
				loadChapter(currentChapter, currentScene, false);
			} catch (IOException e1) {
				EngineLogger.error("Error Loading Chapter");
				return;
			}

			// read inkManager after setting he current scene but before reading
			// scenes and verbs tweens
			if (jsonData.get("inkManager") != null) {
				w.getInkManager().read(json, jsonData.get("inkManager"));
			}

			// inventories have to be put in the hash to find the actors when
			// reading saved data
			w.setCurrentInventory(json.readValue("currentInventory", String.class, jsonData));

			JsonValue jsonInventories = jsonData.get("inventories");

			for (int i = 0; i < jsonInventories.size; i++) {
				JsonValue jsonValue = jsonInventories.get(i);
				Inventory inv = new Inventory();
				w.getInventories().put(jsonValue.name, inv);
				inv.read(json, jsonValue);
			}

			if (jsonData.get("uiActors") != null) {
				w.getUIActors().read(json, jsonData.get("uiActors"));
			}

			for (Scene s : w.getScenes().values()) {
				JsonValue jsonValue = jsonData.get("scenes").get(s.getId());

				if (jsonValue != null)
					s.read(json, jsonValue);
				else
					EngineLogger.debug("LOAD WARNING: Scene not found in saved game: " + s.getId());
			}

			w.setTimeOfGame(json.readValue("timeOfGame", long.class, 0L, jsonData));
			w.setCutMode(json.readValue("cutmode", boolean.class, false, jsonData));

			w.getVerbManager().read(json, jsonData);

			// CUSTOM PROPERTIES
			JsonValue jsonProperties = jsonData.get("customProperties");
			HashMap<String, String> props = w.getCustomProperties();

			for (int i = 0; i < jsonProperties.size; i++) {
				JsonValue jsonValue = jsonProperties.get(i);
				props.put(jsonValue.name, jsonValue.asString());
			}

			String version = json.readValue(Config.VERSION_PROP, String.class, jsonData);
			if (version == null)
				version = "TEST";
			props.put(WorldProperties.SAVED_GAME_VERSION.toString(), version);

			String actorId = json.readValue("dialogActor", String.class, jsonData);
			String dialogId = json.readValue("currentDialog", String.class, jsonData);

			if (dialogId != null) {
				CharacterActor actor = (CharacterActor) w.getCurrentScene().getActor(actorId, false);
				w.setCurrentDialog(actor.getDialog(dialogId));
			}

			w.getTransition().read(json, jsonData.get("transition"));
			w.getMusicManager().read(json, jsonData.get("musicEngine"));

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + w.getCurrentChapter());
		}
	}

	private void cacheSounds() {
		for (Scene s : w.getScenes().values()) {

			HashMap<String, Verb> verbs = s.getVerbManager().getVerbs();

			// Search SoundAction and PlaySoundAction
			for (Verb v : verbs.values()) {
				ArrayList<Action> actions = v.getActions();

				for (int i = 0; i < actions.size(); i++) {

					Action act = actions.get(i);

					try {
						if (act instanceof SoundAction) {

							String actor = ActionUtils.getStringValue(act, "actor");
							String play = ActionUtils.getStringValue(act, "play");
							if (play != null) {
								SoundDesc sd = w.getSounds().get(actor + "_" + play);

								if (sd != null)
									s.getSoundManager().addSoundToLoad(sd);

								HashMap<String, String> params = new HashMap<>();
								params.put("sound", sd.getId());

								try {
									Action a2 = ActionFactory.createByClass(PlaySoundAction.class.getName(), params);
									actions.set(i, a2);
									a2.init(w);
								} catch (ClassNotFoundException | ReflectionException e) {
									e.printStackTrace();
								}
								EngineLogger.debug("Converting SoundAction:" + s.getId() + "." + v.getId());
							} else {
								EngineLogger
										.debug("WARNING: Cannot convert SoundAction:" + s.getId() + "." + v.getId());
							}

						} else if (act instanceof PlaySoundAction) {
							String sound = ActionUtils.getStringValue(act, "sound");
							SoundDesc sd = w.getSounds().get(sound);

							if (sd != null && sd.isPreload())
								s.getSoundManager().addSoundToLoad(sd);

						}
					} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					}
				}
			}

			for (BaseActor a : s.getActors().values()) {

				if (a instanceof InteractiveActor) {
					HashMap<String, Verb> actorVerbs = ((InteractiveActor) a).getVerbManager().getVerbs();

					for (Verb v : actorVerbs.values()) {
						ArrayList<Action> actions = v.getActions();

						for (int i = 0; i < actions.size(); i++) {

							Action act = actions.get(i);

							try {
								if (act instanceof SoundAction) {

									String actor = ActionUtils.getStringValue(act, "actor");
									String play = ActionUtils.getStringValue(act, "play");
									if (play != null) {
										SoundDesc sd = w.getSounds().get(actor + "_" + play);

										if (sd != null)
											s.getSoundManager().addSoundToLoad(sd);

										HashMap<String, String> params = new HashMap<>();
										params.put("sound", sd.getId());

										try {
											Action a2 = ActionFactory.createByClass(PlaySoundAction.class.getName(),
													params);
											actions.set(i, a2);
											a2.init(w);
										} catch (ClassNotFoundException | ReflectionException e) {
											e.printStackTrace();
										}
										EngineLogger.debug("Converting SoundAction in:" + s.getId() + "." + a.getId()
												+ "." + v.getId());
									} else {
										EngineLogger.debug("WARNING: Cannot convert SoundAction:" + s.getId() + "."
												+ a.getId() + "." + v.getId());
									}

								} else if (act instanceof PlaySoundAction) {
									String sound = ActionUtils.getStringValue(act, "sound");
									SoundDesc sd = w.getSounds().get(sound);

									if (sd != null && sd.isPreload())
										s.getSoundManager().addSoundToLoad(sd);

								}
							} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
							}
						}
					}
				}

				if (a instanceof SpriteActor && ((SpriteActor) a).getRenderer() instanceof AnimationRenderer) {
					HashMap<String, AnimationDesc> anims = ((AnimationRenderer) ((SpriteActor) a).getRenderer())
							.getAnimations();

					for (AnimationDesc ad : anims.values()) {
						if (ad.sound != null) {
							String sid = ad.sound;

							SoundDesc sd = w.getSounds().get(sid);

							if (sd == null)
								sid = a.getId() + "_" + sid;

							sd = w.getSounds().get(sid);

							if (sd != null) {
								if (sd.isPreload())
									s.getSoundManager().addSoundToLoad(sd);
							} else
								EngineLogger.error(
										a.getId() + ": SOUND not found: " + ad.sound + " in animation: " + ad.id);
						}
					}
				}

			}
		}

	}
}
