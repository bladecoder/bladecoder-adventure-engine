package org.bladecoder.engineeditor.scn;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SceneCamera;
import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class CanvasDrawer {
	public static final float CORNER_DIST = 20;
	
	private static final Color MOUSESELECTION_FILL_COLOR = new Color(0.2f, 0.2f, 0.8f, 0.4f);
	private static final Color MOUSESELECTION_STROKE_COLOR = new Color(0.2f, 0.2f, 0.8f, 1f);
	private static final Color AXIS_COLOR = new Color(0.5f, 0.5f, 0.5f, 1);

	private final ShapeRenderer drawer = new ShapeRenderer();
	private SceneCamera camera;

	public CanvasDrawer() {	
	}
	
	public void setCamera(SceneCamera camera) {
		this.camera = camera;		
	}


	public void drawBoundingBox(Sprite sp) {
		if (sp == null) return;
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		drawBoundingBox(sp.getWidth(), sp.getHeight());
	}
	
	public void drawBBoxActors(Scene scn) {
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		scn.drawBBoxActors(drawer);
	}

	public void drawMouseSelection(Vector2 p1, Vector2 p2) {
		if (p1 == null || p2 == null) return;
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		drawMouseSelection(p1.x, p1.y, p2.x, p2.y);
	}

	private void drawBoundingBox(float w, float h) {
		Gdx.gl20.glLineWidth(1);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(AXIS_COLOR);
		drawer.rect(0, 0, w, h);
		drawer.end();
	}

	private void drawMouseSelection(float x1, float y1, float x2, float y2) {
		Gdx.gl20.glLineWidth(3);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		Rectangle rect = new Rectangle(
			Math.min(x1, x2), Math.min(y1, y2),
			Math.abs(x2 - x1), Math.abs(y2 - y1)
		);

		drawer.begin(ShapeRenderer.ShapeType.Filled);
		drawer.setColor(MOUSESELECTION_FILL_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);
		drawer.end();

		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(MOUSESELECTION_STROKE_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);
		drawer.end();
	}


	public void drawSelectedActor(Actor selectedActor) {
		//Gdx.gl20.glLineWidth(3);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		Rectangle rect = selectedActor.getBBox();
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());

		drawer.begin(ShapeRenderer.ShapeType.Filled);
		drawer.setColor(MOUSESELECTION_FILL_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);
		drawer.end();
		
		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(MOUSESELECTION_STROKE_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);
		
		// DRAW SELECTION BOUNDS
		drawer.rect(rect.x, rect.y, CORNER_DIST, CORNER_DIST);
		drawer.rect(rect.x + rect.width - CORNER_DIST, rect.y, CORNER_DIST,CORNER_DIST);
		drawer.rect(rect.x, rect.y + rect.height - CORNER_DIST, CORNER_DIST, CORNER_DIST);
		drawer.rect(rect.x + rect.width - CORNER_DIST, rect.y+ rect.height - CORNER_DIST, CORNER_DIST, CORNER_DIST);
		drawer.end();		
	}


	public void drawBGBounds() {		
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		
		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(Color.MAGENTA);
		drawer.rect(0, 0, Ctx.project.getWorld().getWidth(), Ctx.project.getWorld().getHeight());
		drawer.end();		
	}
}
