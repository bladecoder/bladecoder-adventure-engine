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
package com.bladecoder.engineeditor.scneditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.scneditor.ScnWidgetInputListener.DraggingModes;

public class ScnWidget extends Widget {
	private static final Color BLACK_TRANSPARENT = new Color(0f, 0f, 0f, 0.5f);

	// TMPs to avoid GC calls
	private final Vector3 tmpV3 = new Vector3();
	private final Vector2 tmpV2 = new Vector2();
	private final Vector2 tmp2V2 = new Vector2();
	private final Vector3 tmpV3Draw = new Vector3();
	private final Vector2 tmpV2Transform = new Vector2();

	private final SpriteBatch sceneBatch = new SpriteBatch();
	private final CanvasDrawer drawer = new CanvasDrawer();
	private final AnimationDrawer faRenderer = new AnimationDrawer();
	private final ScnWidgetInputListener inputListner = new ScnWidgetInputListener(this);

	private final Rectangle bounds = new Rectangle();
	private final Rectangle scissors = new Rectangle();

	private Scene scn;
	private BaseActor selectedActor = null;
	private boolean inScene = false;
	private boolean animation = true;

	private static final int[] zoomLevels = { 5, 10, 16, 25, 33, 50, 66, 100, 150, 200, 300, 400, 600, 800, 1000 };
	private int zoomLevel = 100;

	private BitmapFont bigFont;
	private BitmapFont defaultFont;
	private TiledDrawable tile;
	private Drawable background;

	private boolean loading = false;
	private boolean loadingError = false;

	private boolean showSpriteBounds = true;

	private final GlyphLayout textLayout = new GlyphLayout();

	private final OrthographicCamera camera = new OrthographicCamera();

	private final TextureRegion scnMoveIcon;
	private final TextureRegion scnRotateIcon;
	private final TextureRegion scnScaleLockIcon;
	private final TextureRegion scnScaleIcon;

	/**
	 * The NOTIFY_PROJECT_LOADED listener is called from other thread. This flag is
	 * to recreate the scene in the OpenGL thread.
	 */
	private boolean projectLoadedFlag = false;

