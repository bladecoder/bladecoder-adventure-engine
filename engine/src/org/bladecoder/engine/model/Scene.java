package org.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.pathfinder.Movers;
import org.bladecoder.engine.pathfinder.PixTileMap;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Scene extends Actor implements Movers, Serializable,
		AssetConsumer {
	
	private static final Color ACTOR_BBOX_COLOR = new Color(0.2f, 0.2f, 0.8f,
			1f);
	private static final String MAP_FILE_EXTENSION = ".map.png";
	private static final TextureFilter BG_TEXFILTER_MAG = TextureFilter.Linear;
	private static final TextureFilter BG_TEXFILTER_MIN = TextureFilter.Linear;

	/** 
	 * All actors in the scene
	 */
	private HashMap<String, Actor> actors = new HashMap<String, Actor>();

	/**
	 * Foreground actors. Non interactive. Always draw this actors in the
	 * foreground
	 */
	private ArrayList<SpriteActor> fgActors = new ArrayList<SpriteActor>();

	/**
	 * Temp list with the 'actors' list + player ordered by 'y' axis to draw in
	 * depth order and to check for click
	 */
	private List<Actor> orderedActors = new ArrayList<Actor>();
	
	private ArrayList<String> preloadedAtlases = new ArrayList<String>();
	private SceneCamera camera = new SceneCamera();
	
	private Texture[] background;
	private Texture[] lightMap;
	private String backgroundFilename;
	private String lightMapFilename;

	/** background tile map for pathfinding */
	private PixTileMap backgroundMap;
	
	/** depth vector. x: scale when y=0, y: scale when y=scene height */
	private Vector2 depthVector;

	/** Overlay image drew over the scene */
	private OverlayImage overlay;

	/** For FADEIN/FADEOUT */
	private Transition transition;

	private String player;
	
	/** The actor the camera will follow */
	private SpriteActor followActor;

	private Music music = null;
	private boolean loopMusic = false;
	private float repeatMusicDelay = 0;
	private float initialMusicDelay = 0;
	private float currentMusicDelay = 0;

	private String musicFilename;
	private boolean isPlayingSer = false;

	transient private boolean isMusicPaused = false;

	public Scene() {	
	}

	public void playMusic() {
		if (music != null && !music.isPlaying()) {
			music.play();
			music.setLooping(loopMusic);
		}
	}

	public void pauseMusic() {
		if (music != null && music.isPlaying()) {
			music.pause();
			isMusicPaused = true;
		}
	}

	public void resumeMusic() {
		if (music != null && isMusicPaused) {
			music.play();
			isMusicPaused = false;
		}
	}

	public void stopMusic() {
		if (music != null)
			music.stop();
	}

	public void setMusic(String filename, boolean loop, float initialDelay,
			float repeatDelay) {
		loopMusic = loop;
		musicFilename = filename;
		initialMusicDelay = initialDelay;
		repeatMusicDelay = repeatDelay;
	}

	public void setAtlases(String atlases) {
		String[] list = atlases.split(",");

		for (String a : list) {
			if (!a.trim().isEmpty())
				this.preloadedAtlases.add(a.trim());
		}
	}

	/**
	 * Method to support ResourceAction: dynamic atlas load/unload
	 * 
	 * @param atlas
	 */
	public void addAtlas(String atlas) {
		preloadedAtlases.add(atlas);
	}

	/**
	 * Method to support ResourceAction: dynamic atlas load/unload
	 * 
	 * @param atlas
	 */
	public void removeAtlas(String atlas) {
		for (int i = 0; i < preloadedAtlases.size(); i++) {
			String a = preloadedAtlases.get(i);

			if (a.equals(atlas)) {
				preloadedAtlases.remove(i);
				break;
			}
		}
	}

	public void update(float delta) {
		// We draw the elements in order: from top to bottom.
		// so we need to order the array list
		Collections.sort(orderedActors);

		if (overlay != null) {
			overlay.update(delta);

			if (overlay.isFinish()) {
				overlay.dispose();
				overlay = null;
			}
		}

		if (transition != null) {
			transition.update(delta);

			if (transition.isFinish()) {
				transition = null;
			}
		}

		// music delay update
		if (music != null && !music.isPlaying()) {
			boolean initialTime = false;

			if (currentMusicDelay <= initialMusicDelay)
				initialTime = true;

			currentMusicDelay += delta;

			if (initialTime) {
				if (currentMusicDelay > initialMusicDelay)
					playMusic();
			} else {
				if (repeatMusicDelay >= 0
						&& currentMusicDelay > repeatMusicDelay
								+ initialMusicDelay) {
					currentMusicDelay = initialMusicDelay;
					playMusic();
				}
			}
		}

		for (Actor a : orderedActors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).update(delta);
		}
	}

	public void draw(SpriteBatch spriteBatch) {
		
		if (background != null) {
			spriteBatch.disableBlending();

			float x = 0;

			for (Texture tile : background) {
				spriteBatch.draw(tile, x, 0f);
				x += tile.getWidth();
			}

			spriteBatch.enableBlending();

			if (EngineLogger.debugMode()
					&& EngineLogger.getDebugLevel() == EngineLogger.DEBUG2)
				backgroundMap.draw(spriteBatch, bbox.width, bbox.height);
		}

		for (Actor a : orderedActors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).draw(spriteBatch);
		}

		for (SpriteActor a : fgActors) {
			a.draw(spriteBatch);
		}

		// Draw the light map
		if (lightMap != null) {
			// Multiplicative blending for light maps
			spriteBatch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);

			float x = 0;

			for (Texture tile : lightMap) {
				spriteBatch.draw(tile, x, 0f);
				x += tile.getWidth();
			}

			spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA,
					GL20.GL_ONE_MINUS_SRC_ALPHA);
		}

		if (overlay != null) {
			overlay.draw(spriteBatch);
		}

		if (EngineLogger.debugMode()
				&& EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {

			StringBuilder sb = new StringBuilder();

			for (Actor a : orderedActors) {
				Rectangle r = a.getBBox();
				sb.setLength(0);
				sb.append(a.getId());
				if (a.getState() != null)
					sb.append(".").append(a.getState());
				// sb.append(" (").append((int) r.getX()).append(", ");
				// sb.append((int) r.getY()).append(", ").append((int)
				// r.getWidth());
				// sb.append(", ").append((int) r.getHeight()).append(") ");
				EngineLogger.getDebugFont().draw(spriteBatch, sb.toString(),
						r.getX(), r.getY());
			}

		}
	}

	public void drawBBoxActors(ShapeRenderer renderer) {
		// renderer.begin(ShapeType.Rectangle);
		renderer.begin(ShapeType.Line);
		renderer.setColor(ACTOR_BBOX_COLOR);

		for (Actor a : orderedActors) {
			Rectangle r = a.getBBox();

			if (r == null) {
				EngineLogger.error("ERROR DRAWING BBOX FOR: " + a.getId());
			}

			renderer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		for (SpriteActor a : fgActors) {
			Rectangle r = a.getBBox();
			renderer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		renderer.end();
	}

	public void setOverlay(OverlayImage o) {
		if (overlay != null)
			overlay.dispose();

		overlay = o;
	}

	public OverlayImage getOverlay() {
		return overlay;
	}

	public void setTransition(Transition t) {
		transition = t;
	}

	public Transition getTransition() {
		return transition;
	}

	public Actor getActor(String id) {
		return getActor(id, true, false);
	}

	public Actor getActor(String id, boolean searchInventory,
			boolean searchFG) {
		Actor a = actors.get(id);

		if (a == null && searchInventory) {
			a = World.getInstance().getInventory().getItem(id);
		}

		if (a == null && searchFG) {
			for (SpriteActor fg : fgActors) {
				if (fg.getId().equals(id))
					a = fg;
			}
		}

		if (a == null && this.id.equals(id))
			a = this;

		return a;
	}

	public HashMap<String, Actor> getActors() {
		return actors;
	}

	public void addActor(Actor actor) {
		actors.put(actor.getId(), actor);
		orderedActors.add(actor);
		
		if(actor instanceof SpriteActor)
			((SpriteActor) actor).setScene(this);
	}

	public void addFgActor(SpriteActor actor) {
		fgActors.add(actor);
	}

	public void setBackground(String bgFilename, String lightMapFilename) {

		if (bgFilename != null && !bgFilename.isEmpty()) {
			ArrayList<String> tiles = getTilesByFilename(bgFilename);

			if (tiles.size() > 0) {
				backgroundFilename = bgFilename;
				background = new Texture[tiles.size()];
			}
		}

		// SET LIGHT MAP
		if (lightMapFilename != null && !lightMapFilename.isEmpty()) {
			ArrayList<String> tiles = getTilesByFilename(lightMapFilename);

			if (tiles.size() > 0) {
				this.lightMapFilename = lightMapFilename;
				lightMap = new Texture[tiles.size()];
			}
		}
	}

	/**
	 * Search for files based in the filename parameter
	 * 
	 * ex. for filename 'bg_0.png': bg_0.png, bg_1.png, bg_2.png... ex. for
	 * filename 'bg.png': bg.png, bg_1.png, bg_2.png...
	 * 
	 * @param filename
	 *            The filename used as search base
	 * @return
	 */
	private ArrayList<String> getTilesByFilename(String filename) {
		ArrayList<String> tiles = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();

		// name without extension
		String name = filename.substring(0, filename.lastIndexOf('.'));
		// extension
		String ext = filename.substring(filename.lastIndexOf('.'));
		String nameWithoutIndex = name.endsWith("_0") ? name.substring(0,
				name.length() - 2) : name;

		int i = 0;

		sb.append(EngineAssetManager.BACKGROUND_DIR).append(filename);

		while (EngineAssetManager.getInstance().assetExists(sb.toString())) {
			i++;

			tiles.add(sb.toString());
			sb.setLength(0);
			sb.append(EngineAssetManager.BACKGROUND_DIR)
					.append(nameWithoutIndex).append("_").append(i).append(ext);
		}

		return tiles;
	}

	public Actor getActorAt(float x, float y) {
		// Se recorre la lista al revés para quedarnos con el más cercano a la
		// cámara
		for (int i = orderedActors.size() - 1; i >= 0; i--) {
			Actor a = orderedActors.get(i);

			if (a.hit(x, y) && !a.getId().equals(player)
					&& a.hasInteraction()) {
				return a;
			}
		}

		return null;
	}

	public Actor getFullSearchActorAt(float x, float y) {
		// Se recorre la lista al revés para quedarnos con el más cercano a la
		// cámara
		for (int i = orderedActors.size() - 1; i >= 0; i--) {
			Actor a = orderedActors.get(i);

			if (a.hit(x, y)) {
				return a;
			}
		}

		for (Actor a : fgActors) {
			if (a.hit(x, y)) {
				return a;
			}
		}

		return null;
	}

	public void setPlayer(SpriteActor a) {
		if (a != null) {
			player = a.getId();
			a.setInteraction(false);
		} else {
			player = null;
		}
	}

	public SpriteActor getPlayer() {
		return (SpriteActor) actors.get(player);
	}

	public PixTileMap getBackgroundMap() {
		return backgroundMap;
	}
	
	public Vector2 getDepthVector() {
		return depthVector;
	}
	
	public void setDepthVector(Vector2 v) {
		depthVector = v;
	}

	public void setBackgroundMap(PixTileMap backgroundMap) {
		this.backgroundMap = backgroundMap;
	}

	public void removeActor(Actor a) {
		Actor res = null;

		if (a.getId().equals(player)) {
			player = null;
		}

		res = actors.remove(a.getId());

		if (res == null)
			fgActors.remove(a);
		else
			orderedActors.remove(a);
	}

	/**
	 * Implements interface for indicating A* where a tile is blocked by an
	 * Actor
	 */
	@Override
	public boolean isBlocked(int x, int y) {
		float size = backgroundMap.getTileSize();

		for (Actor ba : orderedActors) {
			if (!(ba instanceof SpriteActor))
				continue;

			if (ba.getId().equals(player))
				continue; // TODO Change to allow other NPC to move

			if (!ba.hasInteraction())
				continue;

			SpriteActor a = (SpriteActor) ba;

			Rectangle bbox = a.getBBox();

			int x0 = (int) (bbox.x / size);
			int y0 = (int) (bbox.y / size);
			int xf = (int) ((bbox.x + a.getWidth()) / size);
			int yf = (int) ((bbox.y + a.getHeight()) / size);

			// de alto como mucho bloqueamos el ancho
			if (a.getWidth() < a.getHeight())
				yf = (int) ((bbox.y + a.getWidth()) / size);

			// TODO Change the fix +-1 for the player/npc width/2/size
			if (x + 1 >= x0 && x - 1 <= xf && y + 1 >= y0 && y - 1 <= yf)
				return true;
		}

		return false;
	}

	public Texture[] getBackground() {
		return background;
	}
	
	public SceneCamera getCamera() {
		return camera;
	}
	
	public void resetCamera(float worldWidth, float worldHeight) {
		camera.create(worldWidth, worldHeight);

		if (getPlayer() != null)
			setCameraFollowActor(getPlayer());
	}
	
	public void setCameraFollowActor(SpriteActor a) {
		followActor = a;
		
		if(a != null)
			camera.updatePos(a);
	}
	
	public SpriteActor getCameraFollowActor() {
		return followActor;
	}

	@Override
	public void loadAssets() {
		super.loadAssets();

		if (background != null) {
			ArrayList<String> tiles = getTilesByFilename(backgroundFilename);

			// name without extension
			String name = backgroundFilename.substring(0,
					backgroundFilename.lastIndexOf('.'));

			String nameWithoutIndex = name.endsWith("_0") ? name.substring(0,
					name.length() - 2) : name;

			String mapFilename = new StringBuffer()
					.append(EngineAssetManager.BACKGROUND_DIR)
					.append(nameWithoutIndex).append(MAP_FILE_EXTENSION)
					.toString();

			// LOAD MOVEMENT AND DEPTH MAP
			if (EngineAssetManager.getInstance().getAsset(mapFilename).exists()) {
				backgroundMap = new PixTileMap(mapFilename);
			}

			// LOAD BACKGROUND TEXTURES
			for (String filename : tiles) {
				EngineAssetManager.getInstance().loadTexture(filename);
			}

		}

		// LOAD LIGHT MAP
		if (lightMap != null) {
			ArrayList<String> tiles = getTilesByFilename(lightMapFilename);

			for (String filename : tiles) {
				EngineAssetManager.getInstance().loadTexture(filename);
			}
		}

		if (preloadedAtlases != null) {

			for (String s : this.preloadedAtlases) {
				EngineAssetManager.getInstance().loadAtlas(s.trim());
			}
		}

		if (musicFilename != null)
			EngineAssetManager.getInstance().loadMusic(musicFilename);

		for (Actor a : actors.values()) {
			a.loadAssets();
		}

		for (SpriteActor a : fgActors) {
			a.loadAssets();
		}
	}

	@Override
	public void retrieveAssets() {
		super.retrieveAssets();

		// RETRIEVE BACKGROUND
		if (background != null) {
			ArrayList<String> tiles = getTilesByFilename(backgroundFilename);

			float width = 0;

			for (int i = 0; i < background.length; i++) {
				Texture texture = EngineAssetManager.getInstance().getTexture(
						tiles.get(i));
				texture.setFilter(BG_TEXFILTER_MIN, BG_TEXFILTER_MAG);
				background[i] = texture;
				width += texture.getWidth();
			}

			float height = background[0].getHeight();

			setBbox(new Rectangle(0, 0, width, height));

			if (backgroundMap != null) {
				backgroundMap.setTileSize(width
						/ backgroundMap.getWidthInTiles());
			}
			
			// Sets the scrolling dimensions. It must be done here because 
			// the background must be loaded to calculate the bbox
			camera.setScrollingDimensions(getBBox().width,
						getBBox().height);
			
			if(followActor != null)
				camera.updatePos(followActor);
		}

		// RETRIEVE LIGHT MAP
		if (lightMap != null) {
			ArrayList<String> tiles = getTilesByFilename(lightMapFilename);

			for (int i = 0; i < lightMap.length; i++) {
				Texture texture = EngineAssetManager.getInstance().getTexture(
						tiles.get(i));
				// texture.setFilter(BG_TEXFILTER_MIN, BG_TEXFILTER_MAG);
				texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
				lightMap[i] = texture;
			}
		}

		// RETRIEVE ACTORS
		for (Actor a : actors.values()) {
			a.retrieveAssets();
		}

		for (SpriteActor a : fgActors) {
			a.retrieveAssets();
		}

		if (musicFilename != null) {
			music = EngineAssetManager.getInstance().getMusic(musicFilename);
			if (isPlayingSer) { // TODO must be in World???
				playMusic();
				isPlayingSer = false;
			}
		}

		if (overlay != null)
			overlay.retrieveAssets();
	}

	@Override
	public void dispose() {
		super.dispose();

		if (background != null) {
			for (Texture tile : background)
				if (tile != null)
					EngineAssetManager.getInstance().disposeTexture(tile);
		}

		if (lightMap != null) {
			for (Texture tile : lightMap)
				if (tile != null)
					EngineAssetManager.getInstance().disposeTexture(tile);
		}

		if (backgroundMap != null) {
			backgroundMap.dispose();
			backgroundMap = null;
		}

		// orderedActors.clear();

		for (Actor a : actors.values()) {
			a.dispose();
		}

		for (SpriteActor a : fgActors) {
			a.dispose();
		}

		if (preloadedAtlases != null) {
			for (String s : preloadedAtlases) {
				EngineAssetManager.getInstance().disposeAtlas(s.trim());
			}
		}

		if (musicFilename != null && music != null) {
			EngineAssetManager.getInstance().disposeMusic(musicFilename);
			music = null;
		}

		if (overlay != null) {
			overlay.dispose();
			overlay = null;
		}

		transition = null;
	}

	@Override
	public void write(Json json) {
		super.write(json);
		json.writeValue("atlases", preloadedAtlases);
		json.writeValue("actors", actors);
		json.writeValue("fgActors", fgActors);
		json.writeValue("player", player,
				player == null ? null : player.getClass());

		json.writeValue(
				"background",
				backgroundFilename,
				backgroundFilename == null ? null : backgroundFilename
						.getClass());

		json.writeValue("lightMap", lightMapFilename,
				lightMapFilename == null ? null : lightMapFilename.getClass());

		json.writeValue("musicFilename", musicFilename,
				musicFilename == null ? null : musicFilename.getClass());
		json.writeValue("loopMusic", loopMusic);
		json.writeValue("initialMusicDelay", initialMusicDelay);
		json.writeValue("repeatMusicDelay", repeatMusicDelay);

		json.writeValue("isPlaying", music != null && music.isPlaying());

		// TODO save music positionSer when available in API

		json.writeValue("overlay", overlay,
				overlay == null ? null : overlay.getClass());
		json.writeValue("transition", transition, transition == null ? null
				: transition.getClass());
		
		json.writeValue("camera", camera);
		
		json.writeValue("followActor", followActor == null ? null : followActor.getId(),
				followActor == null ? null : String.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		preloadedAtlases = json.readValue("atlases", ArrayList.class,
				String.class, jsonData);

		actors = json.readValue("actors", HashMap.class, Actor.class,
				jsonData);
		fgActors = json.readValue("fgActors", ArrayList.class,
				SpriteActor.class, jsonData);
		player = json.readValue("player", String.class, jsonData);

		for (Actor a : actors.values()) {
			orderedActors.add(a);

			// set scene for SpriteActors
			if (a instanceof SpriteActor) {
				((SpriteActor) a).setScene(this);
			}
		}

		backgroundFilename = json.readValue("background", String.class,
				jsonData);
		lightMapFilename = json.readValue("lightMap", String.class, jsonData);

		setBackground(backgroundFilename, lightMapFilename);

		musicFilename = json.readValue("musicFilename", String.class, jsonData);
		loopMusic = json.readValue("loopMusic", Boolean.class, jsonData);
		initialMusicDelay = json.readValue("initialMusicDelay", Float.class,
				jsonData);
		repeatMusicDelay = json.readValue("repeatMusicDelay", Float.class,
				jsonData);

		isPlayingSer = json.readValue("isPlaying", Boolean.class, jsonData);
		// TODO restore positionSer for music when available in API

		overlay = json.readValue("overlay", OverlayImage.class, jsonData);
		transition = json.readValue("transition", Transition.class, jsonData);
		
		camera = json.readValue("camera", SceneCamera.class, jsonData);
		String followActorId = json.readValue("followActor", String.class,
				jsonData);
		
		setCameraFollowActor((SpriteActor)actors.get(followActorId));
	}

}
