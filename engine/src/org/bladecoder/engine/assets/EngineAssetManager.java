package org.bladecoder.engine.assets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;

public class EngineAssetManager {
	public static final String DESKTOP_PREFS_DIR = "BladeEngine";
	public static final String NOT_DESKTOP_PREFS_DIR = "data/";
	
	public static final String ATLASES_DIR = "atlases/";
	public static final String BACKGROUND_DIR = "backgrounds/";
	public static final String FONTS_DIR = "fonts/";
	public static final String MODEL_DIR = "model/";
	public static final String MUSIC_DIR = "music/";
	public static final String OVERLAY_DIR = "overlays/";
	public static final String SOUND_DIR = "sounds/";
	private static final String MODEL3D_DIR = "3d/";
	private static final String SPINE_DIR = "spine/";
	
	private static final String MODEL3D_EXT = ".g3db";
	private static final String SPINE_EXT = ".json";
	
	private static EngineAssetManager instance = null;

	private AssetManager manager = null;
	private float scale = 1;

	private EngineResolutionFileResolver resResolver;

	protected EngineAssetManager() {
		this(new InternalFileHandleResolver());
		manager.getLogger().setLevel(Application.LOG_DEBUG);
	}

	protected EngineAssetManager(FileHandleResolver resolver) {
		manager = new AssetManager(resolver);

		Resolution[] r = getResolutions(resolver);

		if (r == null || r.length == 0) {
			EngineLogger.error("No resolutions defined");
		}

		resResolver = new EngineResolutionFileResolver(resolver, r);
		manager.setLoader(Texture.class, new TextureLoader(resResolver));
		manager.setLoader(TextureAtlas.class, new TextureAtlasLoader(
				resResolver));
		Texture.setAssetManager(manager);

		Resolution choosed = EngineResolutionFileResolver.choose(r);

		EngineLogger.debug(	"Resolution choosed: " + choosed.suffix);
	}

	public float getScale() {
		return scale;
	}
	
	public void setScale(int worldWidth) {
		scale = resResolver.getResolution().portraitWidth / (float)worldWidth;
		
		EngineLogger.debug(	"Setting SCALE: " + scale);
	}

	public static EngineAssetManager getInstance() {
		if (instance == null) {
			instance = new EngineAssetManager();
		}

		return instance;
	}

	/**
	 * Creates a EngineAssetManager instance for edition. That is:
	 * 
	 * - Puts a PathResolver to locate the assets throws a absolute path - If
	 * reswidth > 0 puts reswidth as fixed resolution: - For testmode reswidth =
	 * 0 - For editmode reswidth = max resolution found
	 * 
	 * @param base
	 * @param resWidth
	 */
	public static void createEditInstance(String base, int resWidth) {
		if (instance != null)
			instance.dispose();

		instance = new EngineAssetManager(new BasePathResolver(base));

		if (resWidth > 0) {
			instance.forceResolution(resWidth);
		}
	}
	
	public void forceResolution(int resWidth) {
		resResolver.forceResolution(resWidth);
		
		EngineLogger.debug("FORCING RESOLUTION: " + resWidth);
	}

	public AssetManager getManager() {
		return manager;
	}

	public boolean isLoading() {
		return !manager.update();
	}

	public void loadAtlas(String name) {
		manager.load(ATLASES_DIR + name + ".atlas", TextureAtlas.class);
	}

	public boolean isAtlasLoaded(String name) {
		return manager.isLoaded(ATLASES_DIR + name + ".atlas");
	}

	public void disposeAtlas(String name) {
		if (isAtlasLoaded(name))
			manager.unload(ATLASES_DIR + name + ".atlas");
	}

	public FileHandle getModelFile(String filename) {
		return resResolver.baseResolve(MODEL_DIR + filename);
	}
	
	/**
	 * Returns a file in the asset directory SEARCHING in the resolution directories
	 * 
	 * @param filename
	 * @return
	 */
	public FileHandle getResAsset(String filename) {
		return resResolver.resolve(filename);
	}

	/**
	 * Returns a file in the asset directory without searching in the resolution directories
	 * 
	 * @param filename
	 * @return
	 */
	public FileHandle getAsset(String filename) {
		return resResolver.baseResolve(filename);
	}

	public Sprite getSprite(String atlas, String name) {
		TextureAtlas a = manager.get(ATLASES_DIR + atlas + ".atlas",
				TextureAtlas.class);

		Sprite s = a.createSprite(name);

		if (s == null) {
			EngineLogger.error("Sprite " + name + " not found in atlas "
					+ atlas);
		}

		return s;
	}

	public AtlasRegion getRegion(String atlas, String name) {
		TextureAtlas a = manager.get(ATLASES_DIR + atlas + ".atlas",
				TextureAtlas.class);

		AtlasRegion region = a.findRegion(name);

		if (region == null) {
			EngineLogger.error("Region " + name + " not found in atlas "
					+ atlas);
		}

		return region;
	}
	
	public TextureAtlas getTextureAtlas(String atlas) {
		TextureAtlas a = manager.get(ATLASES_DIR + atlas + ".atlas",
				TextureAtlas.class);

		return a;
	}

	public Array<AtlasRegion> getRegions(String atlas, String name) {
		TextureAtlas a = manager.get(ATLASES_DIR + atlas + ".atlas",
				TextureAtlas.class);

		Array<AtlasRegion> region = a.findRegions(name);

		if (region == null) {
			EngineLogger.error("Regions for " + name + " not found in atlas "
					+ atlas);
		}

		return region;
	}

	public void loadTexture(String filename) {
		manager.load(filename, Texture.class);
	}

