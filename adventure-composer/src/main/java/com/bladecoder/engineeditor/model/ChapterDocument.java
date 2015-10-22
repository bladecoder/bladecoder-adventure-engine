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
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.World;

public class ChapterDocument extends BaseDocument {

	public static final String ACTOR_TYPES[] = { XMLConstants.BACKGROUND_VALUE, XMLConstants.SPRITE_VALUE,
			XMLConstants.CHARACTER_VALUE, XMLConstants.OBSTACLE_VALUE, XMLConstants.ANCHOR_VALUE };

	public static final String ACTOR_RENDERERS[] = { XMLConstants.ATLAS_VALUE, XMLConstants.SPINE_VALUE,
			XMLConstants.S3D_VALUE, XMLConstants.IMAGE_VALUE };

	public static final String ANIMATION_TYPES[] = { XMLConstants.NO_REPEAT_VALUE, XMLConstants.REPEAT_VALUE,
			XMLConstants.YOYO_VALUE, XMLConstants.REVERSE_VALUE };
	
	String id;

	public ChapterDocument(String modelPath, String id) {
		super();
		setModelPath(modelPath);
		this.id = id;
		setFilename(id + XMLConstants.CHAPTER_EXT);
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
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public void getBBox(Polygon p, Element e) {
		if (e.getAttribute(XMLConstants.BBOX_ATTR).isEmpty())
			return;

		Param.parsePolygon(p, e.getAttribute(XMLConstants.BBOX_ATTR));
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

	
	@Override
	public void load() throws ParserConfigurationException, SAXException, IOException {
		super.load();	
		World.getInstance().loadChapter(id);
	}
}
