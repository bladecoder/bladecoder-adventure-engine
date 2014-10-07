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
package com.bladecoder.engine.loader;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.anim.AtlasFrameAnimation;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpineRenderer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engine.model.SpriteActor.DepthType;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engine.util.EngineLogger;

public class ChapterXMLLoader extends DefaultHandler {
	private BaseActor actor;
	private Scene scene;

	private Verb currentVerb;
	private Dialog currentDialog;
	private DialogOption currentOption;
	private String initFrameAnimation = null;
	private String player = null;

	private float scale;

	private Locator locator;

	private List<Scene> scenes = new ArrayList<Scene>();
	private String initScene;

	public ChapterXMLLoader() {
		scale = EngineAssetManager.getInstance().getScale();
	}

	public String getInitScene() {
		return initScene;
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (currentVerb != null) {
			parseAction(localName, atts, actor != null ? actor.getId() : null);
		} else if (currentDialog != null) {

			if (!localName.equals("option")) {
				SAXParseException e2 = new SAXParseException(
						"Only 'option' tag allowed in dialogs", locator);
				error(e2);
				throw e2;
			}

			String text = atts.getValue("text");
			String responseText = atts.getValue("response_text");
			String verb = atts.getValue("verb");
			String next = atts.getValue("next");

			if (verb != null && verb.trim().isEmpty())
				verb = null;

			if (text == null || text.trim().isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"'text' atribute mandatory for <option> tag", locator);
				error(e2);
				throw e2;
			}

			String visibleStr = atts.getValue("visible");

			DialogOption o = new DialogOption();
			o.setText(text);
			o.setResponseText(responseText);
			o.setVerbId(verb);
			o.setNext(next);
			o.setParent(currentOption);

			if (visibleStr != null && !visibleStr.trim().isEmpty()) {
				o.setVisible(Boolean.parseBoolean(visibleStr));
			}

			currentOption = o;

			if (o.getParent() == null)
				currentDialog.addOption(o);
			else
				o.getParent().addOption(o);

		} else if (localName.equals("actor")) {
			String type = atts.getValue("type");

			if (type == null || type.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"BaseActor 'type' attribute not found or empty", locator);
				error(e2);
				throw e2;
			}

			if (type.equals("no_renderer")) {
				actor = new BaseActor();
			} else {
				actor = new SpriteActor();

				if (type.equals("atlas")) { // ATLAS RENDERER
					((SpriteActor) actor).setRenderer(new AtlasRenderer());
				} else if (type.equals("image")) { // IMAGE RENDERER
					((SpriteActor) actor).setRenderer(new ImageRenderer());					
				} else if (type.equals("3d")) { // 3D RENDERER
					Sprite3DRenderer r = new Sprite3DRenderer();
					((SpriteActor) actor).setRenderer(r);

					Vector3 camPos, camRot;
					float fov = 67;

					try {
						Vector2 spriteSize = Param.parseVector2(atts
								.getValue("sprite_size"));

						spriteSize.x *= scale;
						spriteSize.y *= scale;

						r.setSpriteSize(spriteSize);

						if (atts.getValue("cam_pos") != null) {

							camPos = Param.parseVector3(atts
									.getValue("cam_pos"));

							r.setCameraPos(camPos.x, camPos.y, camPos.z);
						}

						if (atts.getValue("cam_rot") != null) {
							camRot = Param.parseVector3(atts
									.getValue("cam_rot"));

							r.setCameraRot(camRot.x, camRot.y, camRot.z);
						}

						fov = Float.parseFloat(atts.getValue("fov"));
						r.setCameraFOV(fov);

						if (atts.getValue("camera_name") != null) {
							r.setCameraName(atts.getValue("camera_name"));
						}

					} catch (Exception e) {
						SAXParseException e2 = new SAXParseException(
								"Wrong sprite3d params", locator, e);
						error(e2);
						throw e2;
					}

				} else if (type.equals("spine")) { // SPINE RENDERER
					SpineRenderer r = new SpineRenderer();
					((SpriteActor) actor).setRenderer(r);
				}

				if (atts.getValue("walking_speed") != null
						&& !atts.getValue("walking_speed").isEmpty()) {
					float s = Float.parseFloat(atts.getValue("walking_speed"))
							* scale;
					((SpriteActor) actor).setWalkingSpeed(s);
				}

				initFrameAnimation = atts.getValue("init_frame_animation");

				// PARSE DEPTH MAP USE
				String depthType = atts.getValue("depth_type");
				((SpriteActor) actor).setDepthType(DepthType.NONE);

				if (depthType != null && !depthType.isEmpty()) {
					if (depthType.equals("vector"))
						((SpriteActor) actor).setDepthType(DepthType.VECTOR);
				}
			}
			

