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

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.SpriteActor.DepthType;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.EngineLogger;

public class ChapterDocument extends BaseDocument {

	public static final String ACTOR_TYPES[] = { XMLConstants.NO_RENDERER_VALUE, XMLConstants.ATLAS_VALUE, XMLConstants.SPINE_VALUE,
		XMLConstants.S3D_VALUE, XMLConstants.IMAGE_VALUE };

	public static final String ANIMATION_TYPES[] = { XMLConstants.NO_REPEAT_VALUE, XMLConstants.REPEAT_VALUE, XMLConstants.YOYO_VALUE, XMLConstants.REVERSE_VALUE };

	public ChapterDocument(String modelPath) {
		super();
		setModelPath(modelPath);
	}

	@Override
	public String getRootTag() {
		return XMLConstants.CHAPTER_TAG;
	}

	public void setFilenameFromId() {
		setFilename(getId() + XMLConstants.CHAPTER_EXT);
	}

	public Element getActor(Element scn, String id) {
		NodeList actorsNL = getActors(scn);
		for (int j = 0; j < actorsNL.getLength(); j++) {
			Element e = (Element) actorsNL.item(j);
			if (id.equals(e.getAttribute(XMLConstants.ID_ATTR))) {
				return e;
			}
		}

		return null;
	}

	public NodeList getActors(Element scn) {
		NodeList actors = scn.getElementsByTagName(XMLConstants.ACTOR_TAG);

		return actors;
	}
	
	public NodeList getLayers(Element scn) {
		NodeList actors = scn.getElementsByTagName(XMLConstants.LAYER_TAG);

		return actors;
	}

	public NodeList getActions(Element verb) {
		NodeList actions = verb.getElementsByTagName(XMLConstants.ACTION_TAG);

		return actions;
	}

	public NodeList getScenes() {
		NodeList s = getElement().getElementsByTagName(XMLConstants.SCENE_TAG);

		return s;
	}

	public void addActor(Element scn, Element e) {

		scn.appendChild(e);

		modified = true;
		firePropertyChange(ChapterDocument.NOTIFY_ELEMENT_CREATED, e);
	}

	public void removeActor(Element scn, String id) {
		Element e = getActor(scn, id);

		deleteElement(e);
	}

	public Element getPlayer(Element scn) {
		NodeList nl = scn.getElementsByTagName(XMLConstants.PLAYER_ATTR);

		if (nl.getLength() == 0)
			return null;
		else
			return (Element) nl.item(0);
	}

	public void setRootAttr(Element e, String attr, String value) {

		String old = e.getAttribute(attr);

		if (value != null && !value.isEmpty())
			e.setAttribute(attr, value);
		else
			e.removeAttribute(attr);

		modified = true;
		firePropertyChange(attr, old, value);
	}

	public String getMusic(Element scn) {
		return scn.getAttribute(XMLConstants.MUSIC_ATTR);
	}

	public void setMusic(Element scn, String filename, String loopMusic, String initialMusicDelay,
			String repeatMusicDelay) {
		if (filename != null && !filename.isEmpty())
			scn.setAttribute(XMLConstants.MUSIC_ATTR, filename);
		else
			scn.removeAttribute(XMLConstants.MUSIC_ATTR);

		if (loopMusic != null && !loopMusic.isEmpty())
			scn.setAttribute(XMLConstants.LOOP_MUSIC_ATTR, loopMusic);
		else
			scn.removeAttribute(XMLConstants.LOOP_MUSIC_ATTR);

		if (initialMusicDelay != null && !initialMusicDelay.isEmpty())
			scn.setAttribute(XMLConstants.INITIAL_MUSIC_DELAY_ATTR, initialMusicDelay);
		else
			scn.removeAttribute(XMLConstants.INITIAL_MUSIC_DELAY_ATTR);

		if (repeatMusicDelay != null && !repeatMusicDelay.isEmpty())
			scn.setAttribute(XMLConstants.REPEAT_MUSIC_DELAY_ATTR, repeatMusicDelay);
		else
			scn.removeAttribute(XMLConstants.REPEAT_MUSIC_DELAY_ATTR);

		modified = true;
		firePropertyChange(XMLConstants.MUSIC_ATTR, scn);
	}

