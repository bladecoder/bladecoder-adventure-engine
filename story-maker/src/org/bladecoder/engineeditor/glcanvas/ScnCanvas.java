package org.bladecoder.engineeditor.glcanvas;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.TweenManagerSingleton;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.BaseActor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteAtlasRenderer;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.ui.UI;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class ScnCanvas extends ApplicationAdapter {
	private OrthographicCamera screenCamera;
	private SpriteBatch batch;
	private BitmapFont font;
	private CanvasDrawer drawer;
	private FARenderer faRenderer2;
	private InputMultiplexer input;

	private Texture backgroundTexture;

	private Scene scn = null;
	private BaseActor selectedActor = null;
	
	SceneDocument selDoc;
	Element selElementActor;
	String selFA;

	private static final int[] zoomLevels = { 5, 10, 16, 25, 33, 50, 66, 100, 150, 200, 300, 400,
			600, 800, 1000 };
	private int zoomLevel = 100;

	private OnOffComponent showFA;
	private OnOffComponent showFAInScn;
	private OnOffComponent toggleTestMode;
	private OnOffComponent toggleAnim;

	private boolean testMode = false;
	private World world;
	private UI ui;

	private static final String LOADING_MSG = "Loading...";
	private String msg;
	
	/** true when the selected actor must be created */
	private boolean createActor = false;

	@Override
	public void create() {
		Assets.inst().initialize();
		Tween.registerAccessor(Sprite.class, new SpriteAccessor());

		screenCamera = new OrthographicCamera();
//		resetCameras();

		batch = new SpriteBatch();
		font = new BitmapFont();
		faRenderer2 = new FARenderer();
		drawer = new CanvasDrawer();

		showFA = new OnOffComponent();
		showFA.setPos(150, 5);
		showFA.setText("Sprites");

		showFAInScn = new OnOffComponent();
		showFAInScn.setPos(160 + showFA.getBbox().width, 5);
		showFAInScn.setText("Sprites in Scene");
		showFAInScn.setState(false);

		toggleAnim = new OnOffComponent();
		toggleAnim.setPos(showFAInScn.getBbox().x + showFAInScn.getBbox().width + 5, 5);
		toggleAnim.setText("Anim.");
		toggleAnim.setState(true);
		
		toggleTestMode = new OnOffComponent();
		toggleTestMode.setPos(toggleAnim.getBbox().x + toggleAnim.getBbox().width + 5, 5);
		toggleTestMode.setText("Test");
		toggleTestMode.setState(false);

		backgroundTexture = Assets.inst().get("res/images/transparent-light.png", Texture.class);
		backgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

		input = new InputMultiplexer();
		input.addProcessor(new PanZoomInputProcessor(this));
		input.addProcessor(new EditionInputProcessor(this));

		setTestMode(false);

//		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
//			@Override
//			public void propertyChange(PropertyChangeEvent e) {
//				if (e.getPropertyName().equals(Project.NOTIFY_SCENE_SELECTED)) {
//					scnChanged = true;
//					EditorLogger.debug("ScnCanvas Listener: NOTIFY_SCENE_SELECTED " + e.getNewValue());
//				} else if (e.getPropertyName().equals(Project.NOTIFY_ACTOR_SELECTED)) {
//					actorChanged = true;
//					EditorLogger.debug("ScnCanvas Listener: NOTIFY_ACTOR_SELECTED " + e.getNewValue());
//				} else if (e.getPropertyName().equals(Project.NOTIFY_FA_SELECTED)) {
//					faChanged = true;
//					EditorLogger.debug("ScnCanvas Listener: NOTIFY_FA_SELECTED " + e.getNewValue());
//				}
//			}
//		});

		Ctx.project.getWorld().addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				EditorLogger.debug("ScnCanvas Listener: " + e.getPropertyName());
				SceneDocument doc = Ctx.project.getSelectedScene();

				if (e.getPropertyName().equals("scene")) {
					selDoc = null; // FORCE SCENE RELOAD
				} else if (e.getPropertyName().equals("bbox")) {
					Element selActor = (Element) e.getNewValue();
					String id = doc.getId(selActor);
					BaseActor a = scn.getActor(id, false, true);
					if (a == null)
						return;

					a.setBbox(doc.getBBox(selActor));
				} else if (e.getPropertyName().equals("pos")) {
					Element selActor = (Element) e.getNewValue();
					String id = doc.getId(selActor);
					BaseActor a = scn.getActor(id, false, true);
					if (a == null)
						return;
					Vector2 p = doc.getPos(selActor);
					((SpriteActor) a).setPosition(p.x, p.y);
				} else if (e.getPropertyName().equals("id")) {
					String id = (String) e.getOldValue();

					if (selectedActor == null || !selectedActor.getId().equals(id))
						return;

					scn.removeActor(scn.getActor(id));
					selectedActor = null;
					selElementActor = null;
				} else if (e.getPropertyName().equals("frame_animation")) {
//					Element faElement = (Element) e.getNewValue();
//					Element ae = (Element) faElement.getParentNode();

					createActor = true;
//					removeActor(doc, ae);
//					selectedActor = createActor(doc, ae);
//					selElementActor = null;
					selFA = null; // FORCE FA RELOAD
				} else if (e.getPropertyName().equals("init_frame_animation")) {
					Element actor = (Element) e.getNewValue();
					((SpriteActor) selectedActor).setInitFrameAnimation(actor
							.getAttribute("init_frame_animation"));
					selFA = null; // FORCE FA RELOAD
				} else if (e.getPropertyName().equals("actor")) {
					createActor = true;
//					removeActor(doc, (Element) e.getNewValue());
//					selectedActor = createActor(doc, (Element) e.getNewValue());
//					selElementActor = null;
				} else if (e.getPropertyName().equals(BaseDocument.NOTIFY_ELEMENT_DELETED)) {
					if (((Element) e.getNewValue()).getTagName().equals("actor"))
						removeActor(doc, (Element) e.getNewValue());
					else if (((Element) e.getNewValue()).getTagName().equals("frame_animation"))
						selFA = null; // FORCE FA RELOAD
				}
			}
		});
	}

	public void translate(Vector2 delta) {
		scn.getCamera().translate(-delta.x, -delta.y, 0);
		scn.getCamera().update();
	}

	public void zoom(int amount) {
		if (zoomLevel == zoomLevels[0] && amount < 0) {
			zoomLevel = zoomLevels[1];
		} else if (zoomLevel == zoomLevels[zoomLevels.length - 1] && amount > 0) {
			zoomLevel = zoomLevels[zoomLevels.length - 2];
		} else {
			for (int i = 1; i < zoomLevels.length - 1; i++) {
				if (zoomLevels[i] == zoomLevel) {
					zoomLevel = amount > 0 ? zoomLevels[i - 1] : zoomLevels[i + 1];
					break;
				}
			}
		}

		scn.getCamera().zoom = 100f / zoomLevel;
		scn.getCamera().update();
	}

	public Scene getScene() {
		return scn;
	}

	public BaseActor selectedActor() {
		return selectedActor;
	}

	@Override
	public void render() {
		SceneDocument prjDoc = Ctx.project.getSelectedScene();
		Element prjActor = Ctx.project.getSelectedActor();
		String prjFA = Ctx.project.getSelectedFA();
		
		if(createActor) {
			removeActor(prjDoc, prjActor);
			selectedActor = createActor(prjDoc, prjActor);
			createActor = false;
		}

		if (prjDoc != selDoc && msg != null) {
			selDoc = prjDoc;
			msg = null;

			if (scn != null) {
				scn.dispose();
				scn = null;
			}

			if (world != null) {
				world.dispose();
			}

			setTestMode(false);

			selectedActor = null;
			selElementActor = null;
			selFA = null;
			
			faRenderer2.setFrameAnimation(prjDoc, null, null);
			
			resetCameras();
		} else if (prjDoc != selDoc && msg == null) {
			msg = LOADING_MSG;
		}

		if (prjDoc == selDoc && prjActor != selElementActor && msg == null) {
			selElementActor = prjActor;
			selFA = null;
			
			if (scn != null && prjActor != null) {
				selectedActor = scn.getActor(prjDoc.getId(prjActor), false, true);
			} else {
				selectedActor = null;
			}

			faRenderer2.setFrameAnimation(prjDoc, null, null);
		}

		if (prjDoc == selDoc && prjActor == selElementActor && prjFA != selFA && msg == null) {
			selFA = prjFA;
			
			if (scn != null && selectedActor != null && selectedActor instanceof SpriteActor) {
				if(((SpriteActor)selectedActor).getRenderer() instanceof SpriteAtlasRenderer)
					setSpriteAtlasFA(prjFA);
				else
					setSprite3DFA(prjFA);				
			} else {
				faRenderer2.setFrameAnimation(prjDoc, null, null);
			}
		}

		if (msg != null)
			drawMsg();
		else if (testMode)
			drawTestMode();
		else
			drawEditMode();

	}
	
	private void setSpriteAtlasFA(String selFA) {
		SpriteAtlasRenderer s = (SpriteAtlasRenderer) ((SpriteActor)selectedActor).getRenderer();

		if (selFA == null || s.getFrameAnimation(selFA) == null) {
			selFA = ((SpriteActor)selectedActor).getInitFrameAnimation();
		}

		if (selFA != null && s.getFrameAnimation(selFA) != null) {

			faRenderer2.setFrameAnimation(selDoc, selElementActor, selFA);

			if (showFAInScn.getState()
					|| s.getCurrentFrameAnimation() == null
					|| ((SpriteActor)selectedActor).getInitFrameAnimation().equals(selFA)) {
				((SpriteActor)selectedActor).startFrameAnimation(selFA, EngineTween.REPEAT,Tween.INFINITY, null);
			}
		} else {
			faRenderer2.setFrameAnimation(selDoc, null, null);
		}
	}
	
	private void setSprite3DFA(String fa) {
		
		Sprite3DRenderer s = (Sprite3DRenderer) ((SpriteActor)selectedActor).getRenderer();
		
		if (fa == null && ((SpriteActor)selectedActor).getInitFrameAnimation() != null) 
				fa = ((SpriteActor)selectedActor).getInitFrameAnimation();
		
		faRenderer2.setFrameAnimation(selDoc, selElementActor, fa);
		
		if (showFAInScn.getState()
//				|| s.getCurrentFrameAnimation() == null
				|| ((SpriteActor)selectedActor).getInitFrameAnimation() != null && ((SpriteActor)selectedActor).getInitFrameAnimation().equals(fa)) {
			s.startFrameAnimation(fa, EngineTween.FROM_FA, -1, null);

		}
	}

	private BaseActor createActor(SceneDocument doc, Element e) {

		String type = doc.getType(e);
		BaseActor a = doc.getEngineActor(e);

		if (type.equals("foreground")) {
			scn.addFgActor((SpriteActor) a);
		} else {
			scn.addActor(a);
		}
		
		a.loadAssets();
		EngineAssetManager.getInstance().getManager().finishLoading();
		a.retrieveAssets();

		return a;
	}

	private void removeActor(SceneDocument doc, Element e) {
		BaseActor a = scn.getActor(doc.getId(e), false, true);
		if (a != null) {
			scn.removeActor(a);

			a.dispose();
			selectedActor = null;
		}
	}

	private void drawMsg() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		GL20 gl = Gdx.gl20;
		gl.glClearColor(0, 0, 0, 1);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.setProjectionMatrix(screenCamera.combined);
		batch.begin();
		TextBounds b = font.getMultiLineBounds(msg);
		font.drawMultiLine(batch, msg, (w - b.width) / 2, (h - b.height) / 2);
		batch.end();
	}

	private void drawEditMode() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();


		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
		if (scn != null) {

			// BACKGROUND
			batch.setProjectionMatrix(screenCamera.combined);
			batch.begin();
			batch.disableBlending();
			float tw = backgroundTexture.getWidth();
			float th = backgroundTexture.getHeight();
			batch.draw(backgroundTexture, 0f, 0f, w, h, 0f, 0f, w / tw, h / th);
			batch.enableBlending();
			batch.end();

			// WORLD CAMERA
			scn.update(Gdx.graphics.getDeltaTime());
			
			faRenderer2.update(Gdx.graphics.getDeltaTime());

			if (toggleAnim.getState()) {
				TweenManagerSingleton.getInstance().update(Gdx.graphics.getDeltaTime());
				
//				if(selectedActor != null && selectedActor instanceof SpriteActor)
//					((SpriteActor)selectedActor).update(Gdx.graphics.getDeltaTime());
			}

			batch.setProjectionMatrix(scn.getCamera().combined);
			batch.begin();
			scn.draw(batch);
			batch.end();

			drawer.drawBGBounds();
			drawer.drawBBoxActors(scn);

			if (selectedActor != null) {
				drawer.drawSelectedActor(selectedActor);
			}

			// SCREEN CAMERA
			batch.setProjectionMatrix(screenCamera.combined);
			batch.begin();

			//if (showFA.getState()) {
			if (!showFAInScn.getState()) {
				faRenderer2.draw(batch);
			}

			RectangleRenderer
					.draw(batch, 0f, 0f, (float) Gdx.graphics.getWidth(), 60f, Color.BLACK);
//			showFA.draw(batch);
			showFAInScn.draw(batch);
			toggleAnim.draw(batch);
			toggleTestMode.draw(batch);

			font.setColor(Color.WHITE);

			Vector2 coords = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
			font.draw(batch, MessageFormat.format("({0}, {1})", (int) coords.x, (int) coords.y),
					10, 45);

//			if (selectedActor instanceof SpriteAtlasRenderer
//					&& ((SpriteAtlasRenderer) selectedActor).getCurrentFrameAnimation() != null
//					&& ((SpriteAtlasRenderer) selectedActor).getCurrentFrameAnimation().regions != null) {
//				SpriteAtlasRenderer a = (SpriteAtlasRenderer) selectedActor;
//				// FrameAnimation fa = ((SpriteActor)
//				// selectedActor).getCurrentFrameAnimation();
//
//				// font.draw(batch,
//				// MessageFormat.format("Current Frame {4}/{5} IN({0}, {1}) OUT({2}, {3})",
//				// (int) fa.inDX, (int) fa.inDY, (int) fa.outDX, (int) fa.outDY,
//				// a.getCurrentFrame() + 1, a.getNumFrames()), 10,
//				// 22);
//
//				font.draw(
//						batch,
//						MessageFormat.format("Frame {0}/{1}", a.getCurrentFrame() + 1,
//								a.getNumFrames()), 10, 22);
//			}

			batch.end();
		}
	}
	
	public BaseActor getActor() {
		return selectedActor;
	}

	private void drawTestMode() {
		if (world.inTestMode()) {
			try {
				ui.update();
				ui.draw();
			} catch (Exception e) {
				EditorLogger.error(e.getMessage());
				setTestMode(false);
			}
		} else {
			setTestMode(false);
		}
	}

	@Override
	public void resize(int width, int height) {

		if (testMode && ui != null)
			ui.resize(width, height);
		else {
			GL20 gl = Gdx.gl20;
			gl.glViewport(0, 0, width, height);
			resetCameras();
		}
	}

	public void setTestMode(boolean value) {
		testMode = value;
		toggleTestMode.setState(value);

		if (value) {
			if (scn != null) {
				scn.dispose();
				scn = null;
			}

			if (Ctx.project.getProjectDir() != null) {
				try {
					Ctx.project.saveProject();
				} catch (Exception e) {
					EditorLogger.error(e.getMessage());
				}

				EngineAssetManager.createEditInstance(Ctx.project.getProjectDir().getAbsolutePath()
						+ "/assets", 0);
			}

			world = World.getInstance();
			
			world.loadXML(Ctx.project.getWorld().getCurrentChapter().getAttribute("id"), Ctx.project.getSelectedScene().getId());
			ui = new UI();
		} else {

			Gdx.input.setInputProcessor(input);

			if (world != null) {
				world.dispose();
				world = null;
			}

			if (Ctx.project.getProjectDir() != null) {
				EngineAssetManager.createEditInstance(Ctx.project.getProjectDir().getAbsolutePath()
						+ "/assets", Ctx.project.getWorld().getWidth());
			}

			if (scn == null && Ctx.project.getSelectedScene() != null) {
				try {
					scn = Ctx.project.getSelectedScene().getEngineScene( Ctx.project.getWorld().getWidth(),  
							Ctx.project.getWorld().getHeight());
					drawer.setCamera(scn.getCamera());
					resetCameras();
				} catch (Exception e) {
					msg = "COULD NOT CREATE SCENE\n" + e.getMessage();
					e.printStackTrace();
					
					if (scn != null) {
						scn.dispose();
						scn = null;
					}

					if (world != null)
						world.dispose();
				}
			}

		}

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	public synchronized void setMsg(String msg) {
		this.msg = msg;
	}

	public synchronized void setMsgWithTimer(String m, long millis) {
		this.msg = m;

		Timer t = new Timer();

		t.schedule(new TimerTask() {
			@Override
			public void run() {
				msg = null;
			}
		}, millis);
	}

	public Vector2 screenToWorld(int x, int y) {
		Vector3 v3 = new Vector3(x, y, 0);
		scn.getCamera().unproject(v3);
		return new Vector2(v3.x, v3.y);
	}

	public Vector2 toScreen(int x, int y) {
		Vector3 v3 = new Vector3(x, y, 0);
		screenCamera.unproject(v3);
		return new Vector2(v3.x, v3.y);
	}


	private void resetCameras() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		// SETS SCREEN CAMERA
		screenCamera.viewportWidth = w;
		screenCamera.viewportHeight = h;
		screenCamera.position.set(w / 2, h / 2, 0);
		screenCamera.update();

		// SETS WORLD CAMERA
		if (Ctx.project.getProjectDir() != null) {

			float aspect = w / h;

			float width = Ctx.project.getWorld().getWidth();
			float height = Ctx.project.getWorld().getHeight();
			float aspectWorld = width / height;

			if (aspectWorld > aspect) {
				height = width / aspect;
			} else {
				width = height * aspect;
			}
			
			zoomLevel = 100;

			scn.getCamera().setToOrtho(false, width, height);
			// worldCamera.translate(-width / 2, -height / 2, 0);
			scn.getCamera().zoom = 1f;
			scn.getCamera().update();

			translate(new Vector2((width - Ctx.project.getWorld().getWidth()) / 2,
					(height - Ctx.project.getWorld().getHeight()) / 2));
		}
	}

	public void click(float x, float y) {
		showFA.click(x, y);

		boolean old = showFAInScn.getState();
		showFAInScn.click(x, y);
		if (old != showFAInScn.getState()) {
			selFA = null;
		}

		toggleTestMode.click(x, y);
		toggleAnim.click(x, y);

		if (testMode == true && toggleTestMode.getState() == false) {
			setTestMode(false);
		} else if (testMode == false && toggleTestMode.getState() == true) {
			setTestMode(true);
		}
	}
}
