package org.bladecoder.engineeditor.model;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.AtlasRenderer;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpineRenderer;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteActor.DepthType;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.util.EngineLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class ChapterDocument extends BaseDocument {

	public static final String BACKGROUND_ACTOR_TYPE = "background";
	public static final String ATLAS_ACTOR_TYPE = "atlas";
	public static final String SPRITE3D_ACTOR_TYPE = "3d";
	public static final String SPINE_ACTOR_TYPE = "spine";
	public static final String FOREGROUND_ACTOR_TYPE = "foreground";

	public static final String ACTOR_TYPES[] = { BACKGROUND_ACTOR_TYPE,
			ATLAS_ACTOR_TYPE, SPINE_ACTOR_TYPE, SPRITE3D_ACTOR_TYPE,
			FOREGROUND_ACTOR_TYPE };

	public static final String ANIMATION_TYPES[] = { "no_repeat", "repeat",
			"yoyo", "reverse" };

	public ChapterDocument(String modelPath) {
		super();
		setModelPath(modelPath);
	}

	@Override
	public String getRootTag() {
		return "chapter";
	}

	public void setFilenameFromId() {
		setFilename(getId() + ".chapter");
	}

	public Element getActor(Element scn, String id) {
		NodeList actorsNL = getActors(scn);
		for (int j = 0; j < actorsNL.getLength(); j++) {
			Element e = (Element) actorsNL.item(j);
			if (id.equals(e.getAttribute("id"))) {
				return e;
			}
		}

		return null;
	}

	public NodeList getActors(Element scn) {
		NodeList actors = scn
				.getElementsByTagName("actor");

		return actors;
	}
	
	public NodeList getScenes() {
		NodeList s = getElement()
				.getElementsByTagName("scene");

		return s;
	}

	public void createActor(Element scn, String id, String type) {
		Element e = doc.createElement("actor");

		scn.appendChild(e);
		setActorId(scn, e, id);
		e.setAttribute("type", type);

		if (type.equals(BACKGROUND_ACTOR_TYPE))
			setBbox(e, new Rectangle(0, 0, 100, 100));

		modified = true;
		firePropertyChange(ChapterDocument.NOTIFY_ELEMENT_CREATED, e);
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
		NodeList nl = scn.getElementsByTagName("player");

		if (nl.getLength() == 0)
			return null;
		else
			return (Element) nl.item(0);
	}

	public void setRootAttr(Element scn, String attr, String value) {
		Element e = scn;

		String old = e.getAttribute(attr);

		if (value != null && !value.isEmpty())
			e.setAttribute(attr, value);
		else
			e.removeAttribute(attr);

		modified = true;
		firePropertyChange(attr, old, value);
	}

	public String getMusic(Element scn) {
		return scn.getAttribute("music");
	}

	public void setMusic(Element scn, String filename, String loopMusic,
			String initialMusicDelay, String repeatMusicDelay) {
		if (filename != null && !filename.isEmpty())
			scn.setAttribute("music", filename);
		else
			scn.removeAttribute("music");

		if (loopMusic != null && !loopMusic.isEmpty())
			scn.setAttribute("loop_music", loopMusic);
		else
			scn.removeAttribute("loop_music");

		if (initialMusicDelay != null && !initialMusicDelay.isEmpty())
			scn.setAttribute("initial_music_delay",
					initialMusicDelay);
		else
			scn.removeAttribute("initial_music_delay");

		if (repeatMusicDelay != null && !repeatMusicDelay.isEmpty())
			scn.setAttribute("repeat_music_delay",
					repeatMusicDelay);
		else
			scn.removeAttribute("repeat_music_delay");

		modified = true;
		firePropertyChange("music", scn);
	}

	public String getBackground(Element scn) {
		return scn.getAttribute("background");
	}

	public String getLightmap(Element scn) {
		return doc.getDocumentElement().getAttribute("lightmap");
	}

	public void setBackground(Element scn, String value) {
		setRootAttr(scn, "background", value);
	}

	public void setLightmap(Element scn, String value) {
		setRootAttr(scn, "lightmap", value);
	}

	public Scene getEngineScene(Element s, int wWidth, int wHeight) {
		Scene scn = new Scene();

		scn.setId(getId());

		scn.getCamera().create(wWidth, wHeight);

		String background = getBackground(s);
		if (background != null && !background.isEmpty()) {
			scn.setBackground(background, getLightmap(s));
		}

		String depthVector = s.getAttribute("depth_vector");
		if (!depthVector.isEmpty())
			scn.setDepthVector(Param.parseVector2(depthVector));

		// GET ACTORS
		NodeList actors = getActors(s);
		for (int i = 0; i < actors.getLength(); i++) {
			Element a = (Element) actors.item(i);
			Actor actor = getEngineActor(a);

			if (getType(a).equals("foreground")) {
				scn.addFgActor((SpriteActor) actor);
			} else {
				scn.addActor(actor);
			}

			if (getId(a).equals(getRootAttr("player"))) {
				scn.setPlayer((SpriteActor) actor);
			}
		}

		return scn;
	}

	public void create(String id) throws ParserConfigurationException,
			FileNotFoundException, TransformerException {
		create();
		setId(id);
		setFilenameFromId();
		save();
	}

	public void rename(String newId) throws FileNotFoundException,
			TransformerException {

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
		return doc.getDocumentElement().getAttribute("id");
	}

	public void setId(String id) {
		setRootAttr(doc.getDocumentElement(), "id", id);
	}

	public Rectangle getBBox() {
		return getBBox(doc.getDocumentElement());
	}

	public String toString() {
		return getId();
	}

	public void setBbox(Rectangle bbox) {
		setBbox(doc.getDocumentElement(), bbox);
	}

	public ChapterDocument cloneScene() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChapterDocument pasteScene() {
		// TODO Auto-generated method stub
		return null;
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
				String id2 = ae.getAttribute("id");

				if (id2.equals(idChecked) && e != ae) {
					i++;
					idChecked = id + i;
					checked = false;
					break;
				}
			}
		}

		setRootAttr(e, "id", idChecked);
	}

	public String getDesc(Element e) {
		return getTranslation(e.getAttribute("desc"));
	}

	public NodeList getFrameAnimations(Element e) {
		return e.getElementsByTagName("frame_animation");
	}

	public Rectangle getBBox(Element e) {
		if (e.getAttribute("x").isEmpty() || e.getAttribute("y").isEmpty()
				|| e.getAttribute("width").isEmpty()
				|| e.getAttribute("height").isEmpty())
			return null;

		float x = Float.parseFloat(e.getAttribute("x"));
		float y = Float.parseFloat(e.getAttribute("y"));
		float width = Float.parseFloat(e.getAttribute("width"));
		float height = Float.parseFloat(e.getAttribute("height"));

		return new Rectangle(x, y, width, height);
	}

	public Actor getEngineActor(Element e) {
		Actor a = null;

		String type = getType(e);

		if (type.equals(ATLAS_ACTOR_TYPE) || type.equals(FOREGROUND_ACTOR_TYPE)) {
			a = new SpriteActor();
			((SpriteActor) a).setRenderer(new AtlasRenderer());
		} else if (type.equals(SPRITE3D_ACTOR_TYPE)) {
			a = new SpriteActor();
			Sprite3DRenderer r = new Sprite3DRenderer();
			((SpriteActor) a).setRenderer(r);
			r.setSpriteSize(Param.parseVector2(e
					.getAttribute("sprite_size")));
			
		} else if (type.equals(SPINE_ACTOR_TYPE)) {
			a = new SpriteActor();
			((SpriteActor) a).setRenderer(new SpineRenderer());
		} else if (type.equals(BACKGROUND_ACTOR_TYPE)) {
			a = new Actor();
		} else {
			EngineLogger.error(" Wrong actor Type defined in XML");
			return null;
		}

		a.setId(getId(e));
		Rectangle bbox = getBBox(e);
		a.setBbox(bbox);
		a.setDesc(e.getAttribute("desc"));

		if (a instanceof SpriteActor) {
			SpriteRenderer r = ((SpriteActor) a).getRenderer();

			NodeList faList = getFrameAnimations(e);

			for (int i = 0; i < faList.getLength(); i++) {
				Element faElement = (Element) faList.item(i);

				FrameAnimation fa = getEngineFA(type, faElement);

				r.addFrameAnimation(fa);
			}

			Vector2 pos = getPos(e);
			((SpriteActor) a).setPosition(pos.x, pos.y);

			if (!e.getAttribute("init_frame_animation").isEmpty()) {
				((SpriteActor) a).getRenderer().setInitFrameAnimation(
						e.getAttribute("init_frame_animation"));

			}

			// PARSE DEPTH MAP USE
			String depthType = e.getAttribute("depth_type");
			((SpriteActor) a).setDepthType(DepthType.NONE);

			if (!depthType.isEmpty()) {
				if (depthType.equals("map"))
					((SpriteActor) a).setDepthType(DepthType.MAP);
				else if (depthType.equals("vector"))
					((SpriteActor) a).setDepthType(DepthType.VECTOR);
			}
		}

		return a;
	}

	public FrameAnimation getEngineFA(String type, Element faElement) {
		FrameAnimation fa;

		if(type.equals(ATLAS_ACTOR_TYPE)||type.equals(FOREGROUND_ACTOR_TYPE)) {
			fa = new AtlasFrameAnimation();	
		} else {
			fa = new FrameAnimation();
		}
			
		fa.id = faElement.getAttribute("id");
		fa.source = faElement.getAttribute("source");

		if (faElement.getAttribute("animation_type").isEmpty()
				|| faElement.getAttribute("animation_type").equalsIgnoreCase(
						"repeat")) {
			fa.animationType = Tween.REPEAT;
		} else if (faElement.getAttribute("animation_type").equalsIgnoreCase(
				"yoyo")) {
			fa.animationType = Tween.PINGPONG;
		} else {
			fa.animationType = Tween.NO_REPEAT;
		}

		if (!faElement.getAttribute("speed").isEmpty())
			fa.duration = Float.parseFloat(faElement.getAttribute("speed"));

		if (!faElement.getAttribute("delay").isEmpty())
			fa.delay = Float.parseFloat(faElement.getAttribute("delay"));

		if (!faElement.getAttribute("count").isEmpty())
			fa.count = Integer.parseInt(faElement.getAttribute("count"));
		else
			fa.count = Tween.INFINITY;

		if (!faElement.getAttribute("sound").isEmpty())
			fa.sound = faElement.getAttribute("sound");

		if (!faElement.getAttribute("inD").isEmpty()) {
			fa.inD = Param.parseVector2(faElement.getAttribute("inD"));
		}

		if (!faElement.getAttribute("outD").isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute("outD"));
		}
		
		if (!faElement.getAttribute("preload").isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute("preload"));
		}
		
		if (!faElement.getAttribute("dispose_when_played").isEmpty()) {
			fa.outD = Param.parseVector2(faElement.getAttribute("dispose_when_played"));
		}

		return fa;
	}

	public Vector2 getPos(Element e) {
		return Param.parseVector2(e.getAttribute("pos"));
	}

	public void setPos(Element e, Vector2 pos) {
		if (pos == null) {
			e.removeAttribute("pos");
		} else {
			e.setAttribute("pos", Param.toStringParam(pos));
		}

		modified = true;
		firePropertyChange("pos", e);
	}

	public NodeList getDialogs(Element e) {
		return e.getElementsByTagName("dialog");
	}

	public void setBbox(Element e, Rectangle bbox) {
		if (bbox == null) {
			e.removeAttribute("x");
			e.removeAttribute("y");
			e.removeAttribute("width");
			e.removeAttribute("height");
		} else {
			e.setAttribute("x", Float.toString(bbox.x));
			e.setAttribute("y", Float.toString(bbox.y));
			e.setAttribute("width", Float.toString(bbox.width));
			e.setAttribute("height", Float.toString(bbox.height));
		}

		modified = true;
		firePropertyChange("bbox", e);
	}

	public Element createDialogOption(Element parent, String text,
			String responseText, String verb, String next, String visible) {
		Element e = doc.createElement("option");
		e.setAttribute("text", text);
		if (responseText != null && !responseText.isEmpty())
			e.setAttribute("responseText", responseText);
		if (verb != null && !verb.isEmpty())
			e.setAttribute("verb", verb);
		if (next != null && !next.isEmpty())
			e.setAttribute("next", next);
		if (visible != null && !visible.isEmpty())
			e.setAttribute("visible", visible);

		parent.appendChild(e);

		modified = true;
		firePropertyChange("option", e);

		return e;
	}
}
