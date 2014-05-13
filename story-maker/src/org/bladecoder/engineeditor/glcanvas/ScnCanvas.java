package org.bladecoder.engineeditor.glcanvas;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.ui.UI;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.scene2d.ActorPanel;
import org.bladecoder.engineeditor.ui.scene2d.ScenePanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class ScnCanvas extends ApplicationAdapter {
	public static final String SKIN = "res/skin/HoloSkin/Holo-dark-ldpi.json";
//	public static final String SKIN = "res/skin/uiskin.json";
	
	private OrthographicCamera screenCamera;
	private SpriteBatch batch;
	private BitmapFont font;

	private Scene scn = null;
	private Actor selectedActor = null;

	ChapterDocument selDoc;
	Element selElementScene;
	Element selElementActor;
	String selFA;

	private boolean testMode = false;
	private World world;
	private UI ui;

	private static final String LOADING_MSG = "Loading...";
	private String msg;

	/** true when the selected actor must be created */
	private boolean createActor = false;

	/************************** SCENE 2D UI TESTING **************************/
	Stage stage;
	ScnWidget scnWidget;
	CheckBox inSceneCb;
	CheckBox animCb;
	TextButton testButton;
	Label coordsLbl;

	@Override
	public void create() {
		Assets.inst().initialize();

		screenCamera = new OrthographicCamera();
		// resetCameras();

		batch = new SpriteBatch();
		font = new BitmapFont();
		scnWidget = new ScnWidget();

		/*** STAGE SETUP ***/
		stage = new Stage(new ScreenViewport());
		// Skin skin = new Skin(Gdx.files.internal("res/skin/uiskin.json"));
		Skin skin = new Skin(
				Gdx.files.internal(SKIN));
		
		// Generate a 1x1 white texture and store it in the skin named "background".
//		Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
//		pixmap.setColor(Color.BLACK);
//		pixmap.fill();
//		skin.add("background", new Texture(pixmap));
		
		Table editorPanel = new Table(skin);
		
		ScenePanel scenePanel = new ScenePanel(skin);
		ActorPanel actorPanel = new ActorPanel(skin);
		
		Table rightPanel = new Table();
//		verticalGroup.fill();
		rightPanel.add(scenePanel).expand().fill();
		rightPanel.row();
		rightPanel.add(actorPanel).expand().fill();
				
		SplitPane splitPane = new SplitPane(editorPanel, rightPanel, false, skin);
		splitPane.setFillParent(true);
		stage.addActor(splitPane);
		
		stage.setScrollFocus(scnWidget);

		inSceneCb = new CheckBox("In Scene Sprites", skin);
		inSceneCb.setChecked(false);
		animCb = new CheckBox("Animation", skin);
		animCb.setChecked(true);
		testButton = new TextButton("Test", skin);
		coordsLbl = new Label("", skin);
		editorPanel.add(scnWidget).expand().fill();
		editorPanel.row();
		
		Table bottomTable = new Table(skin);
//		bottomTable.setBackground("background");
		editorPanel.add(bottomTable).fill();

		bottomTable.add(coordsLbl);
		bottomTable.add(inSceneCb);
		bottomTable.add(animCb);
		bottomTable.add(testButton);
		
		inSceneCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setInSceneSprites(inSceneCb.isChecked());
			}
		});
		
		animCb.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				scnWidget.setAnimation(animCb.isChecked());
			}
		});

		testButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event,
					com.badlogic.gdx.scenes.scene2d.Actor actor) {
				setTestMode(true);

			}
		});
		
		setTestMode(false);

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						EditorLogger.debug("ScnCanvas Listener: "
								+ e.getPropertyName());
						ChapterDocument doc = Ctx.project.getSelectedChapter();

						if (e.getPropertyName().equals("scene")) {
							selElementScene = null; // FORCE SCENE RELOAD
						} else if (e.getPropertyName().equals("bbox")) {
							Element selActor = (Element) e.getNewValue();
							String id = doc.getId(selActor);
							Actor a = scn.getActor(id, false, true);
							if (a == null)
								return;

							a.setBbox(doc.getBBox(selActor));
						} else if (e.getPropertyName().equals("pos")) {
							Element selActor = (Element) e.getNewValue();
							String id = doc.getId(selActor);
							Actor a = scn.getActor(id, false, true);
							if (a == null)
								return;
							Vector2 p = doc.getPos(selActor);
							((SpriteActor) a).setPosition(p.x, p.y);
						} else if (e.getPropertyName().equals("id")) {
							String id = (String) e.getOldValue();

							if (selectedActor == null
									|| !selectedActor.getId().equals(id))
								return;

							scn.removeActor(scn.getActor(id));
							selectedActor = null;
							selElementActor = null;
							scnWidget.setSelectedActor(selectedActor);
						} else if (e.getPropertyName()
								.equals("frame_animation")) {
							// Element faElement = (Element) e.getNewValue();
							// Element ae = (Element) faElement.getParentNode();

							createActor = true;
							// removeActor(doc, ae);
							// selectedActor = createActor(doc, ae);
							// selElementActor = null;
							selFA = null; // FORCE FA RELOAD
						} else if (e.getPropertyName().equals(
								"init_frame_animation")) {
							Element actor = (Element) e.getNewValue();
							((SpriteActor) selectedActor)
									.getRenderer()
									.setInitFrameAnimation(
											actor.getAttribute("init_frame_animation"));
							selFA = null; // FORCE FA RELOAD
						} else if (e.getPropertyName().equals("actor")) {
							createActor = true;
							// removeActor(doc, (Element) e.getNewValue());
							// selectedActor = createActor(doc, (Element)
							// e.getNewValue());
							// selElementActor = null;
						} else if (e.getPropertyName().equals(
								BaseDocument.NOTIFY_ELEMENT_DELETED)) {
							if (((Element) e.getNewValue()).getTagName()
									.equals("actor"))
								removeActor(doc, (Element) e.getNewValue());
							else if (((Element) e.getNewValue()).getTagName()
									.equals("frame_animation"))
								selFA = null; // FORCE FA RELOAD
						}
					}
				});
		

	}


	public Scene getScene() {
		return scn;
	}

	public Actor selectedActor() {
		return selectedActor;
	}

	@Override
	public void render() {
		ChapterDocument prjDoc = Ctx.project.getSelectedChapter();
		Element prjScene = Ctx.project.getSelectedScene();
		Element prjActor = Ctx.project.getSelectedActor();
		String prjFA = Ctx.project.getSelectedFA();

		if (createActor) {
			removeActor(prjDoc, prjActor);
			selectedActor = createActor(prjDoc, prjActor);
			createActor = false;
			scnWidget.setSelectedActor(selectedActor);
		}

		if ((prjDoc != selDoc || prjScene != selElementScene) && msg != null) {
			selDoc = prjDoc;
			selElementScene = prjScene;
			msg = null;

			if (scn != null) {
				scn.dispose();
				scn = null;
				scnWidget.setScene(scn);
			}

			if (world != null) {
				world.dispose();
			}

			setTestMode(false);

			selectedActor = null;
			scnWidget.setSelectedActor(selectedActor);
			selElementActor = null;
			selFA = null;

			resetCameras();
		} else if ((prjDoc != selDoc || prjScene != selElementScene)
				&& msg == null) {
			msg = LOADING_MSG;
		}

		if (prjDoc == selDoc && prjActor != selElementActor && msg == null) {
			selElementActor = prjActor;
			selFA = null;

			if (scn != null && prjActor != null) {
				selectedActor = scn.getActor(prjDoc.getId(prjActor), false,
						true);
			} else {
				selectedActor = null;
			}
			
			scnWidget.setSelectedActor(selectedActor);
		}

		if (prjDoc == selDoc && prjActor == selElementActor && prjFA != selFA
				&& msg == null) {
			selFA = prjFA;

			if (scn != null && selectedActor != null
					&& selectedActor instanceof SpriteActor) {
				setFA(prjFA);
			} else {
				scnWidget.setFrameAnimation(null);
			}
		}

		if (msg != null)
			drawMsg();
		else if (testMode)
			drawTestMode();
		else
			drawEditMode();

	}

	private void setFA(String selFA) {
		SpriteRenderer s = ((SpriteActor) selectedActor).getRenderer();

		if (selFA == null || s.getFrameAnimations().get(selFA) == null) {
			selFA = ((SpriteActor) selectedActor).getRenderer()
					.getInitFrameAnimation();
		}

		if (selFA != null && s.getFrameAnimations().get(selFA) != null) {

			scnWidget.setFrameAnimation(s.getFrameAnimations().get(selFA));

			if (inSceneCb.isChecked()
					|| s.getCurrentFrameAnimation() == null
					|| ((SpriteActor) selectedActor).getRenderer()
							.getInitFrameAnimation().equals(selFA)) {
				((SpriteActor) selectedActor).startFrameAnimation(selFA,
						Tween.REPEAT, Tween.INFINITY, null);
			}
		} else {
			scnWidget.setFrameAnimation(null);
		}
	}

	private Actor createActor(ChapterDocument doc, Element e) {

		String type = doc.getType(e);
		Actor a = doc.getEngineActor(e);

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

	private void removeActor(ChapterDocument doc, Element e) {
		Actor a = scn.getActor(doc.getId(e), false, true);
		if (a != null) {
			scn.removeActor(a);

			a.dispose();
			selectedActor = null;
			scnWidget.setSelectedActor(selectedActor);
		}
	}

	private void drawMsg() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		String tmp = msg;

		GL20 gl = Gdx.gl20;
		gl.glClearColor(0, 0, 0, 1);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(screenCamera.combined);
		batch.begin();
		TextBounds b = font.getMultiLineBounds(tmp);
		font.drawMultiLine(batch, tmp, (w - b.width) / 2, (h - b.height) / 2);
		batch.end();
	}

	private void drawEditMode() {

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (scn != null) {
			Vector2 coords = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(coords);
			coordsLbl.setText(MessageFormat.format("({0}, {1})", (int) coords.x,
					(int) coords.y));
			
			stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
			stage.draw();
			Table.drawDebug(stage);
		}
	}

	public Actor getActor() {
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
			stage.getViewport().update(width, height, true);
		}
	}

	public void setTestMode(boolean value) {
		testMode = value;

		if (value) {
			if (scn != null) {
				scn.dispose();
				scn = null;
				scnWidget.setScene(scn);
			}

			if (Ctx.project.getProjectDir() != null) {
				try {
					Ctx.project.saveProject();
				} catch (Exception e) {
					EditorLogger.error(e.getMessage());
				}

				EngineAssetManager.createEditInstance(Ctx.project
						.getProjectDir().getAbsolutePath() + "/assets", 0);
			}

			world = World.getInstance();

			world.loadXML(Ctx.project.getSelectedChapter().getElement()
					.getAttribute("id"), Ctx.project.getSelectedScene()
					.getAttribute("id"));
			ui = new UI();
		} else {

			Gdx.input.setInputProcessor(stage);

			if (world != null) {
				world.dispose();
				world = null;
			}

			if (Ctx.project.getProjectDir() != null) {
				EngineAssetManager.createEditInstance(Ctx.project
						.getProjectDir().getAbsolutePath() + "/assets",
						Ctx.project.getWorld().getWidth());
			}

			if (scn == null && Ctx.project.getSelectedScene() != null) {
				try {
					scn = Ctx.project.getSelectedChapter().getEngineScene(
							Ctx.project.getSelectedScene(),
							Ctx.project.getWorld().getWidth(),
							Ctx.project.getWorld().getHeight());
					
					scnWidget.setScene(scn);
					
					resetCameras();
				} catch (Exception e) {
					msg = "COULD NOT CREATE SCENE\n" + e.getMessage();
					e.printStackTrace();

					if (scn != null) {
						scn.dispose();
						scn = null;
						scnWidget.setScene(scn);
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

	private void resetCameras() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		// SETS SCREEN CAMERA
		screenCamera.viewportWidth = w;
		screenCamera.viewportHeight = h;
		screenCamera.position.set(w / 2, h / 2, 0);
		screenCamera.update();
	}
}