	public Scene getEngineScene(Element s, int wWidth, int wHeight) {
		Scene scn = new Scene();

		scn.setId(getId());

		scn.getCamera().create(wWidth, wHeight);

		String background = s.getAttribute(XMLConstants.BACKGROUND_ATLAS_ATTR);
		if (background != null && !background.isEmpty()) {
			scn.setBackground(s.getAttribute(XMLConstants.BACKGROUND_ATLAS_ATTR), s.getAttribute(XMLConstants.BACKGROUND_REGION_ATTR), 
					s.getAttribute(XMLConstants.LIGHTMAP_ATLAS_ATTR), s.getAttribute(XMLConstants.LIGHTMAP_REGION_ATTR));
		}

		String depthVector = s.getAttribute(XMLConstants.DEPTH_VECTOR_ATTR);
		if (!depthVector.isEmpty())
			scn.setDepthVector(Param.parseVector2(depthVector));
		
		// LAYERS
		NodeList layers = getLayers(s);
		for (int i = 0; i < layers.getLength(); i++) {
			Element l = (Element) layers.item(i);
			SceneLayer layer = new SceneLayer();
			layer.setName(l.getAttribute(XMLConstants.ID_ATTR));
			layer.setVisible(Boolean.parseBoolean(l.getAttribute(XMLConstants.VISIBLE_ATTR)));
			layer.setDynamic(Boolean.parseBoolean(l.getAttribute(XMLConstants.DYNAMIC_ATTR)));
			scn.addLayer(layer);
		}

		// GET ACTORS
		NodeList actors = getActors(s);
		for (int i = 0; i < actors.getLength(); i++) {
			Element a = (Element) actors.item(i);
			BaseActor actor = getEngineActor(a);
			scn.addActor(actor);

			if (getId(a).equals(getRootAttr(XMLConstants.PLAYER_ATTR))) {
				scn.setPlayer((SpriteActor) actor);
			}
		}

		// WALK ZONE
		Element wz = getWalkZone(s);

		if (wz != null) {
			PolygonalNavGraph polygonalPathFinder = new PolygonalNavGraph();
			polygonalPathFinder.setWalkZone(Param.parsePolygon(wz.getAttribute(XMLConstants.POLYGON_ATTR), wz.getAttribute(XMLConstants.POS_ATTR)));

			scn.setPolygonalNavGraph(polygonalPathFinder);

			NodeList obstacles = wz.getElementsByTagName(XMLConstants.OBSTACLE_TAG);
			for (int i = 0; i < obstacles.getLength(); i++) {
				Element o = (Element) obstacles.item(i);

				polygonalPathFinder.addObstacle(Param.parsePolygon(o.getAttribute(XMLConstants.POLYGON_ATTR), o.getAttribute(XMLConstants.POS_ATTR)));
			}
		}
		
		scn.orderLayersByZIndex();

		return scn;
	}

	public void create(String id) throws ParserConfigurationException, FileNotFoundException, TransformerException {
		create();
		setId(id);
		setFilenameFromId();
		save();
	}

	public void rename(String newId) throws FileNotFoundException, TransformerException {

		deleteFiles();

		setId(newId);
		setFilenameFromId();

		save();
	}

	public void deleteFiles() {
		File f = new File(getAbsoluteName());

		f.delete();

		// TODO delete all .properties
		f = new File(getI18NFilename());
		f.delete();
	}

	public String getId() {
		return doc.getDocumentElement().getAttribute(XMLConstants.ID_ATTR);
	}

	public void setId(String id) {
		setRootAttr(doc.getDocumentElement(), XMLConstants.ID_ATTR, id);
	}

	public String toString() {
		return getId();
	}

	/*********************** ELEMENT METHODS *************************/

	/**
	 * Sets the actor id avoiding duplicated ids
	 * 
	 * @param e
	 * @param id
	 */
	public void setActorId(Element scn, Element e, String id) {
		NodeList actors = getActors(scn);

		boolean checked = false;

		int i = 1;

		String idChecked = id;

		while (!checked) {
			checked = true;

			for (int j = 0; j < actors.getLength(); j++) {
				Element ae = (Element) actors.item(j);
				String id2 = ae.getAttribute(XMLConstants.ID_ATTR);

				if (id2.equals(idChecked) && e != ae) {
					i++;
					idChecked = id + i;
					checked = false;
					break;
				}
			}
		}

		setRootAttr(e, XMLConstants.ID_ATTR, idChecked);
	}

