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
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.ActorRenderer;
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
	private String initAnimation = null;
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

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {

		if (currentVerb != null) { // INSIDE VERB

			if (!localName.equals(XMLConstants.ACTION_TAG)) {
				SAXParseException e2 = new SAXParseException("TAG not supported inside VERB: " + localName, locator);
				error(e2);
				throw e2;
			}

			parseAction(atts, actor != null ? actor.getId() : null);
		} else if (currentDialog != null) { // INSIDE DIALOG

			if (!localName.equals(XMLConstants.OPTION_TAG)) {
				SAXParseException e2 = new SAXParseException("Only 'option' tag allowed in dialogs", locator);
				error(e2);
				throw e2;
			}

			parseOption(atts);

		} else if (localName.equals(XMLConstants.ACTOR_TAG)) {
			parseActor(atts);
		} else if (localName.equals(XMLConstants.ANIMATION_TAG)) {
			parseAnimation(atts);
		} else if (localName.equals(XMLConstants.VERB_TAG)) {
			parseVerb(atts, actor != null ? ((InteractiveActor) actor).getVerbManager() : scene.getVerbManager());
		} else if (localName.equals(XMLConstants.DIALOG_TAG)) {
			String id = atts.getValue(XMLConstants.ID_ATTR);

			currentDialog = new Dialog();
			currentDialog.setId(id);
			currentDialog.setActor(actor.getId());

			((CharacterActor) actor).addDialog(id, currentDialog);
		} else if (localName.equals(XMLConstants.SOUND_TAG)) {
			parseSound(atts, (InteractiveActor) actor);
		} else if (localName.equals(XMLConstants.CHAPTER_TAG)) {
			initScene = atts.getValue(XMLConstants.INIT_SCENE_ATTR);
		} else if (localName.equals(XMLConstants.WALK_ZONE_TAG)) {
			PolygonalNavGraph polygonalPathFinder = new PolygonalNavGraph();
			Polygon poly = new Polygon();

			Param.parsePolygon(poly, atts.getValue(XMLConstants.POLYGON_ATTR), atts.getValue(XMLConstants.POS_ATTR));
			poly.setScale(scale, scale);
			poly.setPosition(poly.getX() * scale, poly.getY() * scale);
			polygonalPathFinder.setWalkZone(poly);

			scene.setPolygonalNavGraph(polygonalPathFinder);
		} else if (localName.equals(XMLConstants.SCENE_TAG)) {
			parseScene(atts);
		} else if (localName.equals(XMLConstants.LAYER_TAG)) {
			parseLayer(atts);
		} else {
			// SAXParseException e = new SAXParseException("Wrong label '"
			// + localName + "' loading Scene.", locator);
			// error(e);
			// throw e;
			EngineLogger
					.error("TAG not supported in Chapter document: " + localName + " LINE: " + locator.getLineNumber());
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

		if (localName.equals(XMLConstants.VERB_TAG))
			currentVerb = null;
		else if (localName.equals(XMLConstants.DIALOG_TAG))
			currentDialog = null;
		else if (localName.equals(XMLConstants.ACTOR_TAG)) {
			if (actor instanceof SpriteActor && initAnimation != null && !initAnimation.isEmpty()) {
				((SpriteActor) actor).getRenderer().setInitAnimation(initAnimation);
			}

			actor = null;
		} else if (localName.equals(XMLConstants.SCENE_TAG)) {
			scene.setPlayer((CharacterActor) scene.getActor(player, false));
			scene.orderLayersByZIndex();
			scene = null;
		}
	}

	private void parseScene(Attributes atts) throws SAXException {

		this.scene = new Scene();

		String idScn = atts.getValue(XMLConstants.ID_ATTR);
		String musicFilename = atts.getValue(XMLConstants.MUSIC_ATTR);
		String loopMusicStr = atts.getValue(XMLConstants.LOOP_MUSIC_ATTR);
		String initialMusicDelayStr = atts.getValue(XMLConstants.INITIAL_MUSIC_DELAY_ATTR);
		String repeatMusicDelayStr = atts.getValue(XMLConstants.REPEAT_MUSIC_DELAY_ATTR);
		String state = atts.getValue(XMLConstants.STATE_ATTR);

		scene.setBackground(atts.getValue(XMLConstants.BACKGROUND_ATLAS_ATTR),
				atts.getValue(XMLConstants.BACKGROUND_REGION_ATTR), atts.getValue(XMLConstants.LIGHTMAP_ATLAS_ATTR),
				atts.getValue(XMLConstants.LIGHTMAP_REGION_ATTR));

		if (state != null)
			scene.setState(state);

		scene.setDepthVector(Param.parseVector2(atts.getValue(XMLConstants.DEPTH_VECTOR_ATTR)));
		player = atts.getValue(XMLConstants.PLAYER_ATTR);

		if (idScn == null || idScn.isEmpty()) {
			SAXParseException e2 = new SAXParseException("Scene 'id' not found or empty", locator);
			error(e2);
			throw e2;
		}

		scene.setId(idScn);

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

			scene.setMusic(musicFilename, loopMusic, initialDelay, repeatDelay);
		}

		scenes.add(scene);

		if (initScene == null)
			initScene = this.scene.getId();
	}

	private void parseActor(Attributes atts) throws SAXException {
		String type = atts.getValue(XMLConstants.TYPE_ATTR);

		if (type == null || type.isEmpty()) {
			SAXParseException e2 = new SAXParseException("BaseActor 'type' attribute not found or empty", locator);
			error(e2);
			throw e2;
		}

		if (type.equals(XMLConstants.BACKGROUND_VALUE)) {
			actor = new InteractiveActor();
		} else if (type.equals(XMLConstants.OBSTACLE_VALUE)) {
			actor = new ObstacleActor();
		} else { // Sprite or character
			String renderer = atts.getValue(XMLConstants.RENDERER_ATTR);

			if (type.equals(XMLConstants.CHARACTER_VALUE)) {
				actor = new CharacterActor();
			} else {
				actor = new SpriteActor();
			}

			if (renderer.equals(XMLConstants.ATLAS_VALUE)) { // ATLAS RENDERER
				((SpriteActor) actor).setRenderer(new AtlasRenderer());
			} else if (renderer.equals(XMLConstants.IMAGE_VALUE)) { // IMAGE
				// RENDERER
				((SpriteActor) actor).setRenderer(new ImageRenderer());
			} else if (renderer.equals(XMLConstants.S3D_VALUE)) { // 3D RENDERER
				Sprite3DRenderer r = new Sprite3DRenderer();
				((SpriteActor) actor).setRenderer(r);

				Vector3 camPos, camRot;
				float fov = 67;

				try {
					Vector2 spriteSize = Param.parseVector2(atts.getValue(XMLConstants.SPRITE_SIZE_ATTR));

					spriteSize.x *= scale;
					spriteSize.y *= scale;

					r.setSpriteSize(spriteSize);

					if (atts.getValue(XMLConstants.CAM_POS_ATTR) != null) {

						camPos = Param.parseVector3(atts.getValue(XMLConstants.CAM_POS_ATTR));

						r.setCameraPos(camPos.x, camPos.y, camPos.z);
					}

					if (atts.getValue(XMLConstants.CAM_ROT_ATTR) != null) {
						camRot = Param.parseVector3(atts.getValue(XMLConstants.CAM_ROT_ATTR));

						r.setCameraRot(camRot.x, camRot.y, camRot.z);
					}

					fov = Float.parseFloat(atts.getValue(XMLConstants.FOV_ATTR));
					r.setCameraFOV(fov);

					if (atts.getValue(XMLConstants.CAMERA_NAME_ATTR) != null) {
						r.setCameraName(atts.getValue(XMLConstants.CAMERA_NAME_ATTR));
					}

				} catch (Exception e) {
					SAXParseException e2 = new SAXParseException("Wrong sprite3d params", locator, e);
					error(e2);
					throw e2;
				}

			} else if (renderer.equals(XMLConstants.SPINE_VALUE)) { // SPINE
				// RENDERER
				try {
					Class<?> c = ClassReflection.forName("com.bladecoder.engine.spine.SpineRenderer");
					ActorRenderer r = (ActorRenderer) ClassReflection.newInstance(c);
					((SpriteActor) actor).setRenderer(r);
				} catch (ReflectionException e) {
					SAXParseException e2 = new SAXParseException("Spine plugin not found", locator, e);
					error(e2);
					throw e2;
				}
			}

			initAnimation = atts.getValue(XMLConstants.INIT_ANIMATION_ATTR);

			// PARSE DEPTH MAP USE
			String depthType = atts.getValue(XMLConstants.DEPTH_TYPE_ATTR);
			((SpriteActor) actor).setDepthType(DepthType.NONE);

			if (depthType != null && !depthType.isEmpty()) {
				if (depthType.equals(XMLConstants.VECTOR_ATTR))
					((SpriteActor) actor).setDepthType(DepthType.VECTOR);
			}

			if (actor instanceof CharacterActor) {
				if (atts.getValue(XMLConstants.WALKING_SPEED_ATTR) != null
						&& !atts.getValue(XMLConstants.WALKING_SPEED_ATTR).isEmpty()) {
					float s = Float.parseFloat(atts.getValue(XMLConstants.WALKING_SPEED_ATTR));
					((CharacterActor) actor).setWalkingSpeed(s);
				}

				if (atts.getValue(XMLConstants.TEXT_COLOR_ATTR) != null
						&& !atts.getValue(XMLConstants.TEXT_COLOR_ATTR).isEmpty()) {
					String s = atts.getValue(XMLConstants.TEXT_COLOR_ATTR);
					((CharacterActor) actor).setTextColor(Param.parseColor(s));
				}
			}
		}

		String id = atts.getValue(XMLConstants.ID_ATTR);
		if (id == null || id.isEmpty()) {
			SAXParseException e2 = new SAXParseException("BaseActor 'id' attribute not found or empty", locator);
			error(e2);
			throw e2;
		}

		actor.setId(id);

		// PARSE BBOX
		if (atts.getValue(XMLConstants.BBOX_ATTR) != null) {

			try {
				Param.parsePolygon(actor.getBBox(), atts.getValue(XMLConstants.BBOX_ATTR));
				actor.getBBox().setScale(scale, scale);
			} catch (NumberFormatException e) {
				SAXParseException e2 = new SAXParseException("Wrong Bounding Box Definition", locator, e);
				error(e2);
				throw e2;
			}
		} else if (!(actor instanceof SpriteActor)) {
			SAXParseException e2 = new SAXParseException("Bounding box definition not set for actor", locator);
			error(e2);
			throw e2;
		} else {
			((SpriteActor) actor).setBboxFromRenderer(true);
		}

		// PARSE POSITION
		Vector2 pos = Param.parseVector2(atts.getValue(XMLConstants.POS_ATTR));
		if (pos == null) {
			SAXParseException e2 = new SAXParseException("Wrong actor XML position", locator);
			error(e2);
			throw e2;
		}

		pos.x *= scale;
		pos.y *= scale;

		actor.setPosition(pos.x, pos.y);

		if (atts.getValue(XMLConstants.VISIBLE_ATTR) != null) {
			boolean visible = Boolean.parseBoolean(atts.getValue(XMLConstants.VISIBLE_ATTR));
			actor.setVisible(visible);
		}

		if (actor instanceof InteractiveActor) {

			InteractiveActor ia = (InteractiveActor) actor;
			
			String layerStr = atts.getValue(XMLConstants.LAYER_ATTR);
			ia.setLayer(layerStr);

			String desc = atts.getValue(XMLConstants.DESC_ATTR);
			if (desc != null)
				ia.setDesc(desc);

			String state = atts.getValue(XMLConstants.STATE_ATTR);
			if (state != null)
				ia.setState(state);

			if (atts.getValue("zIndex") != null) {
				float z = Float.parseFloat(atts.getValue(XMLConstants.ZINDEX_ATTR));
				ia.setZIndex(z);
			}

			if (atts.getValue(XMLConstants.INTERACTION_ATTR) != null) {
				boolean interaction = Boolean.parseBoolean(atts.getValue(XMLConstants.INTERACTION_ATTR));
				ia.setInteraction(interaction);
			}

			if (atts.getValue(XMLConstants.SCALE_ATTR) != null && actor instanceof SpriteActor) {
				float s = Float.parseFloat(atts.getValue(XMLConstants.SCALE_ATTR));
				((SpriteActor) actor).setScale(s);
			}
		}

		scene.addActor(actor);
	}

	private void parseLayer(Attributes atts) throws SAXException {
		SceneLayer layer = new SceneLayer();

		layer.setName(atts.getValue(XMLConstants.ID_ATTR));
		layer.setVisible(Boolean.parseBoolean(atts.getValue(XMLConstants.VISIBLE_ATTR)));
		layer.setDynamic(Boolean.parseBoolean(atts.getValue(XMLConstants.DYNAMIC_ATTR)));

		scene.addLayer(layer);
	}

	private void parseOption(Attributes atts) throws SAXException {
		String text = atts.getValue(XMLConstants.TEXT_ATTR);
		String responseText = atts.getValue(XMLConstants.RESPONSE_TEXT_ATTR);
		String verb = atts.getValue(XMLConstants.VERB_ATTR);
		String next = atts.getValue(XMLConstants.NEXT_ATTR);
		String visibleStr = atts.getValue(XMLConstants.VISIBLE_ATTR);
		String onceStr = atts.getValue(XMLConstants.ONCE_ATTR);

		if (verb != null && verb.trim().isEmpty())
			verb = null;

		if (text == null || text.trim().isEmpty()) {
			SAXParseException e2 = new SAXParseException("'text' atribute mandatory for <option> tag", locator);
			error(e2);
			throw e2;
		}

		DialogOption o = new DialogOption();
		o.setText(text);
		o.setResponseText(responseText);
		o.setVerbId(verb);
		o.setNext(next);

		if (visibleStr != null && !visibleStr.trim().isEmpty()) {
			o.setVisible(Boolean.parseBoolean(visibleStr));
		}

		if (onceStr != null && !onceStr.trim().isEmpty()) {
			o.setOnce(Boolean.parseBoolean(onceStr));
		}

		currentDialog.addOption(o);
	}

	private void parseAnimation(Attributes atts) throws SAXException {

		if (actor == null || !(actor instanceof SpriteActor)) {
			SAXParseException e = new SAXParseException("'animation' TAG must be inside sprite actors", locator);
			error(e);
			throw e;
		}

		String speedstr = atts.getValue(XMLConstants.SPEED_ATTR);
		String animationTypestr = atts.getValue(XMLConstants.ANIMATION_TYPE_ATTR);
		String delaystr = atts.getValue(XMLConstants.DELAY_ATTR);
		String countstr = atts.getValue(XMLConstants.COUNT_ATTR);
		String soundId = atts.getValue(XMLConstants.SOUND_ATTR);
		String inDstr = atts.getValue(XMLConstants.IND_ATTR);
		String outDstr = atts.getValue(XMLConstants.OUTD_ATTR);
		String preloadstr = atts.getValue(XMLConstants.PRELOAD_ATTR);
		String disposewhenplayedstr = atts.getValue(XMLConstants.DISPOSE_WHEN_PLAYED_ATTR);

		float speed = 1f;
		float delay = 0f;
		Tween.Type animationType;
		int count = Tween.INFINITY;
		Vector2 inD = null, outD = null;
		boolean preload = true;
		boolean disposeWhenPlayed = false;

		String id = atts.getValue(XMLConstants.ID_ATTR);

		if (id == null || id.isEmpty()) {
			SAXParseException e = new SAXParseException("Animation 'id' not found or empty", locator);
			error(e);
			throw e;
		}

		String source = atts.getValue(XMLConstants.SOURCE_ATTR);
		if (source == null || source.isEmpty()) {
			SAXParseException e2 = new SAXParseException("Source name not found or empty", locator);
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

			if (disposewhenplayedstr != null && !disposewhenplayedstr.isEmpty())
				disposeWhenPlayed = Boolean.parseBoolean(disposewhenplayedstr);

			if (inDstr != null && !inDstr.isEmpty()) {
				inD = Param.parseVector2(inDstr);
			}

			if (outDstr != null && !outDstr.isEmpty()) {
				outD = Param.parseVector2(outDstr);
			}

		} catch (NumberFormatException e) {
			SAXParseException e2 = new SAXParseException("Wrong Sprite Animation parameters", locator, e);
			error(e2);
			throw e2;
		}

		if (animationTypestr == null || animationTypestr.isEmpty()
				|| animationTypestr.equalsIgnoreCase(XMLConstants.REPEAT_VALUE)) {
			animationType = Tween.Type.REPEAT;
		} else if (animationTypestr.equalsIgnoreCase(XMLConstants.REVERSE_VALUE)) {
			animationType = Tween.Type.REVERSE;
		} else if (animationTypestr.equalsIgnoreCase(XMLConstants.YOYO_VALUE)) {
			animationType = Tween.Type.YOYO;
		} else {
			animationType = Tween.Type.NO_REPEAT;
		}

		AnimationDesc sa = null;

		boolean isSpine = false;

		ActorRenderer renderer = ((SpriteActor) actor).getRenderer();

		if (renderer instanceof AtlasRenderer) {
			sa = new AtlasAnimationDesc();
		} else if (renderer instanceof ImageRenderer || renderer instanceof Sprite3DRenderer) {
			sa = new AnimationDesc();
		} else {

			try {
				isSpine = ClassReflection.isAssignableFrom(((SpriteActor) actor).getRenderer().getClass(),
						ClassReflection.forName("com.bladecoder.engine.spine.SpineRenderer"));
			} catch (ReflectionException e) {
				EngineLogger.debug("Spine plugin not found: " + e.getMessage());
			}

			if (isSpine) {
				sa = new SpineAnimationDesc();
				((SpineAnimationDesc) sa).atlas = atts.getValue(XMLConstants.ATLAS_VALUE);
			} else {
				sa = new AnimationDesc();
			}
		}

		sa.set(id, source, speed, delay, count, animationType, soundId, inD, outD, preload, disposeWhenPlayed);

		((SpriteActor) actor).getRenderer().addAnimation(sa);
	}

	private void parseSound(Attributes atts, InteractiveActor actor) throws SAXException {
		String id = atts.getValue(XMLConstants.ID_ATTR);
		String filename = atts.getValue(XMLConstants.FILENAME_ATTR);
		String loopStr = atts.getValue(XMLConstants.LOOP_ATTR);
		String volumeStr = atts.getValue(XMLConstants.VOLUME_ATTR);

		boolean loop = false;
		float volume = 1f;

		if (filename == null || filename.isEmpty())
			error(new SAXParseException("Sound 'filename' not found or empty", locator));

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

	private void parseVerb(Attributes atts, VerbManager v) {

		String id = atts.getValue(XMLConstants.ID_ATTR);
		String target = atts.getValue(XMLConstants.TARGET_ATTR);
		String state = atts.getValue(XMLConstants.STATE_ATTR);

		if (target != null)
			id = id + "." + target;

		if (state != null)
			id = id + "." + state;

		currentVerb = new Verb(id);

		v.addVerb(id, currentVerb);
	}

	private final HashMap<String, String> actionParams = new HashMap<String, String>();

	private void parseAction(Attributes atts, String actor) {

		String actionName = null;
		Action action = null;
		String actionClass = null;
		actionParams.clear();

		for (int i = 0; i < atts.getLength(); i++) {
			String attName = atts.getLocalName(i);

			if (attName.equals(XMLConstants.CLASS_ATTR)) {
				actionClass = atts.getValue(attName);
			} else if (attName.equals(XMLConstants.ACTION_NAME_ATTR)) {
				actionName = atts.getValue(attName);
			} else if (attName.equals(XMLConstants.ACTION_ENABLED_ATTR)) {
				if (atts.getValue(attName).equals(XMLConstants.FALSE_VALUE))
					return;
			} else {
				String value = atts.getValue(attName);

				actionParams.put(attName, value);
			}
		}

		if (atts.getValue("", XMLConstants.ACTOR_TAG) == null)
			actionParams.put(XMLConstants.ACTOR_TAG, actor);

		if (actionClass != null) {
			action = ActionFactory.createByClass(actionClass, actionParams);
		} else if (actionName != null) {
			action = ActionFactory.create(actionName, actionParams);
		}

		if (action != null) {
			currentVerb.add(action);
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
		if (actor != null)
			EngineLogger.error(MessageFormat.format("{0}.{1} Line: {2} Column: {3}. {4}", scene.getId(), actor.getId(),
					e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
		else if (scene != null)
			EngineLogger.error(MessageFormat.format("{0} Line: {1} Column: {2}. {3}", actor.getId(), e.getLineNumber(),
					e.getColumnNumber(), e.getMessage()));
		else
			EngineLogger.error(MessageFormat.format("Line: {1} Column: {2}. {3}", e.getLineNumber(),
					e.getColumnNumber(), e.getMessage()));

		EngineLogger.error("CAUSA", (Exception) e.getCause());
	}

}