	public void disposeTexture(Texture t) {
		if (manager.isLoaded(manager.getAssetFileName(t)))
			manager.unload(manager.getAssetFileName(t));
	}

	public Texture getTexture(String filename) {
		// TextureParameter param = new TextureParameter();
		// param.minFilter = TextureFilter.Linear;
		// param.genMipMaps = true;

		return manager.get(filename, Texture.class);
	}

	public void dispose() {
		manager.dispose();
		instance = null;
	}

	public void loadMusic(String filename) {
		manager.load(MUSIC_DIR + filename, Music.class);
	}

	public void disposeMusic(String filename) {
		if (manager.isLoaded(MUSIC_DIR + filename))
			manager.unload(MUSIC_DIR + filename);
	}

	public Music getMusic(String filename) {
		return manager.get(MUSIC_DIR + filename, Music.class);
	}

	public void loadSound(String filename) {
		manager.load(SOUND_DIR + filename, Sound.class);
	}

	public Sound getSound(String filename) {
		return manager.get(SOUND_DIR + filename, Sound.class);
	}

	public void disposeSound(String filename) {
		if (manager.isLoaded(SOUND_DIR + filename))
			manager.unload(SOUND_DIR + filename);
	}
	

	public FileHandle getSpine(String name) {
		return resResolver.baseResolve(SPINE_DIR + name + SPINE_EXT);
	}
	
	public void loadModel3D(String name) {
		manager.load(MODEL3D_DIR + name + MODEL3D_EXT, Model.class);
	}

	public Model getModel3D(String name) {
		return manager.get(MODEL3D_DIR + name + MODEL3D_EXT, Model.class);
	}
		

	public void disposeModel3D(String name) {
		if (manager.isLoaded(MODEL3D_DIR + name + MODEL3D_EXT))
			manager.unload(MODEL3D_DIR + name + MODEL3D_EXT);
	}	
	
	public Texture getOverlay(String filename) {
		Texture tex = new Texture(EngineAssetManager.getInstance().getResAsset(OVERLAY_DIR + filename));
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		
		return tex;
	}

	public <T> String getAssetFileName(T asset) {
		return manager.getAssetFileName(asset);
	}

	public boolean assetExists(String filename) {
		return resResolver.exists(filename);
	}
	
	private Resolution[] getResolutions(FileHandleResolver resolver) {
		ArrayList<Resolution> rl = new ArrayList<Resolution>();

		String n = "atlases";
		
		URL u = EngineAssetManager.class.getResource("/atlases");

		if (u != null
				&& u.getProtocol().equals("jar")) {
			String list[] = getResolutionsFromJar();
			
			for (String name : list) {
				if (name.contains("_")) {
					Resolution r = parseResolution(name);

					if (r != null)
						rl.add(r);
				}
			}
			
		} else {
			if (u != null)	n = u.getFile();

			FileHandle f = null;
			
			try {
				f = resolver.resolve(URLDecoder.decode( n, "UTF-8" ));
			} catch (UnsupportedEncodingException e) {
				EngineLogger.error("Error decoding URL", e);
				return rl.toArray(new Resolution[rl.size()]);
			}
			
			FileHandle[] list = f.list();

			for (FileHandle h : list) {
				String name = h.name();

				if (h.isDirectory() && name.contains("_")) {
					Resolution r = parseResolution(name);

					if (r != null)
						rl.add(r);
				}
			}
		}

		Collections.sort(rl, new Comparator<Resolution>() {
			public int compare(Resolution a, Resolution b) {
				return a.portraitWidth - b.portraitWidth;
			}
		});

		return rl.toArray(new Resolution[rl.size()]);
	}

	private Resolution parseResolution(String name) {
		String s[] = name.split("_");
		if (s.length < 2) {
			return null;
		}

		int width;
		int height;
		
		try{
			width = Integer.parseInt(s[0]);
			height = Integer.parseInt(s[1]);
		} catch(NumberFormatException e) {
			return null;
		}

		EngineLogger.debug(MessageFormat.format("New Resolution: {0}", name));

		return new Resolution(width, height, name);
	}	

	/**
	 * Returns the resolutions from a jar file.
	 */
	private String[] getResolutionsFromJar() {
		URL dirURL = EngineAssetManager.class.getResource("/atlases");
		
		Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory

		if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
			String jarPath = dirURL.getPath().substring(5,
					dirURL.getPath().indexOf("!")); // strip out only the JAR file
			
			JarFile jar;
			
			try {
				jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			} catch (Exception e) {
				EngineLogger.error("Locating jar file", e);
				return result.toArray(new String[result.size()]);
			}
			
			Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar 
			
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();			
				
				int start = name.indexOf('/');
				int end = name.lastIndexOf('/');
				
				if(start == end) continue;
				
				String entry = name.substring(start + 1, end);
				
				result.add(entry);
			}
			
			try {
				jar.close();
			} catch (IOException e) {
				EngineLogger.error("Closing jar file", e);
				return result.toArray(new String[result.size()]);
			}
			
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	public FileHandle getUserFile(String filename) {
		FileHandle file = null;
		
		if(Gdx.app.getType() == ApplicationType.Desktop||
				Gdx.app.getType() == ApplicationType.Applet) {
			String dir = Config.getProperty(Config.TITLE_PROP, DESKTOP_PREFS_DIR);
			dir.replaceAll(" ", "");
			
			StringBuilder sb = new StringBuilder();
			sb.append(".").append(dir).append("/").append(filename);
			file = Gdx.files.external(sb.toString());
		} else {
			file = Gdx.files.local(NOT_DESKTOP_PREFS_DIR + filename);
		}
		
		return file;
	}	
}