	public String getDesc(Element e) {
		return getTranslation(e.getAttribute(XMLConstants.DESC_ATTR));
	}

	public NodeList getAnimations(Element e) {
		return e.getElementsByTagName(XMLConstants.ANIMATION_TAG);
	}

	public Polygon getBBox(Element e) {
		if (e.getAttribute(XMLConstants.BBOX_ATTR).isEmpty())
			return null;

		return Param.parsePolygon(e.getAttribute(XMLConstants.BBOX_ATTR));
	}

	public BaseActor getEngineActor(Element e) {
		BaseActor a = null;

		String type = getType(e);

		if (type.equals(XMLConstants.ATLAS_VALUE)) {
			a = new SpriteActor();
			((SpriteActor) a).setRenderer(new AtlasRenderer());
		} else if (type.equals(XMLConstants.S3D_VALUE)) {
			a = new SpriteActor();
			Sprite3DRenderer r = new Sprite3DRenderer();
			((SpriteActor) a).setRenderer(r);
			r.setSpriteSize(Param.parseVector2(e.getAttribute(XMLConstants.SPRITE_SIZE_ATTR)));

		} else if (type.equals(XMLConstants.SPINE_VALUE)) {
			a = new SpriteActor();
			SpineRenderer r = new SpineRenderer();
			r.enableEvents(false);
			((SpriteActor) a).setRenderer(r);
		} else if (type.equals(XMLConstants.IMAGE_VALUE)) {
			a = new SpriteActor();
			((SpriteActor) a).setRenderer(new ImageRenderer());
		} else if (type.equals(XMLConstants.NO_RENDERER_VALUE)) {
			a = new BaseActor();
		} else {
			EngineLogger.error(" Wrong actor Type defined in XML");
			return null;
		}

		String layer = e.getAttribute(XMLConstants.LAYER_ATTR);
		a.setLayer(layer);

		a.setId(getId(e));
		Polygon bbox = getBBox(e);
		a.setBbox(bbox);

		if (bbox == null) {
			bbox = new Polygon();
			a.setBbox(bbox);

			if (a instanceof SpriteActor)
				((SpriteActor) a).setBboxFromRenderer(true);
		}

		Vector2 pos = getPos(e);
		if (pos != null)
			a.setPosition(pos.x, pos.y);

		a.setDesc(e.getAttribute(XMLConstants.DESC_ATTR));

		if (a instanceof SpriteActor) {
			ActorRenderer r = ((SpriteActor) a).getRenderer();

			NodeList faList = getAnimations(e);

			for (int i = 0; i < faList.getLength(); i++) {
				Element faElement = (Element) faList.item(i);

				AnimationDesc fa = getEngineFA(type, faElement);

				r.addAnimation(fa);
			}

			if (!e.getAttribute(XMLConstants.INIT_ANIMATION_ATTR).isEmpty()) {
				((SpriteActor) a).getRenderer().setInitAnimation(e.getAttribute(XMLConstants.INIT_ANIMATION_ATTR));

			}
			
			if (!e.getAttribute(XMLConstants.SCALE_ATTR).isEmpty()) {
				((SpriteActor) a).setScale(Float.parseFloat(e.getAttribute(XMLConstants.SCALE_ATTR)));
			}

			// PARSE DEPTH TYPE
			String depthType = e.getAttribute(XMLConstants.DEPTH_TYPE_ATTR);
			((SpriteActor) a).setDepthType(DepthType.NONE);

			if (!depthType.isEmpty()) {
				if (depthType.equals(XMLConstants.VECTOR_ATTR))
					((SpriteActor) a).setDepthType(DepthType.VECTOR);
			}
		}
		
		if (e.getAttribute(XMLConstants.OBSTACLE_ATTR).equals(XMLConstants.TRUE_VALUE))
			a.setWalkObstacle(true);
		
		if (!e.getAttribute(XMLConstants.ZINDEX_ATTR).isEmpty()) {
			a.setZIndex(Float.parseFloat(e.getAttribute(XMLConstants.ZINDEX_ATTR)));
		}

		return a;
	}

