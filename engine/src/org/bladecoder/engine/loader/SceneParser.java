package org.bladecoder.engine.loader;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionFactory;
import org.bladecoder.engine.actions.Param;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.Dialog;
import org.bladecoder.engine.model.DialogOption;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteActor.DepthType;
import org.bladecoder.engine.model.SpriteAtlasRenderer;
import org.bladecoder.engine.model.SpriteSpineRenderer;
import org.bladecoder.engine.model.Verb;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.I18NControl;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class SceneParser extends DefaultHandler {
	private Actor actor;
	private Scene scene;

	Verb currentVerb;
	Dialog currentDialog;
	DialogOption currentOption;
	String initFrameAnimation = null;
	String player = null;

	float scale;

	ResourceBundle i18n;

	Locator locator;

	public SceneParser(String i18nFilename) {
		scale = EngineAssetManager.getInstance().getScale();
		this.scene = new Scene();

		Locale locale = Locale.getDefault();

		try {
			i18n = ResourceBundle.getBundle(i18nFilename, locale,
					new I18NControl("ISO-8859-1"));
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nFilename);
		}
	}

	public SceneParser(ResourceBundle i18n) {
		scale = EngineAssetManager.getInstance().getScale();
		this.scene = new Scene();

		this.i18n = i18n;
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (currentVerb != null) {
			parseAction(localName, atts, actor != null ? actor : scene);
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

			if (i18n != null && text != null && text.length() > 0
					&& text.charAt(0) == '@')
				text = i18n.getString(text.substring(1));

			if (i18n != null && responseText != null
					&& responseText.length() > 0
					&& responseText.charAt(0) == '@')
				responseText = i18n.getString(responseText.substring(1));

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
						"Actor 'type' attribute not found or empty", locator);
				error(e2);
				throw e2;
			}

			if (type.equals("background")) {
				actor = new Actor();
			} else {
				if (type.equals("sprite") || type.equals("foreground")) { // ATLAS
																			// ACTOR
					actor = new SpriteActor();
					((SpriteActor)actor).setRenderer(new SpriteAtlasRenderer());
				} else if (type.equals("sprite3d")) { // 3D ACTOR
					actor = new SpriteActor();
					Sprite3DRenderer r = new Sprite3DRenderer();						
					((SpriteActor)actor).setRenderer(r);

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

							r.setCameraPos(camPos.x,
									camPos.y, camPos.z);
						}

						if (atts.getValue("cam_rot") != null) {
							camRot = Param.parseVector3(atts
									.getValue("cam_rot"));

							r.setCameraRot(camRot.x,
									camRot.y, camRot.z);
						}

						fov = Float.parseFloat(atts.getValue("fov"));
						r.setCameraFOV(fov);

						if (atts.getValue("camera_name") != null) {
							r.setCameraName(atts
									.getValue("camera_name"));
						}

					} catch (Exception e) {
						SAXParseException e2 = new SAXParseException(
								"Wrong sprite3d params", locator, e);
						error(e2);
						throw e2;
					}

					String model = atts.getValue("model");

					r.setModel(model);
				} else if (type.equals("spine")) { // SPINE RENDERER				
					actor = new SpriteActor();
					SpriteSpineRenderer r = new SpriteSpineRenderer();						
					((SpriteActor)actor).setRenderer(r);
					
					String source = atts.getValue("source");
					r.setSource(source);
				}

				if (atts.getValue("walking_speed") != null
						&& !atts.getValue("walking_speed").isEmpty()) {
					float s = Float.parseFloat(atts.getValue("walking_speed"))
							* scale;
					((SpriteActor) actor).setWalkingSpeed(s);
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

				((SpriteActor) actor).setPosition(pos.x, pos.y);

				initFrameAnimation = atts.getValue("init_frame_animation");

				// PARSE DEPTH MAP USE
				String depthType = atts.getValue("depth_type");
				((SpriteActor) actor).setDepthType(DepthType.NONE);
				
				if ( depthType != null && !depthType.isEmpty()) {
					if(depthType.equals("map"))
						((SpriteActor) actor).setDepthType(DepthType.MAP);
					else if(depthType.equals("vector"))
						((SpriteActor) actor).setDepthType(DepthType.VECTOR);
				}
			}

			// PARSE BBOX
			if (atts.getValue("x") != null) {
				float x, y, width = 0, height = 0;

				try {
					x = Float.parseFloat(atts.getValue("x")) * scale;
					y = Float.parseFloat(atts.getValue("y")) * scale;
					width = Float.parseFloat(atts.getValue("width")) * scale;
					height = Float.parseFloat(atts.getValue("height")) * scale;
				} catch (NumberFormatException e) {
					SAXParseException e2 = new SAXParseException(
							"Wrong Bounding Box Definition", locator, e);
					error(e2);
					throw e2;
				}

				actor.setBbox(new Rectangle(x, y, width, height));
			} else if (type.equals("background")) {
				SAXParseException e2 = new SAXParseException(
						"Bounding box definition not set for actor", locator);
				error(e2);
				throw e2;
			}

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

			if (atts.getValue("active") != null) {
				boolean active = Boolean.parseBoolean(atts.getValue("active"));
				actor.setActive(active);
			}

			String id = atts.getValue("id");
			String desc = atts.getValue("desc");

			if (i18n != null && desc != null && desc.length() > 0
					&& desc.charAt(0) == '@') {
				try {
					desc = i18n.getString(desc.substring(1));
				} catch (MissingResourceException e) {
					EngineLogger.error("MISSING TRANSLATION KEY: " + desc);
				}
			}

			String state = atts.getValue("state");

			if (id == null || id.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"Actor 'id' attribute not found or empty", locator);
				error(e2);
				throw e2;
			}

			actor.setId(id);

			if (desc != null)
				actor.setDesc(desc);

			if (state != null)
				actor.setState(state);

			if (type.equals("foreground")) {
				scene.addFgActor((SpriteActor) actor);
			} else {
				scene.addActor(actor);
			}

		} else if (localName.equals("frame_animation")) {

			if (actor == null || !(actor instanceof SpriteActor)) {
				SAXParseException e = new SAXParseException(
						"'frame_animation' TAG must be inside actor of type 'sprite', 'player' or 'foreground'",
						locator);
				error(e);
				throw e;
			}

			if (!(((SpriteActor)actor).getRenderer() instanceof SpriteAtlasRenderer)) {
				SAXParseException e2 = new SAXParseException(
						"Only SpriteAtlasActors can have animations", locator);
				error(e2);
				throw e2;
			}

			String speedstr = atts.getValue("speed");
			String animationTypestr = atts.getValue("animation_type");
			String delaystr = atts.getValue("delay");
			String countstr = atts.getValue("count");
			String soundId = atts.getValue("sound");
			String inDstr = atts.getValue("inD");
			String outDstr = atts.getValue("outD");

			float speed;
			float delay;
			int animationType;
			int count;
			Vector2 inD = null, outD=null;

			String id = atts.getValue("id");

			if (id == null || id.isEmpty()) {
				SAXParseException e = new SAXParseException(
						"Animation 'id' not found or empty", locator);
				error(e);
				throw e;
			}

			// i18n images and animations
			if (i18n != null && id != null && id.length() > 0
					&& id.charAt(0) == '@')
				id = i18n.getString(id.substring(1));

			String atlas = atts.getValue("atlas");
			if (atlas == null || atlas.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"Atlas name not found or empty", locator);
				error(e2);
				throw e2;
			}

			try {
				if (speedstr == null || speedstr.isEmpty())
					speed = 1f;
				else
					speed = Float.parseFloat(speedstr);

				if (delaystr == null || delaystr.isEmpty())
					delay = 0f;
				else
					delay = Float.parseFloat(delaystr);

				if (countstr == null || countstr.isEmpty())
					count = Tween.INFINITY;
				else
					count = Integer.parseInt(countstr);

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
				animationType = EngineTween.REPEAT;
			} else if (animationTypestr.equalsIgnoreCase("yoyo")) {
				animationType = EngineTween.YOYO;
			} else {
				animationType = EngineTween.NO_REPEAT;
			}

			AtlasFrameAnimation sa = new AtlasFrameAnimation(id, atlas, speed, delay,
					count, animationType, soundId, inD, outD);

			((SpriteAtlasRenderer)((SpriteActor) actor).getRenderer()).addFrameAnimation(sa);
		} else if (localName.equals("verb")) {
			parseVerb(localName, atts, actor != null ? actor : scene);
		} else if (localName.equals("dialog")) {
			String id = atts.getValue("id");

			currentDialog = new Dialog();
			currentDialog.setId(id);
			currentDialog.setActor(actor.getId());
			currentOption = null;

			actor.addDialog(id, currentDialog);
		} else if (localName.equals("sound")) {
			parseSound(localName, atts, actor != null ? actor : scene);
		} else if (localName.equals("scene")) {
			String idScn = atts.getValue("id");
			String bgFilename = atts.getValue("background");
			String lightmap = atts.getValue("lightmap");
			String musicFilename = atts.getValue("music");
			String loopMusicStr = atts.getValue("loop_music");
			String initialMusicDelayStr = atts.getValue("initial_music_delay");
			String repeatMusicDelayStr = atts.getValue("repeat_music_delay");

			scene.setDepthVector(Param.parseVector2(atts.getValue("depth_vector")));
			player = atts.getValue("player");

			if (idScn == null || idScn.isEmpty()) {
				SAXParseException e2 = new SAXParseException(
						"Scene 'id' not found or empty", locator);
				error(e2);
				throw e2;
			}

			scene.setId(idScn);

			String atlases = atts.getValue("atlases");
			if (atlases != null)
				scene.setAtlases(atlases);

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
			SAXParseException e = new SAXParseException("Wrong label '"
					+ localName + "' loading Scene.", locator);
			error(e);
			throw e;
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
				((SpriteActor) actor).setInitFrameAnimation(initFrameAnimation);
			}

			actor = null;
		}

		scene.setPlayer((SpriteActor) scene.getActor(player));
	}

	private void parseSound(String localName, Attributes atts, Actor actor)
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

	private void parseVerb(String localName, Attributes atts, Actor actor) {

		String id = atts.getValue("id");
		String target = atts.getValue("target");
		String state = atts.getValue("state");

		if (target != null)
			id = id + "." + target;

		if (state != null)
			id = id + "." + state;

		currentVerb = new Verb(id);

		actor.addVerb(id, currentVerb);
	}

	private void parseAction(String localName, Attributes atts, Actor actor) {
		String actionName = localName;
		Action action = null;
		HashMap<String, String> params = new HashMap<String, String>();
		String actionClass = null;

		for (int i = 0; i < atts.getLength(); i++) {
			String attName = atts.getLocalName(i);

			if (attName.equals("class")) {
				actionClass = atts.getValue(attName);
			} else {
				String value = atts.getValue(attName);
				if (i18n != null && value != null && value.length() > 0
						&& value.charAt(0) == '@') {
					try {
						value = i18n.getString(value.substring(1));
					} catch (MissingResourceException e) {
						EngineLogger.error("MISSING TRANSLATION KEY: " + value);
					}
				}

				params.put(attName, value);
			}
		}

		if (atts.getValue("actor") == null)
			params.put("actor", actor.getId());

		if (actionClass != null) {
			action = ActionFactory.createByClass(actionClass, params);
		} else {
			action = ActionFactory.create(actionName, params);
		}

		if (action != null) {
			currentVerb.add(action);
		} else {
			EngineLogger.error("Action '" + actionName + "' not found.");
		}
	}

	public Scene getScene() {
		return scene;
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