	public ScnWidget(Skin skin) {
		bigFont = skin.get("big-font", BitmapFont.class);
		defaultFont = skin.get("default-font", BitmapFont.class);

		setSize(150, 150);

		tile = new TiledDrawable(Ctx.assetManager.getIcon("transparent-light"));
		background = skin.getDrawable("background");

		faRenderer.setViewport(getWidth(), getHeight());

		setLayoutEnabled(true);

		addListener(inputListner);

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				EditorLogger.debug("ScnWidget Listener: " + e.getPropertyName());

				if (e.getPropertyName().equals(Project.NOTIFY_SCENE_SELECTED)) {
					if (!projectLoadedFlag)
						setSelectedScene(Ctx.project.getSelectedScene());
				} else if (e.getPropertyName().equals(Project.NOTIFY_ACTOR_SELECTED)) {
					if (!projectLoadedFlag)
						setSelectedActor(Ctx.project.getSelectedActor());
				} else if (e.getPropertyName().equals(Project.NOTIFY_ANIM_SELECTED)) {
					if (!projectLoadedFlag && Ctx.project.getSelectedFA() != null)
						setSelectedFA(Ctx.project.getSelectedFA());
				} else if (e.getPropertyName().equals(Project.NOTIFY_PROJECT_LOADED)) {
					projectLoadedFlag = true;
				} else if (e.getPropertyName().equals("scene")) {
					setSelectedScene(Ctx.project.getSelectedScene());
					setSelectedActor(Ctx.project.getSelectedActor());
				} else if (e.getPropertyName().equals("init_animation")) {
					if (!inScene)
						setSelectedFA(null);
				}
			}
		});

		showSpriteBounds = Boolean
				.parseBoolean(Ctx.project.getEditorConfig().getProperty("view.showSpriteBounds", "true"));
		inScene = Boolean.parseBoolean(Ctx.project.getEditorConfig().getProperty("view.inScene", "false"));
		animation = Boolean.parseBoolean(Ctx.project.getEditorConfig().getProperty("view.animation", "true"));

		scnMoveIcon = Ctx.assetManager.getIcon("scn_move");
		scnRotateIcon = Ctx.assetManager.getIcon("scn_rotate");
		scnScaleLockIcon = Ctx.assetManager.getIcon("scn_scale_lock");
		scnScaleIcon = Ctx.assetManager.getIcon("scn_scale");
	}

	public OrthographicCamera getCamera() {
		return camera;
	}

	@Override
	public void act(float delta) {
		if (projectLoadedFlag) {
			projectLoadedFlag = false;

			if (scn != null) {
				scn.dispose();
				scn = null;
			}

			setSelectedScene(Ctx.project.getSelectedScene());
			setSelectedActor(Ctx.project.getSelectedActor());
			setSelectedFA(Ctx.project.getSelectedFA());
		}

		if (scn != null && !loading && !loadingError) {
			if (!inScene)
				faRenderer.update(delta);
			// scn.update(delta);

			for (SceneLayer layer : scn.getLayers())
				layer.update();

			if (animation) {
				for (BaseActor a : scn.getActors().values()) {
					boolean v = a.isVisible();
					a.setVisible(true);
					a.update(delta);
					a.setVisible(v);
				}
			}

			handleKeyPositioning();
		}
	}

	private void handleKeyPositioning() {

		if (getStage() == null || getStage().getKeyboardFocus() != this)
			return;

		if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.LEFT)
				|| Gdx.input.isKeyPressed(Keys.RIGHT)) {

			BaseActor selActor = getSelectedActor();

			if (selActor == null)
				return;

			if (Gdx.input.isKeyPressed(Keys.UP))
				// p.translate(0, 1);
				selActor.setPosition(selActor.getX(), selActor.getY() + 1);
			else if (Gdx.input.isKeyPressed(Keys.DOWN))
				// p.translate(0, -1);
				selActor.setPosition(selActor.getX(), selActor.getY() - 1);
			else if (Gdx.input.isKeyPressed(Keys.LEFT))
				// p.translate(-1, 0);
				selActor.setPosition(selActor.getX() - 1, selActor.getY());
			else if (Gdx.input.isKeyPressed(Keys.RIGHT))
				// p.translate(1, 0);
				selActor.setPosition(selActor.getX() + 1, selActor.getY());
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate();

		float tmp = batch.getPackedColor();
		batch.setColor(Color.WHITE);

		if (scn != null && !loading && !loadingError) {
			// BACKGROUND
			batch.disableBlending();
			tile.draw(batch, getX(), getY(), getWidth(), getHeight());
			batch.enableBlending();

			Vector3 v = tmpV3Draw.set(getX(), getY(), 0);
			v = v.prj(batch.getTransformMatrix());

			batch.end();

			HdpiUtils.glViewport((int) v.x, (int) v.y, (int) getWidth(), (int) (getHeight()));

			getStage().calculateScissors(bounds, scissors);

			if (ScissorStack.pushScissors(scissors)) {
				// WORLD CAMERA
				sceneBatch.setProjectionMatrix(camera.combined);
				sceneBatch.begin();

				Array<AtlasRegion> scnBackground = scn.getBackground();

				if (scnBackground != null) {
					sceneBatch.disableBlending();

					float x = 0;

					for (AtlasRegion tile : scnBackground) {
						sceneBatch.draw(tile, x, 0f);
						x += tile.getRegionWidth();
					}

					sceneBatch.enableBlending();
				}

				// draw layers from bottom to top
				List<SceneLayer> layers = scn.getLayers();
				for (int i = layers.size() - 1; i >= 0; i--) {
					SceneLayer layer = layers.get(i);

					if (!layer.isVisible())
						continue;

					List<InteractiveActor> actors = layer.getActors();

					for (InteractiveActor a : actors) {
						if (a instanceof SpriteActor && Ctx.project.isEditorVisible(a)) {
							boolean visibility = a.isVisible();
							a.setVisible(true);
							((SpriteActor) a).draw(sceneBatch);
							a.setVisible(visibility);
						}
					}
				}

				sceneBatch.end();
				ScissorStack.popScissors();
			}

			drawer.drawBGBounds();
			drawer.drawBBoxActors(scn, showSpriteBounds);

			if (selectedActor != null) {
				drawer.drawSelectedActor(selectedActor);
			}

			getStage().getViewport().apply();

			// SCREEN CAMERA
			batch.begin();

			drawFakeDepthMarkers((SpriteBatch) batch);

			if (!inScene) {
				faRenderer.draw((SpriteBatch) batch);
			}

			// DRAW selected actor ICONS
			if (selectedActor != null) {
				drawTransformIcons((SpriteBatch) batch, selectedActor);
			}

			// DRAW COORDS
			Vector2 coords = tmpV2Transform.set(Gdx.input.getX(), Gdx.input.getY());
			screenToWorldCoords(coords);
			String str = MessageFormat.format("({0}, {1})", (int) coords.x, (int) coords.y);

			textLayout.setText(defaultFont, str);

			RectangleRenderer.draw(batch, 0f, getY() + getHeight() - textLayout.height - 15, textLayout.width + 10,
					textLayout.height + 10, BLACK_TRANSPARENT);
			defaultFont.draw(batch, textLayout, 5, getHeight() + getY() - 10);

			batch.setPackedColor(tmp);

		} else {
			background.draw(batch, getX(), getY(), getWidth(), getHeight());

			String s;

			if (loading) {
				s = "LOADING...";

				Timer.post(new Task() {
					@Override
					public void run() {
						loading = false;

						try {

							EngineAssetManager.getInstance().finishLoading();

							scn.retrieveAssets();

							// disable Spine events
							for (BaseActor a : scn.getActors().values()) {
								if (a instanceof SpriteActor
										&& ((SpriteActor) a).getRenderer() instanceof SpineRenderer) {
									((SpineRenderer) ((SpriteActor) a).getRenderer()).enableEvents(false);
								}
							}

							drawer.setCamera(camera);

							invalidate();
						} catch (Exception e) {
							Message.showMsg(getStage(), "Could not load assets for scene", 4);
							EditorLogger.printStackTrace(e);
							loadingError = true;
							loading = false;
						}
					}
				});

			} else if (loadingError) {
				s = "ERROR IN SCENE DATA. CANNOT DISPLAY SCENE";
			} else if (!Ctx.project.isLoaded()) {
				s = "CREATE OR LOAD A PROJECT";
			} else {
				s = "THERE ARE NO SCENES IN THIS CHAPTER YET";
			}

			textLayout.setText(bigFont, s);

			bigFont.draw(batch, textLayout, (getWidth() - textLayout.width) / 2,
					getHeight() / 2 + bigFont.getLineHeight() * 3);

		}

	}

	private void drawTransformIcons(SpriteBatch batch, BaseActor a) {
		Polygon p = a.getBBox();

		if (!(a instanceof AnchorActor)) {

			if (a instanceof InteractiveActor) {
				InteractiveActor ia = (InteractiveActor) a;

				if (!scn.getLayer(ia.getLayer()).isVisible())
					return;
			}

			Rectangle r = p.getBoundingRectangle();

			worldToScreenCoords(tmpV2Transform.set(r.x, r.y));

			float x = tmpV2Transform.x;
			float y = tmpV2Transform.y;

			worldToScreenCoords(tmpV2Transform.set(r.x + r.width, r.y + r.height));

			float x2 = tmpV2Transform.x;
			float y2 = tmpV2Transform.y;

			batch.draw(scnMoveIcon, x + (x2 - x - scnMoveIcon.getRegionWidth()) / 2, y2);

			if (a instanceof SpriteActor) {
				batch.draw(scnRotateIcon, x2 - scnRotateIcon.getRegionWidth() / 3,
						y2 - scnRotateIcon.getRegionHeight() / 3);

				if (!((SpriteActor) a).getFakeDepth()) {
					batch.draw(scnScaleLockIcon, x - scnScaleLockIcon.getRegionWidth(), y2);
					batch.draw(scnScaleIcon, x - scnScaleIcon.getRegionWidth(), y - scnScaleIcon.getRegionHeight());
				}
			}
		}
	}

	public boolean inTransformIcon(float px, float py, DraggingModes dm) {
		Polygon p = selectedActor.getBBox();

		Rectangle r = p.getBoundingRectangle();

		worldToScreenCoords(tmpV2Transform.set(r.x, r.y));

		float x = tmpV2Transform.x;
		float y = tmpV2Transform.y;

		worldToScreenCoords(tmpV2Transform.set(r.x + r.width, r.y + r.height));

		float x2 = tmpV2Transform.x;
		float y2 = tmpV2Transform.y;

		Rectangle r2 = null;

		if (dm == DraggingModes.ROTATE_ACTOR) {
			r2 = new Rectangle(x2 - scnRotateIcon.getRegionWidth() / 3, y2 - scnRotateIcon.getRegionHeight() / 3,
					scnRotateIcon.getRegionWidth(), scnRotateIcon.getRegionHeight());
		} else if (dm == DraggingModes.SCALE_ACTOR) {
			r2 = new Rectangle(x - scnScaleIcon.getRegionWidth(), y - scnScaleIcon.getRegionHeight(),
					scnScaleIcon.getRegionWidth(), scnScaleIcon.getRegionHeight());
		} else if (dm == DraggingModes.SCALE_LOCK_ACTOR) {
			r2 = new Rectangle(x - scnScaleLockIcon.getRegionWidth(), y2, scnScaleLockIcon.getRegionWidth(),
					scnScaleLockIcon.getRegionHeight());
		} else if (dm == DraggingModes.DRAGGING_ACTOR) {
			r2 = new Rectangle(x + (x2 - x - scnMoveIcon.getRegionWidth()) / 2, y2, scnMoveIcon.getRegionWidth(),
					scnMoveIcon.getRegionHeight());
		}

		worldToScreenCoords(tmpV2Transform.set(px, py));

		return r2.contains(tmpV2Transform.x, tmpV2Transform.y);
	}

	private void drawFakeDepthMarkers(SpriteBatch batch) {
		int margin = 5;

		Vector2 d = scn.getDepthVector();

		if (d == null)
			return;

		tmp2V2.x = 0;
		tmp2V2.y = d.y;
		worldToScreenCoords(tmp2V2);

		String s = "100%";

		textLayout.setText(defaultFont, s);

		float posx = tmp2V2.x - textLayout.width - 20;

		RectangleRenderer.draw(batch, posx, tmp2V2.y, textLayout.width + margin * 2, textLayout.height + margin * 2,
				Color.BLACK);
		RectangleRenderer.draw(batch, tmp2V2.x - 20, tmp2V2.y, 20, 2, Color.BLACK);

		defaultFont.draw(batch, textLayout, posx + margin, tmp2V2.y + textLayout.height + margin);

		tmp2V2.x = 0;
		tmp2V2.y = d.x;
		worldToScreenCoords(tmp2V2);
		s = "0%";

		textLayout.setText(defaultFont, s);

		posx = tmp2V2.x - textLayout.width - 20;

		RectangleRenderer.draw(batch, posx, tmp2V2.y, textLayout.width + margin * 2, textLayout.height + margin * 2,
				Color.BLACK);
		RectangleRenderer.draw(batch, tmp2V2.x - 20, tmp2V2.y, 20, 2, Color.BLACK);

		defaultFont.draw(batch, textLayout, posx + margin, tmp2V2.y + textLayout.height + margin);

	}

	public void setInSceneSprites(boolean v) {
		inScene = v;
		Ctx.project.getEditorConfig().setProperty("view.inScene", Boolean.toString(inScene));

		if (!inScene)
			setSelectedFA(null);
	}

	public boolean getInSceneSprites() {
		return inScene;
	}

	public void setAnimation(boolean v) {
		animation = v;
		Ctx.project.getEditorConfig().setProperty("view.animation", Boolean.toString(animation));
	}

	public boolean getAnimation() {
		return animation;
	}

	public void setAnimationRenderer(BaseActor a, AnimationDesc fa) {
		try {
			faRenderer.setActor(a);
			faRenderer.setAnimation(fa);
		} catch (Exception e) {
			Message.showMsg(getStage(), "Could not retrieve assets for sprite: " + fa.id, 4);
			EditorLogger.printStackTrace(e);

			faRenderer.setAnimation(null);
		}
	}

	public boolean getShowSpriteBounds() {
		return showSpriteBounds;
	}

	public void setShowSpriteBounds(boolean v) {
		showSpriteBounds = v;
		Ctx.project.getEditorConfig().setProperty("view.showSpriteBounds", Boolean.toString(showSpriteBounds));
	}

	@Override
	public void layout() {
		// EditorLogger.debug("LAYOUT SIZE CHANGED - X: " + getX() + " Y: "
		// + getY() + " Width: " + getWidth() + " Height: " + getHeight());
		// EditorLogger.debug("Last Point coords - X: " + (getX() + getWidth())
		// + " Y: " + (getY() + getHeight()));
		localToScreenCoords(tmpV2.set(getX() + getWidth(), getY() + getHeight()));
		// EditorLogger.debug("Screen Last Point coords: " + tmpV2);

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

			camera.setToOrtho(false, wWidth, wHeight);
			camera.zoom = 1f;
			camera.position.set(Ctx.project.getWorld().getWidth() / 2, Ctx.project.getWorld().getHeight() / 2, 0);
			camera.update();
			zoom(+1);
		}
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

		if (scn != null) {
			camera.zoom = 100f / zoomLevel;
			camera.update();
		}
	}

	public void translate(Vector2 delta) {
		// EditorLogger.debug("TRANSLATING - X: " + delta.x + " Y: " + delta.y);
		if (scn != null) {
			camera.translate(-delta.x, -delta.y, 0);
			camera.update();
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
		camera.unproject(tmpV3, getX(), getY(), getWidth(), getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}

	public void screenToWorldCoords(Vector2 coords) {
		tmpV2.set(0, 0);
		localToStageCoordinates(tmpV2);
		// getStage().stageToScreenCoordinates(tmpV2);
		tmpV3.set(coords.x, coords.y, 0);
		camera.unproject(tmpV3, tmpV2.x, tmpV2.y, getWidth(), getHeight());
		coords.set(tmpV3.x, tmpV3.y);
	}

	public void worldToScreenCoords(Vector2 coords) {
		tmpV2.set(getX(), getY());
		localToStageCoordinates(tmpV2);
		tmpV3.set(coords.x, coords.y, 0);
		camera.project(tmpV3, tmpV2.x, tmpV2.y, getWidth(), getHeight());
		coords.set(tmpV3.x, tmpV3.y);
		stageToLocalCoordinates(coords);
	}

	public boolean inScaleIcon(float px, float py) {
		Polygon p = selectedActor.getBBox();

		if (selectedActor instanceof SpriteActor) {

			InteractiveActor ia = (InteractiveActor) selectedActor;

			if (!scn.getLayer(ia.getLayer()).isVisible())
				return false;

			Rectangle r = p.getBoundingRectangle();

			worldToScreenCoords(tmpV2Transform.set(r.x + r.width, r.y + r.height));

			float x = tmpV2Transform.x;
			float y = tmpV2Transform.y;

			Rectangle r2 = new Rectangle(x - scnMoveIcon.getRegionWidth() / 2, y, scnMoveIcon.getRegionWidth(),
					scnMoveIcon.getRegionHeight());

			worldToScreenCoords(tmpV2Transform.set(px, py));

			return r2.contains(tmpV2Transform.x, tmpV2Transform.y);
		}

		return false;
	}

	public Scene getScene() {
		return scn;
	}

	public BaseActor getSelectedActor() {
		return selectedActor;
	}

	public void setSelectedScene(Scene s) {
		if (scn != null) {
			scn.dispose();
			faRenderer.dispose();
			scn = null;

			EngineAssetManager.getInstance().clear();
		}

		loadingError = false;

		setSelectedActor(null);

		if (s != null) {
			scn = s;

			scn.loadAssets();
			loading = true;
		}

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

			camera.setToOrtho(false, wWidth, wHeight);
			camera.zoom = 1f;
			camera.update();

			// translate(new Vector2((-getWidth() + wWidth ) / 2 *
			// camera.zoom,
			// (-getHeight() + wHeight) / 2 * camera.zoom));

			translate(new Vector2(0, (-getHeight() + wHeight) / 2));
		}
	}

	public void setSelectedActor(BaseActor actor) {
		BaseActor a = null;

		if (scn != null && actor != null) {
			a = actor;
		}

		selectedActor = a;
		// faRenderer.setActor(a);
		setAnimationRenderer(null, null);
	}

	public void setSelectedFA(String selFA) {
		if (selectedActor instanceof SpriteActor
				&& ((SpriteActor) selectedActor).getRenderer() instanceof AnimationRenderer) {
			AnimationRenderer s = (AnimationRenderer) ((SpriteActor) selectedActor).getRenderer();

			if (selFA == null || (s.getAnimations().get(selFA) == null
					&& s.getAnimations().get(AnimationRenderer.getFlipId(selFA)) == null)) {
				selFA = s.getInitAnimation();
			}

			if (selFA != null && (s.getAnimations().get(selFA) != null
					|| s.getAnimations().get(AnimationRenderer.getFlipId(selFA)) != null)) {

				setAnimationRenderer(selectedActor, s.getAnimations().get(selFA));

				String animInScene = selFA;
				if (!inScene && s.getInitAnimation() != null)
					animInScene = s.getInitAnimation();

				try {
					((SpriteActor) selectedActor).startAnimation(animInScene, Tween.Type.REPEAT, Tween.INFINITY, null);
				} catch (Exception e) {
					setAnimationRenderer(selectedActor, null);
					s.getAnimations().remove(selFA);
				}
			} else {
				setAnimationRenderer(selectedActor, null);
			}
		} else {
			setAnimationRenderer(selectedActor, null);
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