	public AnimationDesc getEngineFA(String type, Element faElement) {
		AnimationDesc fa;

		if (type.equals(XMLConstants.ATLAS_VALUE)) {
			fa = new AtlasAnimationDesc();
		} else {
			fa = new AnimationDesc();
		}

		fa.id = faElement.getAttribute(XMLConstants.ID_ATTR);
		fa.source = faElement.getAttribute(XMLConstants.SOURCE_ATTR);

		if (faElement.getAttribute(XMLConstants.ANIMATION_TYPE_ATTR).isEmpty()
				|| faElement.getAttribute(XMLConstants.ANIMATION_TYPE_ATTR).equalsIgnoreCase(XMLConstants.REPEAT_VALUE)) {
			fa.animationType = Tween.REPEAT;
		} else if (faElement.getAttribute(XMLConstants.ANIMATION_TYPE_ATTR).equalsIgnoreCase(XMLConstants.YOYO_VALUE)) {
			fa.animationType = Tween.PINGPONG;
		} else {
			fa.animationType = Tween.NO_REPEAT;
		}

		if (!faElement.getAttribute(XMLConstants.SPEED_ATTR).isEmpty())
			fa.duration = Float.parseFloat(faElement.getAttribute(XMLConstants.SPEED_ATTR));

		if (!faElement.getAttribute(XMLConstants.DELAY_ATTR).isEmpty())
			fa.delay = Float.parseFloat(faElement.getAttribute(XMLConstants.DELAY_ATTR));

		if (!faElement.getAttribute(XMLConstants.COUNT_ATTR).isEmpty())
			fa.count = Integer.parseInt(faElement.getAttribute(XMLConstants.COUNT_ATTR));
		else
			fa.count = Tween.INFINITY;

		if (!faElement.getAttribute(XMLConstants.SOUND_ATTR).isEmpty())
			fa.sound = faElement.getAttribute(XMLConstants.SOUND_ATTR);

		if (!faElement.getAttribute(XMLConstants.IND_ATTR).isEmpty()) {
			fa.inD = Param.parseVector2(faElement.getAttribute(XMLConstants.IND_ATTR));
		}

		if (!faElement.getAttribute(XMLConstants.OUTD_ATTR).isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute(XMLConstants.OUTD_ATTR));
		}

