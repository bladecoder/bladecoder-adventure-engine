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
package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.bladecoder.engine.model.Actor;
import com.bladecoder.engine.model.SceneCamera;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engine.model.World;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.pathfinder.NavNode;
import com.bladecoder.engine.polygonalpathfinder.NavNodePolygonal;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engine.util.EngineLogger;

public class Scene implements Serializable,
		AssetConsumer {
	
	public static final Color ACTOR_BBOX_COLOR = new Color(0.2f, 0.2f, 0.8f,
			1f);
	private static final TextureFilter BG_TEXFILTER_MAG = TextureFilter.Linear;
	private static final TextureFilter BG_TEXFILTER_MIN = TextureFilter.Linear;
	public static final Color WALKZONE_COLOR = Color.GREEN;
	public static final Color OBSTACLE_COLOR = Color.RED;

	/** 
	 * All actors in the scene
	 */
	private HashMap<String, Actor> actors = new HashMap<String, Actor>();

	/**
	 * Actor layers: Background actors, dynamic (ordered) and foreground
	 */
	private final List<Actor> bgActors = new ArrayList<Actor>();
	private final List<Actor> dynamicActors = new ArrayList<Actor>();
	private final List<Actor> fgActors = new ArrayList<Actor>();
	
	private SceneCamera camera = new SceneCamera();
	
	private Texture[] background;
	private Texture[] lightMap;
	private String backgroundFilename;
	private String lightMapFilename;
	
	/** For polygonal PathFinding */
	private PolygonalNavGraph polygonalNavGraph;
	
	/** depth vector. x: scale when y=0, y: scale when y=scene height */
	private Vector2 depthVector;

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
	
	private String id;
	
	/** internal state. Can be used for actions to maintain a state machine */
	private String state;
	
	private VerbManager verbs = new VerbManager();
	
	/**
	 * Add support for the use of global custom properties/variables in the game
	 * logic
	 */
	private HashMap<String, String> customProperties;

	public Scene() {	
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String s) {
		state = s;
	}

	
	public void setCustomProperty(String name, String value) {
		if(customProperties == null)
			customProperties = new HashMap<String, String>();
		
		customProperties.put(name, value);
	}
	
	public String getCustomProperty(String name) {
		return customProperties.get(name);
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
	
	public float getFakeDepthScale(float y) {
		if(depthVector==null)
			return 1.0f;
		
		// interpolation equation
		return Math.abs(depthVector.x + (depthVector.y - depthVector.x) * y
				/ camera.getScrollingHeight());
	}

	public void setMusic(String filename, boolean loop, float initialDelay,
			float repeatDelay) {
		loopMusic = loop;
		musicFilename = filename;
		initialMusicDelay = initialDelay;
		repeatMusicDelay = repeatDelay;
	}
	
	public VerbManager getVerbManager() {
		return verbs;
	}

	public Verb getVerb(String id) {
		return verbs.getVerb(id, state, null);
	}
	
	public void runVerb(String id) {
		verbs.runVerb(id, state, null);
	}

	public void update(float delta) {
		// We draw the elements in order: from top to bottom.
		// so we need to order the array list
		Collections.sort(dynamicActors);

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

		for (Actor a:actors.values()) {
			a.update(delta);
		}
		
		camera.update(delta);
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
		}
		
		for (Actor a : bgActors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).draw(spriteBatch);
		}

		for (Actor a : dynamicActors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).draw(spriteBatch);
		}

		for (Actor a : fgActors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).draw(spriteBatch);
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
	}

	public void drawBBoxLines(ShapeRenderer renderer) {
		// renderer.begin(ShapeType.Rectangle);
		renderer.begin(ShapeType.Line);
		renderer.setColor(ACTOR_BBOX_COLOR);

		for (Actor a : actors.values()) {
			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING BBOX FOR: " + a.getId());
			}
			
			Rectangle r = a.getBBox().getBoundingRectangle();

			renderer.polygon(p.getTransformedVertices());
			renderer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}
		
		if(polygonalNavGraph != null) {
			renderer.setColor(WALKZONE_COLOR);
			renderer.polygon(polygonalNavGraph.getWalkZone().getTransformedVertices());
			
			ArrayList<Polygon> obstacles = polygonalNavGraph.getObstacles();
			
			renderer.setColor(OBSTACLE_COLOR);
			for(Polygon p: obstacles) {
				renderer.polygon(p.getTransformedVertices());
			}
			
			// DRAW LINEs OF SIGHT
			renderer.setColor(Color.WHITE);
			ArrayList<NavNodePolygonal> nodes = polygonalNavGraph.getGraphNodes();
			for(NavNodePolygonal n:nodes) {
				for(NavNode n2:n.neighbors) {
					renderer.line(n.x, n.y, ((NavNodePolygonal)n2).x, ((NavNodePolygonal)n2).y);
				}
			}
		}

		renderer.end();
	}

	public void setTransition(Transition t) {
		transition = t;
	}

	public Transition getTransition() {
		return transition;
	}

	public Actor getActor(String id, boolean searchInventory) {
		Actor a = actors.get(id);

		if (a == null && searchInventory) {
			a = World.getInstance().getInventory().getItem(id);
		}

		return a;
	}

	public HashMap<String, Actor> getActors() {
		return actors;
	}

	public void addActor(Actor actor) {
		actors.put(actor.getId(), actor);
		actor.setScene(this);
		
		switch(actor.getLayer()) {
		case BACKGROUND:
			bgActors.add(actor);
			break;
		case DYNAMIC:
			dynamicActors.add(actor);
			break;
		case FOREGROUND:
			fgActors.add(actor);
			break;		
		}
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
	 * Search for files based in the filename parameter. Used for bg images and maps.
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
		for (Actor a:fgActors) {
			if ( a.hasInteraction() && a.hit(x, y)) {
				return a;
			}
		}
				
		// Se recorre la lista al revés para quedarnos con el más cercano a la
		// cámara
		for (int i = dynamicActors.size() - 1; i >= 0; i--) {
			Actor a = dynamicActors.get(i);

			if (a.hasInteraction() && a.hit(x, y)) {
				return a;
			}
		}
		
		for (Actor a:bgActors) {
			if (a.hasInteraction() && a.hit(x, y)) {
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

	public Vector2 getDepthVector() {
		return depthVector;
	}
	
	public void setDepthVector(Vector2 v) {
		depthVector = v;
	}

	public void removeActor(Actor a) {

		if (a.getId().equals(player)) {
			player = null;
		}

		actors.remove(a.getId());
		
		switch(a.getLayer()) {
		case BACKGROUND:
			bgActors.remove(a);
			break;
		case DYNAMIC:
			dynamicActors.remove(a);
			break;
		case FOREGROUND:
			fgActors.remove(a);
			break;		
		}
		
		if(a.isWalkObstacle() && polygonalNavGraph != null)
			polygonalNavGraph.removeDinamicObstacle(a.getBBox());
			
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

		if (background != null) {
			ArrayList<String> tiles = getTilesByFilename(backgroundFilename);

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

		if (musicFilename != null)
			EngineAssetManager.getInstance().loadMusic(musicFilename);

		for (Actor a : actors.values()) {
			a.loadAssets();
		}
	}

	@Override
	public void retrieveAssets() {

		// RETRIEVE BACKGROUND
		if (background != null) {
			ArrayList<String> tiles = getTilesByFilename(backgroundFilename);

			int width = 0;

			for (int i = 0; i < background.length; i++) {
				Texture texture = EngineAssetManager.getInstance().getTexture(
						tiles.get(i));
				texture.setFilter(BG_TEXFILTER_MIN, BG_TEXFILTER_MAG);
				background[i] = texture;
				width += texture.getWidth();
			}

			int height = background[0].getHeight();
			
			// Sets the scrolling dimensions. It must be done here because 
			// the background must be loaded to calculate the bbox
			camera.setScrollingDimensions(width,
						height);
			
//			if(followActor != null)
//				camera.updatePos(followActor);
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
		
		// CALC WALK GRAPH
		if(polygonalNavGraph != null) {
			polygonalNavGraph.createInitialGraph();
		}

		// RETRIEVE ACTORS
		for (Actor a : actors.values()) {
			a.retrieveAssets();
		}

		if (musicFilename != null) {
			music = EngineAssetManager.getInstance().getMusic(musicFilename);
			if (isPlayingSer) { // TODO must be in World???
				playMusic();
				isPlayingSer = false;
			}
		}
	}

	@Override
	public void dispose() {

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

		// orderedActors.clear();

		for (Actor a : actors.values()) {
			a.dispose();
		}

		if (musicFilename != null && music != null) {
			EngineAssetManager.getInstance().disposeMusic(musicFilename);
			music = null;
		}

		transition = null;
	}
	

	public PolygonalNavGraph getPolygonalNavGraph() {
		return polygonalNavGraph;
	}

	public void setPolygonalNavGraph(PolygonalNavGraph polygonalNavGraph) {
		this.polygonalNavGraph = polygonalNavGraph;
	}


	// TODO SAVE BG WIDTH AND HEIGHT + WALKZONE
	@Override
	public void write(Json json) {
		json.writeValue("id", id);
		json.writeValue("state", state, state == null ? null : state.getClass());
		json.writeValue("verbs", verbs);
		
		json.writeValue("actors", actors);
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

		json.writeValue("transition", transition, transition == null ? null
				: transition.getClass());
		
		json.writeValue("camera", camera);
		
		json.writeValue("followActor", followActor == null ? null : followActor.getId(),
				followActor == null ? null : String.class);
		
		json.writeValue("customProperties", customProperties, customProperties == null ? null : customProperties.getClass());
		
		json.writeValue("depthVector", depthVector);
		
		json.writeValue("polygonalNavGraph", polygonalNavGraph, polygonalNavGraph == null ? null : PolygonalNavGraph.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		id = json.readValue("id", String.class, jsonData);
		state = json.readValue("state", String.class, jsonData);
		verbs = json.readValue("verbs", VerbManager.class, jsonData);

		actors = json.readValue("actors", HashMap.class, Actor.class,
				jsonData);
		player = json.readValue("player", String.class, jsonData);

		for (Actor a : actors.values()) {			
			a.setScene(this);
			
			switch(a.getLayer()) {
			case BACKGROUND:
				bgActors.add(a);
				break;
			case DYNAMIC:
				dynamicActors.add(a);
				break;
			case FOREGROUND:
				fgActors.add(a);
				break;		
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

		transition = json.readValue("transition", Transition.class, jsonData);
		
		camera = json.readValue("camera", SceneCamera.class, jsonData);
		String followActorId = json.readValue("followActor", String.class,
				jsonData);
		
		setCameraFollowActor((SpriteActor)actors.get(followActorId));
		
		customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);
		
		depthVector = json.readValue("depthVector", Vector2.class, jsonData);
		polygonalNavGraph = json.readValue("polygonalNavGraph", PolygonalNavGraph.class, jsonData);
	}
}
