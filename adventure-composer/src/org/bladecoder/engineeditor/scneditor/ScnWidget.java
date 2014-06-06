package org.bladecoder.engineeditor.scneditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ScnWidget extends Widget {
	private static final Color BLACK_TRANSPARENT= new Color(0f,0f,0f,0.5f);
	
	// TMPs to avoid GC calls
	private final Vector3 tmpV3 = new Vector3();
	private final Vector2 tmpV2 = new Vector2();

	private final SpriteBatch sceneBatch = new SpriteBatch();
	private final CanvasDrawer drawer = new CanvasDrawer();
	private final SpriteDrawer faRenderer = new SpriteDrawer();
	private final ScnWidgetInputListener inputListner = new ScnWidgetInputListener(
			this);

	private final Rectangle bounds = new Rectangle();
	private final Rectangle scissors = new Rectangle();

	private Scene scn;
	private Actor selectedActor = null;
	private boolean inScene = false;
	private boolean animation = true;

	private static final int[] zoomLevels = { 5, 10, 16, 25, 33, 50, 66, 100,
			150, 200, 300, 400, 600, 800, 1000 };
	private int zoomLevel = 100;

	BitmapFont bigFont;
	BitmapFont defaultFont;
	TiledDrawable tile;

	boolean loading = false;
	
	WalkZoneWindow walkZoneWindow;

	public ScnWidget(Skin skin) {
		bigFont = skin.get("big-font", BitmapFont.class);
		defaultFont = skin.get("default-font", BitmapFont.class);

		setSize(150, 150);

		tile = new TiledDrawable(Ctx.assetManager.getIcon("transparent-light"));

		faRenderer.setViewport(getWidth(), getHeight());

		setLayoutEnabled(true);

		addListener(inputListner);

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals(Project.NOTIFY_SCENE_SELECTED)) {
					setSelectedScene(Ctx.project.getSelectedScene());
				} else if (e.getPropertyName().equals(
						Project.NOTIFY_ACTOR_SELECTED)) {
					setSelectedActor(Ctx.project.getSelectedActor());
				} else if (e.getPropertyName().equals(
						Project.NOTIFY_FA_SELECTED)) {
					setSelectedFA(Ctx.project.getSelectedFA());
				} else if (e.getPropertyName().equals(
						Project.NOTIFY_PROJECT_LOADED)) {
					if (scn != null) {
						scn.dispose();
						scn = null;
					}

					EngineAssetManager.createEditInstance(Ctx.project
							.getProjectDir().getAbsolutePath() + "/assets",
							Ctx.project.getWorld().getWidth());
				}
			}
		});

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						EditorLogger.debug("Editor Listener: "
								+ e.getPropertyName());
						ChapterDocument doc = Ctx.project.getSelectedChapter();

						if (e.getPropertyName().equals("scene")) {
							setSelectedScene(Ctx.project.getSelectedScene());
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
							setSelectedActor(null);
						} else if (e.getPropertyName()
								.equals("frame_animation")) {
							createAndSelectActor(Ctx.project.getSelectedActor());
							setSelectedFA(null);
						} else if (e.getPropertyName().equals(
								"init_frame_animation")) {
							String initFA = (String) e.getNewValue();
							((SpriteActor) selectedActor).getRenderer()
									.setInitFrameAnimation(initFA);
							setSelectedFA(null);
						} else if (e.getPropertyName().equals("actor")) {
							createAndSelectActor((Element) e.getNewValue());
						} else if (e.getPropertyName().equals(
								BaseDocument.NOTIFY_ELEMENT_DELETED)) {
							if (((Element) e.getNewValue()).getTagName()
									.equals("actor"))
								removeActor(doc, (Element) e.getNewValue());
							else if (((Element) e.getNewValue()).getTagName()
									.equals("frame_animation"))
								setSelectedFA(null);
						}
					}
				});
		
		
		walkZoneWindow = new WalkZoneWindow(skin);
	}

	@Override
	public void act(float delta) {
		faRenderer.update(delta);

		if (scn != null && animation && !loading) {
			scn.update(delta);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate();

		Color tmp = batch.getColor();
		batch.setColor(Color.WHITE);

		// BACKGROUND
		batch.disableBlending();
		tile.draw(batch, getX(), getY(), getWidth(), getHeight());
		batch.enableBlending();

		if (scn != null && !loading) {
			Vector3 v = new Vector3(getX(), getY(), 0);
			v = v.prj(batch.getTransformMatrix());

			batch.end();

			// System.out.println("X: " + v.x+ " Y:" + v.y);
			Gdx.gl.glViewport((int) v.x, (int) v.y, (int) getWidth(),
					(int) (getHeight()));

			getStage().calculateScissors(bounds, scissors);

			if (ScissorStack.pushScissors(scissors)) {
				// WORLD CAMERA
				sceneBatch.setProjectionMatrix(scn.getCamera().combined);
				sceneBatch.begin();
				scn.draw(sceneBatch);
				sceneBatch.end();
				ScissorStack.popScissors();
			}
			
			if(scn.getPolygonalNavGraph() != null) {
				drawer.drawPolygon(scn.getPolygonalNavGraph().getWalkZone(), Color.GREEN);
				
				ArrayList<Polygon> obstacles = scn.getPolygonalNavGraph().getObstacles();
				
				for(Polygon p: obstacles) {
					drawer.drawPolygon(p, Color.RED);
				}
			}

			drawer.drawBGBounds();
			drawer.drawBBoxActors(scn);

			if (selectedActor != null) {
				drawer.drawSelectedActor(selectedActor);
			}

			getStage().getViewport().update();

			// SCREEN CAMERA
			batch.begin();

			if (!inScene) {
				faRenderer.draw((SpriteBatch) batch);
			}
			
			// DRAW COORDS
			Vector2 coords = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			screenToWorldCoords(coords);
			String str = MessageFormat.format("({0}, {1})",
						(int) coords.x, (int) coords.y);
			
			TextBounds bounds2 = defaultFont.getBounds(str);
			RectangleRenderer.draw((SpriteBatch)batch, 0f, getY() + getHeight() - bounds2.height - 15, 
					bounds2.width + 10, bounds2.height + 10, BLACK_TRANSPARENT);
			defaultFont.draw(batch, str, 5, getHeight() + getY() - 10);

			batch.setColor(tmp);

		} else {
			RectangleRenderer.draw((SpriteBatch) batch, getX(), getY(),
					getWidth(), getHeight(), Color.BLACK);

			String s;

			if (loading) {
				s = "LOADING...";

				if (!EngineAssetManager.getInstance().isLoading()) {
					loading = false;

					try {
						scn.retrieveAssets();
					} catch (Exception e) {
						Ctx.msg.show(getStage(),
								"Could not load assets for scene", 4);
						e.printStackTrace();
					}

					drawer.setCamera(scn.getCamera());

					invalidate();
				}
			} else if (Ctx.project.getProjectDir() == null) {
				s = "CREATE OR LOAD A PROJECT";
			} else {
				s = "THERE ARE NO SCENES IN THIS CHAPTER YET";
			}

			bigFont.draw(batch, s,
					(getWidth() - bigFont.getBounds(s).width) / 2, getHeight()
							/ 2 + bigFont.getLineHeight() * 3);

		}

	}

	public void setInSceneSprites(boolean v) {
		inScene = v;
	}

	public void setAnimation(boolean v) {
		animation = v;
	}

	public void setFrameAnimation(FrameAnimation fa) {
		try {
			faRenderer.setFrameAnimation(fa);
		} catch (Exception e) {
			Ctx.msg.show(getStage(), "Could not retrieve assets for sprite: "
					+ fa.id, 4);
			e.printStackTrace();

			faRenderer.setFrameAnimation(null);
		}
	}
	
	public void showEditWalkZoneWindow() {
		getParent().addActor(walkZoneWindow);
	}
	

	public void hideEditWalkZoneWindow() {
		getParent().removeActor(walkZoneWindow);
	}

	@Override
	public void layout() {
		EditorLogger.debug("LAYOUT SIZE CHANGED - X: " + getX() + " Y: "
				+ getY() + " Width: " + getWidth() + " Height: " + getHeight());
		EditorLogger.debug("Last Point coords - X: " + (getX() + getWidth())
				+ " Y: " + (getY() + getHeight()));
		localToScreenCoords(tmpV2
				.set(getX() + getWidth(), getY() + getHeight()));
		EditorLogger.debug("Screen Last Point coords:  " + tmpV2);

		faRenderer.setViewport(getWidth(), getHeight());
		bounds.set(getX(), getY(), getWidth(), getHeight());

		// SETS WORLD CAMERA
		if (scn != null) {

			float aspect = getWidth() / getHeight();

			float wWidth = Ctx.project.getWorld().getWidth();
			float wHeight = Ctx.project.getWorld().getHeight();
			float aspectWorld = wWidth / wHeight;

			if (aspectWorld > aspect) {
				wHeight = wWidth / aspect;
			} else {
				wWidth = wHeight * aspect;
			}

			zoomLevel = 100;

			scn.getCamera().setToOrtho(false, wWidth, wHeight);
			// worldCamera.translate(-width / 2, -height / 2, 0);
			scn.getCamera().zoom = 1f;
			scn.getCamera().update();

			translate(new Vector2((-getWidth() + wWidth) / 2,
					(-getHeight() + wHeight) / 2));
		}
		
		walkZoneWindow.setPosition(getX() + 5, getY() + 5);
		walkZoneWindow.invalidate();
	}

	public void zoom(int amount) {
		if (zoomLevel == zoomLevels[0] && amount < 0) {
			zoomLevel = zoomLevels[1];
		} else if (zoomLevel == zoomLevels[zoomLevels.length - 1] && amount > 0) {
			zoomLevel = zoomLevels[zoomLevels.length - 2];
		} else {
			for (int i = 1; i < zoomLevels.length - 1; i++) {
				if (zoomLevels[i] == zoomLevel) {
					zoomLevel = amount > 0 ? zoomLevels[i - 1]
							: zoomLevels[i + 1];
					break;
				}
			}
		}

		if (scn != null) {
			scn.getCamera().zoom = 100f / zoomLevel;
			scn.getCamera().update();
		}
	}

	public void translate(Vector2 delta) {
		// EditorLogger.debug("TRANSLATING - X: " + delta.x + " Y: " + delta.y);
		if (scn != null) {
			scn.getCamera().translate(-delta.x, -delta.y, 0);
			scn.getCamera().update();
		}
	}

	public void localToScreenCoords(Vector2 coords) {
		localToStageCoordinates(coords);
		getStage().stageToScreenCoordinates(coords);
	}

	public void localToWorldCoords(Vector2 coords) {
		localToStageCoordinates(coords);
		getStage().stageToScreenCoordinates(coords);

		tmpV3.set(coords.x, coords.y, 0);
		getScene().getCamera().unproject(tmpV3, getX(), getY(), getWidth(),
				getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}

	public void screenToWorldCoords(Vector2 coords) {
		tmpV2.set(0, 0);
		localToStageCoordinates(tmpV2);
		// getStage().stageToScreenCoordinates(tmpV2);
		tmpV3.set(coords.x, coords.y, 0);
		getScene().getCamera().unproject(tmpV3, tmpV2.x, tmpV2.y, getWidth(),
				getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}

	public Scene getScene() {
		return scn;
	}

	public void setSelectedScene(Element e) {
		if (scn != null) {
			scn.dispose();
			scn = null;
		}

		setSelectedActor(null);

		if (e != null) {
			scn = Ctx.project.getSelectedChapter().getEngineScene(e,
					Ctx.project.getWorld().getWidth(),
					Ctx.project.getWorld().getHeight());

			scn.loadAssets();
			loading = true;
		}
		
		walkZoneWindow.setScene(scn);
	}

	public void setSelectedActor(Element actor) {
		Actor a = null;

		if (scn != null && actor != null) {
			a = scn.getActor(Ctx.project.getSelectedChapter().getId(actor),
					false, true);
		}

		selectedActor = a;
		faRenderer.setActor(a);
		setFrameAnimation(null);
	}

	public void setSelectedFA(String selFA) {
		if (selectedActor instanceof SpriteActor) {
			SpriteRenderer s = ((SpriteActor) selectedActor).getRenderer();

			if (selFA == null || s.getFrameAnimations().get(selFA) == null) {
				selFA = ((SpriteActor) selectedActor).getRenderer()
						.getInitFrameAnimation();
			}

			if (selFA != null && s.getFrameAnimations().get(selFA) != null) {

				setFrameAnimation(s.getFrameAnimations().get(selFA));

				if (inScene
						|| s.getCurrentFrameAnimation() == null
						|| ((SpriteActor) selectedActor).getRenderer()
								.getInitFrameAnimation().equals(selFA)) {
					try {

						((SpriteActor) selectedActor).startFrameAnimation(
								selFA, Tween.REPEAT, Tween.INFINITY, null);
					} catch (Exception e) {
						setFrameAnimation(null);
						((SpriteActor) selectedActor).getRenderer().getFrameAnimations().remove(selFA);
					}
				}
			} else {
				setFrameAnimation(null);
			}
		} else {
			setFrameAnimation(null);
		}
	}

	private void createAndSelectActor(Element actor) {
		removeActor(Ctx.project.getSelectedChapter(), actor);
		selectedActor = createActor(Ctx.project.getSelectedChapter(), actor);
		setSelectedActor(actor);
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
			setSelectedActor(null);
		}
	}

	public void dispose() {
		if (scn != null) {
			scn.dispose();
			scn = null;
		}

		faRenderer.dispose();
	}

}