		if (!faElement.getAttribute(XMLConstants.PRELOAD_ATTR).isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute(XMLConstants.PRELOAD_ATTR));
		}

		if (!faElement.getAttribute(XMLConstants.DISPOSE_WHEN_PLAYED_ATTR).isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute(XMLConstants.DISPOSE_WHEN_PLAYED_ATTR));
		}

		return fa;
	}

	public Vector2 getPos(Element e) {
		return Param.parseVector2(e.getAttribute(XMLConstants.POS_ATTR));
	}

	public void setPos(Element e, Vector2 pos) {
		if (pos == null) {
			e.removeAttribute(XMLConstants.POS_ATTR);
		} else {
			e.setAttribute(XMLConstants.POS_ATTR, Param.toStringParam(pos));
		}

		modified = true;
		firePropertyChange(XMLConstants.POS_ATTR, e);
	}

	public NodeList getDialogs(Element e) {
		return e.getElementsByTagName(XMLConstants.DIALOG_TAG);
	}

	public void setBbox(Element e, Polygon p) {
		if (p == null) {
			p = new Polygon();

			float[] verts = new float[8];

			verts[0] = 0f;
			verts[1] = 0f;
			verts[2] = 0f;
			verts[3] = 200;
			verts[4] = 200;
			verts[5] = 200;
			verts[6] = 200;
			verts[7] = 0f;

			p.setVertices(verts);
		}

		e.setAttribute(XMLConstants.BBOX_ATTR, Param.toStringParam(p));

		modified = true;
		firePropertyChange(XMLConstants.BBOX_ATTR, e);
	}

	public Element createDialogOption(Element parent, String text, String responseText, String verb, String next,
			String visible) {
		Element e = doc.createElement(XMLConstants.OPTION_TAG);
		e.setAttribute(XMLConstants.TEXT_ATTR, text);
		if (responseText != null && !responseText.isEmpty())
			e.setAttribute(XMLConstants.RESPONSE_TEXT_ATTR, responseText);
		if (verb != null && !verb.isEmpty())
			e.setAttribute(XMLConstants.VERB_ATTR, verb);
		if (next != null && !next.isEmpty())
			e.setAttribute(XMLConstants.NEXT_ATTR, next);
		if (visible != null && !visible.isEmpty())
			e.setAttribute(XMLConstants.VISIBLE_ATTR, visible);

		parent.appendChild(e);

		modified = true;
		firePropertyChange(XMLConstants.OPTION_TAG, e);

		return e;
	}

	public Element createWalkZone(Element scn, Polygon poly) {
		Element e = doc.createElement(XMLConstants.WALK_ZONE_TAG);
		e.setAttribute(XMLConstants.POLYGON_ATTR, Param.toStringParam(poly));
		e.setAttribute(XMLConstants.POS_ATTR, Param.toStringParam(new Vector2(poly.getX(), poly.getY())));

		scn.appendChild(e);

		modified = true;
		firePropertyChange(XMLConstants.WALK_ZONE_TAG, e);

		return e;
	}

	public void setWalkZonePolygon(Element scn, Polygon poly) {
		Element e = getWalkZone(scn);

		if (e == null)
			e = createWalkZone(scn, poly);
		else {
			e.setAttribute(XMLConstants.POLYGON_ATTR, Param.toStringParam(poly));
			e.setAttribute(XMLConstants.POS_ATTR, Param.toStringParam(new Vector2(poly.getX(), poly.getY())));
		}

		modified = true;
		firePropertyChange(XMLConstants.WALK_ZONE_TAG, e);

	}

	public Element getWalkZone(Element scn) {
		NodeList nl = scn.getElementsByTagName(XMLConstants.WALK_ZONE_TAG);
		Element e = null;

		if (nl.getLength() > 0) {
			e = (Element) nl.item(0);
		}

		return e;
	}

	public void deleteWalkZone(Element scn) {
		Element e = getWalkZone(scn);

		if (e != null) {
			deleteElement(e);
		}

		modified = true;
		firePropertyChange(XMLConstants.WALK_ZONE_TAG, e);
	}

	public Element createObstacle(Element scn, Polygon poly) {
		Element e = doc.createElement(XMLConstants.OBSTACLE_ATTR);
		e.setAttribute(XMLConstants.POLYGON_ATTR, Param.toStringParam(poly));
		e.setAttribute(XMLConstants.POS_ATTR, Param.toStringParam(new Vector2(poly.getX(), poly.getY())));

		getWalkZone(scn).appendChild(e);

		modified = true;
		firePropertyChange(XMLConstants.OBSTACLE_ATTR, e);

		return e;
	}

	public Element getObstacle(Element scn, int i) {
		Element wz = getWalkZone(scn);
		Element e = null;

		NodeList nl = wz.getElementsByTagName(XMLConstants.OBSTACLE_ATTR);

		e = (Element) nl.item(i);

		return e;
	}

	public void setObstaclePolygon(Element scn, int i, Polygon poly) {
		Element e = getObstacle(scn, i);

		if (e == null)
			return;

		e.setAttribute(XMLConstants.POLYGON_ATTR, Param.toStringParam(poly));
		e.setAttribute(XMLConstants.POS_ATTR, Param.toStringParam(new Vector2(poly.getX(), poly.getY())));

		modified = true;
		firePropertyChange(XMLConstants.OBSTACLE_ATTR, e);
	}

	public void deleteObstacle(Element scn, int i) {
		Element e = getObstacle(scn, i);

		if (e != null) {
			deleteElement(e);
		}

		modified = true;
		firePropertyChange(XMLConstants.OBSTACLE_ATTR, e);
	}

	public Element getSceneById(String id) {
		NodeList scenes = getScenes();
		
		for(int i = 0; i < scenes.getLength(); i++) {
			Element e = (Element) scenes.item(i);
			
			if(e.getAttribute(XMLConstants.ID_ATTR).equals(id))
				return e;
		}
		
		return null;
	}
}
