package org.bladecoder.engineeditor.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteAtlasRenderer;
import org.bladecoder.engine.model.SpriteActor.DepthType;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.util.EngineLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class SceneDocument extends BaseDocument {

	public static final String BACKGROUND_ACTOR = "background";
	public static final String SPRITE_ACTOR = "sprite";
	public static final String SPRITE3D_ACTOR = "sprite3d";
	public static final String FOREGROUND_ACTOR = "foreground";

	public static final String ACTOR_TYPES[] = { BACKGROUND_ACTOR,
			SPRITE_ACTOR, SPRITE3D_ACTOR, FOREGROUND_ACTOR };

	public static final String ANIMATION_TYPES[] = { "no_repeat", "repeat",
			"yoyo" };

	/**
	 * Stores the FA num. of frames. The XML doesn't have this information and
	 * must be retrieved from atlas by ScnCanvas.
	 */
	private HashMap<String, Integer> faFrames = new HashMap<String, Integer>();

	/**
	 * Stores the 3d Animations. The XML doesn't have this information and must
	 * be retrieved from model by ScnCanvas.
	 */
	private HashMap<String, Array<Animation>> animations3d = new HashMap<String, Array<Animation>>();

	public SceneDocument(String modelPath) {
		super();
		setModelPath(modelPath);
	}

	@Override
	public String getRootTag() {
		return "scene";
	}

	public void setFilenameFromId() {
		setFilename(getId() + ".scn");
	}

	public Element getActor(String id) {
		NodeList actorsNL = getActors();
		for (int j = 0; j < actorsNL.getLength(); j++) {
			Element e = (Element) actorsNL.item(j);
			if (id.equals(e.getAttribute("id"))) {
				return e;
			}
		}

		return null;
	}

	public NodeList getActors() {
		NodeList actors = doc.getDocumentElement()
				.getElementsByTagName("actor");

		return actors;
	}

	public void createActor(String id, String type) {
		Element e = doc.createElement("actor");

		doc.getDocumentElement().appendChild(e);
		setActorId(e, id);
		e.setAttribute("type", type);

		if (type.equals(BACKGROUND_ACTOR))
			setBbox(e, new Rectangle(0, 0, 100, 100));

		modified = true;
		firePropertyChange(SceneDocument.NOTIFY_ELEMENT_CREATED, e);
	}

	public void addActor(Element e) {

		doc.getDocumentElement().appendChild(e);

		modified = true;
		firePropertyChange(SceneDocument.NOTIFY_ELEMENT_CREATED, e);
	}

	public void removeActor(String id) {
		Element e = getActor(id);

		deleteElement(e);
	}

	public Element getPlayer() {
		NodeList nl = doc.getDocumentElement().getElementsByTagName("player");

		if (nl.getLength() == 0)
			return null;
		else
			return (Element) nl.item(0);
	}

	public void setRootAttr(String attr, String value) {
		Element e = doc.getDocumentElement();

		String old = e.getAttribute(attr);

		if (value != null && !value.isEmpty())
			e.setAttribute(attr, value);
		else
			e.removeAttribute(attr);

		modified = true;
		firePropertyChange(attr, old, value);
	}

	public String getMusic() {
		return doc.getDocumentElement().getAttribute("music");
	}

	public void setMusic(String filename, String loopMusic,
			String initialMusicDelay, String repeatMusicDelay) {
		if (filename != null && !filename.isEmpty())
			doc.getDocumentElement().setAttribute("music", filename);
		else
			doc.getDocumentElement().removeAttribute("music");

		if (loopMusic != null && !loopMusic.isEmpty())
			doc.getDocumentElement().setAttribute("loop_music", loopMusic);
		else
			doc.getDocumentElement().removeAttribute("loop_music");

		if (initialMusicDelay != null && !initialMusicDelay.isEmpty())
			doc.getDocumentElement().setAttribute("initial_music_delay",
					initialMusicDelay);
		else
			doc.getDocumentElement().removeAttribute("initial_music_delay");

		if (repeatMusicDelay != null && !repeatMusicDelay.isEmpty())
			doc.getDocumentElement().setAttribute("repeat_music_delay",
					repeatMusicDelay);
		else
			doc.getDocumentElement().removeAttribute("repeat_music_delay");

		modified = true;
		firePropertyChange("music", doc.getDocumentElement());
	}

	public String getBackground() {
		return doc.getDocumentElement().getAttribute("background");
	}

	public String getLightmap() {
		return doc.getDocumentElement().getAttribute("lightmap");
	}

	public void setBackground(String value) {
		setRootAttr("background", value);
	}

	public void setLightmap(String value) {
		setRootAttr("lightmap", value);
	}

	public Scene getEngineScene(int wWidth, int wHeight) {
		Scene scn = new Scene();

		scn.setId(getId());

		scn.getCamera().create(wWidth, wHeight);

		String background = getBackground();
		if (background != null && !background.isEmpty()) {
			scn.setBackground(background, getLightmap());
		}

		String depthVector = getRootAttr("depth_vector");
		if (!depthVector.isEmpty())
			scn.setDepthVector(Param.parseVector2(depthVector));

		// GET ACTORS
		NodeList actors = getActors();
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

		scn.loadAssets();
		EngineAssetManager.getInstance().getManager().finishLoading();
		scn.retrieveAssets();

		// SET FA NUMFRAMES OR ANIMATIONS3D
		for (int i = 0; i < actors.getLength(); i++) {
			Element a = (Element) actors.item(i);
			Actor ba = scn.getActor(getId(a), false, true);

			if (ba instanceof SpriteActor) {
				SpriteRenderer r = ((SpriteActor) ba).getRenderer();

				if (r instanceof SpriteAtlasRenderer) {
					SpriteAtlasRenderer sa = (SpriteAtlasRenderer) r;
					for (AtlasFrameAnimation fa : sa.getFrameAnimations()
							.values()) {
						if (fa.regions == null)
							setFANumFrames(a, fa.id, 0);
						else
							setFANumFrames(a, fa.id, fa.regions.size);
					}
				} else if (r instanceof Sprite3DRenderer) {
					Sprite3DRenderer sa = (Sprite3DRenderer) r;
					animations3d.put(ba.getId(), sa.getAnimations());
				}
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

		// File oldFile = new File(getFilename());
		// setFilenameFromId(newId);
		//
		// File newFile = new File(getFilename());
		//
		// oldFile.renameTo(newFile);

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
		setRootAttr("id", id);
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

	public SceneDocument cloneScene() {
		// TODO Auto-generated method stub
		return null;
	}

	public SceneDocument pasteScene() {
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
	public void setActorId(Element e, String id) {
		NodeList actors = getActors();

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

	public int getFANumFrames(Element e, String id) {
		int n;

		if (faFrames.get(getId(e) + "." + id) == null)
			return 0;

		n = faFrames.get(getId(e) + "." + id);

		return n;
	}

	public void setFANumFrames(Element e, String id, int n) {
		faFrames.put(getId(e) + "." + id, n);
	}

	public NodeList getFrameAnimations(Element e) {
		return e.getElementsByTagName("frame_animation");
	}

	public Array<Animation> getAnimations3d(String id) {
		return animations3d.get(id);
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

		if (type.equals(SPRITE_ACTOR) || type.equals(FOREGROUND_ACTOR)) {
			a = new SpriteActor();
			((SpriteActor)a).setRenderer(new SpriteAtlasRenderer());
		} else if (type.equals(SPRITE3D_ACTOR)) {
			a = new SpriteActor();
			((SpriteActor)a).setRenderer(new Sprite3DRenderer());
		} else if (type.equals(BACKGROUND_ACTOR)) {
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
			SpriteRenderer r = ((SpriteActor)a).getRenderer();
			
			if (r instanceof SpriteAtlasRenderer) {
				NodeList faList = getFrameAnimations(e);

				for (int i = 0; i < faList.getLength(); i++) {
					Element faElement = (Element) faList.item(i);

					AtlasFrameAnimation fa = getEngineFA(faElement);

					((SpriteAtlasRenderer) r).addFrameAnimation(fa);
				}
			} else {
				Sprite3DRenderer a3d = (Sprite3DRenderer) r;
				a3d.setModel(e.getAttribute("model"));
				a3d.setSpriteSize(Param.parseVector2(e
						.getAttribute("sprite_size")));
			}

			Vector2 pos = getPos(e);
			((SpriteActor) a).setPosition(pos.x, pos.y);

			if (!e.getAttribute("init_frame_animation").isEmpty()) {
				((SpriteActor) a).getRenderer().setInitFrameAnimation(e
						.getAttribute("init_frame_animation"));

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

	public AtlasFrameAnimation getEngineFA(Element faElement) {

		AtlasFrameAnimation fa = new AtlasFrameAnimation();
		fa.id = faElement.getAttribute("id");
		fa.atlas = faElement.getAttribute("atlas");

		if (faElement.getAttribute("animation_type").isEmpty()
				|| faElement.getAttribute("animation_type").equalsIgnoreCase(
						"repeat")) {
			fa.animationType = EngineTween.REPEAT;
		} else if (faElement.getAttribute("animation_type").equalsIgnoreCase(
				"yoyo")) {
			fa.animationType = EngineTween.YOYO;
		} else {
			fa.animationType = EngineTween.NO_REPEAT;
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

		// LOAD THE FA ATLAS AND REGIONS
		// EngineAssetManager.getInstance().loadAtlas(fa.atlas);
		// EngineAssetManager.getInstance().getManager().finishLoading();
		// fa.regions = EngineAssetManager.getInstance().getRegions(fa.atlas,
		// fa.id);

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