			String id = atts.getValue("id");
			String desc = atts.getValue("desc");

			String state = atts.getValue("state");

			if (id == null || id.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"BaseActor 'id' attribute not found or empty", locator);
				error(e2);
				throw e2;
			}

			actor.setId(id);

			if (desc != null)
				actor.setDesc(desc);

			if (state != null)
				actor.setState(state);					

			// PARSE BBOX
			Polygon p = null;

			if (atts.getValue("bbox") != null) {

				try {
					p = Param.parsePolygon(atts.getValue("bbox"));
					p.setScale(scale, scale);
				} catch (NumberFormatException e) {
					SAXParseException e2 = new SAXParseException(
							"Wrong Bounding Box Definition", locator, e);
					error(e2);
					throw e2;
				}

				actor.setBbox(p);
			} else if (type.equals("no_renderer")) {
				SAXParseException e2 = new SAXParseException(
						"Bounding box definition not set for actor", locator);
				error(e2);
				throw e2;
			} else {
				p = new Polygon();
				actor.setBbox(p);
				((SpriteActor) actor).setBboxFromRenderer(true);
			}	

			// PARSE POSTITION
			Vector2 pos = Param.parseVector2(atts.getValue("pos"));
			if (pos == null) {
				SAXParseException e2 = new SAXParseException(
						"Wrong actor XML position", locator);
				error(e2);
				throw e2;
			}

			pos.x *= scale;
			pos.y *= scale;

			actor.setPosition(pos.x, pos.y);

			if (atts.getValue("interaction") != null) {
				boolean interaction = Boolean.parseBoolean(atts
						.getValue("interaction"));
				actor.setInteraction(interaction);
			}

			if (atts.getValue("visible") != null) {
				boolean visible = Boolean
						.parseBoolean(atts.getValue("visible"));
				actor.setVisible(visible);
			}

			if (atts.getValue("obstacle") != null) {
				boolean obstacle = Boolean.parseBoolean(atts
						.getValue("obstacle"));
				actor.setWalkObstacle(obstacle);
			}

			String layerStr = atts.getValue("layer");
			
			if(layerStr.equals("background")) {
				actor.setLayer(BaseActor.ActorLayer.BACKGROUND);
			} else if(layerStr.equals("foreground")) {
				actor.setLayer(BaseActor.ActorLayer.FOREGROUND);			
			} else {
				actor.setLayer(BaseActor.ActorLayer.DYNAMIC);
			}
			
			scene.addActor(actor);

		} else if (localName.equals("frame_animation")) {

			if (actor == null || !(actor instanceof SpriteActor)) {
				SAXParseException e = new SAXParseException(
						"'frame_animation' TAG must be inside sprite actors",
						locator);
				error(e);
				throw e;
			}

			String speedstr = atts.getValue("speed");
			String animationTypestr = atts.getValue("animation_type");
			String delaystr = atts.getValue("delay");
			String countstr = atts.getValue("count");
			String soundId = atts.getValue("sound");
			String inDstr = atts.getValue("inD");
			String outDstr = atts.getValue("outD");
			String preloadstr = atts.getValue("preload");
			String disposewhenplayedstr = atts.getValue("dispose_when_played");

			float speed = 1f;
			float delay = 0f;
			int animationType;
			int count = Tween.INFINITY;
			Vector2 inD = null, outD = null;
			boolean preload = true;
			boolean disposeWhenPlayed = false;

			String id = atts.getValue("id");

			if (id == null || id.isEmpty()) {
				SAXParseException e = new SAXParseException(
						"Animation 'id' not found or empty", locator);
				error(e);
				throw e;
			}

			String source = atts.getValue("source");
			if (source == null || source.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"Source name not found or empty", locator);
				error(e2);
				throw e2;
			}

			try {
				if (speedstr != null && !speedstr.isEmpty())
					speed = Float.parseFloat(speedstr);

				if (delaystr != null && !delaystr.isEmpty())
					delay = Float.parseFloat(delaystr);

				if (countstr != null && !countstr.isEmpty())
					count = Integer.parseInt(countstr);

				if (preloadstr != null && !preloadstr.isEmpty())
					preload = Boolean.parseBoolean(preloadstr);

				if (disposewhenplayedstr != null
						&& !disposewhenplayedstr.isEmpty())
					disposeWhenPlayed = Boolean
							.parseBoolean(disposewhenplayedstr);

				if (inDstr != null && !inDstr.isEmpty()) {
					inD = Param.parseVector2(inDstr);
				}

				if (outDstr != null && !outDstr.isEmpty()) {
					outD = Param.parseVector2(outDstr);
				}

			} catch (NumberFormatException e) {
				SAXParseException e2 = new SAXParseException(
						"Wrong Sprite Animation parameters", locator, e);
				error(e2);
				throw e2;
			}

			if (animationTypestr == null || animationTypestr.isEmpty()
					|| animationTypestr.equalsIgnoreCase("repeat")) {
				animationType = Tween.REPEAT;
			} else if (animationTypestr.equalsIgnoreCase("reverse")) {
				animationType = Tween.REVERSE;
			} else if (animationTypestr.equalsIgnoreCase("yoyo")) {
				animationType = Tween.PINGPONG;
			} else {
				animationType = Tween.NO_REPEAT;
			}

			AtlasFrameAnimation sa = new AtlasFrameAnimation();

			sa.set(id, source, speed, delay, count, animationType, soundId,
					inD, outD, preload, disposeWhenPlayed);

			((SpriteActor) actor).getRenderer().addFrameAnimation(sa);
		} else if (localName.equals("verb")) {
			parseVerb(localName, atts, actor != null ? actor.getVerbManager()
					: scene.getVerbManager());
		} else if (localName.equals("dialog")) {
			String id = atts.getValue("id");

			currentDialog = new Dialog();
			currentDialog.setId(id);
			currentDialog.setActor(actor.getId());
			currentOption = null;

			actor.addDialog(id, currentDialog);
		} else if (localName.equals("sound")) {
			parseSound(localName, atts, actor);
		} else if (localName.equals("chapter")) {
			initScene = atts.getValue("init_scene");
		} else if (localName.equals("walk_zone")) {
			PolygonalNavGraph polygonalPathFinder = new PolygonalNavGraph();
			Polygon poly = Param.parsePolygon(atts.getValue("polygon"),
					atts.getValue("pos"));
			poly.setScale(scale, scale);
			poly.setPosition(poly.getX() * scale, poly.getY() * scale);
			polygonalPathFinder.setWalkZone(poly);

			scene.setPolygonalNavGraph(polygonalPathFinder);
		} else if (localName.equals("obstacle")) {
			PolygonalNavGraph polygonalPathFinder = scene
					.getPolygonalNavGraph();
			Polygon poly = Param.parsePolygon(atts.getValue("polygon"),
					atts.getValue("pos"));
			poly.setScale(scale, scale);
			poly.setPosition(poly.getX() * scale, poly.getY() * scale);
			polygonalPathFinder.addObstacle(poly);
		} else if (localName.equals("scene")) {
			this.scene = new Scene();
			scenes.add(scene);

			if (initScene == null)
				initScene = this.scene.getId();

			String idScn = atts.getValue("id");
			String bgFilename = atts.getValue("background");
			String lightmap = atts.getValue("lightmap");
			String musicFilename = atts.getValue("music");
			String loopMusicStr = atts.getValue("loop_music");
			String initialMusicDelayStr = atts.getValue("initial_music_delay");
			String repeatMusicDelayStr = atts.getValue("repeat_music_delay");

			scene.setDepthVector(Param.parseVector2(atts
					.getValue("depth_vector")));
			player = atts.getValue("player");

			if (idScn == null || idScn.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"Scene 'id' not found or empty", locator);
				error(e2);
				throw e2;
			}

			scene.setId(idScn);

			scene.setBackground(bgFilename, lightmap);

			if (musicFilename != null) {
				boolean loopMusic = false;
				float initialDelay = 0;
				float repeatDelay = -1;

				if (loopMusicStr != null)
					loopMusic = Boolean.parseBoolean(loopMusicStr);
				if (initialMusicDelayStr != null)
					initialDelay = Float.parseFloat(initialMusicDelayStr);
				if (repeatMusicDelayStr != null)
					repeatDelay = Float.parseFloat(repeatMusicDelayStr);

				scene.setMusic(musicFilename, loopMusic, initialDelay,
						repeatDelay);
			}
		} else {
//			SAXParseException e = new SAXParseException("Wrong label '"
//					+ localName + "' loading Scene.", locator);
//			error(e);
//			throw e;
			EngineLogger.error("TAG not supported in Chapter document: "  + localName + " LINE: " + locator.getLineNumber());
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		if (localName.equals("verb"))
			currentVerb = null;
		else if (localName.equals("dialog"))
			currentDialog = null;
		else if (localName.equals("option"))
			currentOption = currentOption.getParent();
		else if (localName.equals("actor")) {
			if (actor instanceof SpriteActor && initFrameAnimation != null
					&& !initFrameAnimation.isEmpty()) {
				((SpriteActor) actor).getRenderer().setInitFrameAnimation(
						initFrameAnimation);
			}

			actor = null;
		} else if (localName.equals("scene")) {
			scene.setPlayer((SpriteActor) scene.getActor(player, false));
		}
	}

	private void parseSound(String localName, Attributes atts, BaseActor actor)
			throws SAXException {
		String id = atts.getValue("id");
		String filename = atts.getValue("filename");
		String loopStr = atts.getValue("loop");
		String volumeStr = atts.getValue("volume");

		boolean loop = false;
		float volume = 1f;

		if (filename == null || filename.isEmpty())
			error(new SAXParseException("Sound 'filename' not found or empty",
					locator));

		if (loopStr != null && !loopStr.isEmpty()) {
			loop = Boolean.parseBoolean(loopStr);
		}

		if (volumeStr != null && !volumeStr.isEmpty()) {
			volume = Float.parseFloat(volumeStr);
		}

		if (id == null || id.isEmpty())
			id = filename;

		actor.addSound(id, filename, loop, volume);
	}

	private void parseVerb(String localName, Attributes atts, VerbManager v) {

		String id = atts.getValue("id");
		String target = atts.getValue("target");
		String state = atts.getValue("state");

		if (target != null)
			id = id + "." + target;

		if (state != null)
			id = id + "." + state;

		currentVerb = new Verb(id);

		v.addVerb(id, currentVerb);
	}

	
	private final HashMap<String, String> actionParams = new HashMap<String, String>();
	
	private void parseAction(String localName, Attributes atts, String actor) {

		if (localName.equals("action")) {
			String actionName = null;
			Action action = null;
			String actionClass = null;
			actionParams.clear();

			for (int i = 0; i < atts.getLength(); i++) {
				String attName = atts.getLocalName(i);

				if (attName.equals("class")) {
					actionClass = atts.getValue(attName);
				} else if (attName.equals("action_name")) {
					actionName = atts.getValue(attName);
				} else {
					String value = atts.getValue(attName);

					actionParams.put(attName, value);
				}
			}

			if (atts.getValue("actor") == null)
				actionParams.put("actor", actor);

			if (actionClass != null) {
				action = ActionFactory.createByClass(actionClass, actionParams);
			} else if (actionName != null) {
				action = ActionFactory.create(actionName, actionParams);
			}

			if (action != null) {
				currentVerb.add(action);
			}
		} else {
			EngineLogger.error("TAG not supported inside VERB: "  + localName + " LINE: " + locator.getLineNumber());
		}
	}

	public List<Scene> getScenes() {
		return scenes;
	}

	@Override
	public void setDocumentLocator(Locator l) {
		locator = l;
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		EngineLogger.error(MessageFormat.format(
				"{0} Line: {1} Column: {2}. {3}", actor.getId(),
				e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
		EngineLogger.error("CAUSA", (Exception) e.getCause());
	}

}