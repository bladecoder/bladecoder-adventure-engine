package org.bladecoder.engineeditor.scneditor;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SceneCamera;
import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class CanvasDrawer {
	public static final float CORNER_DIST = 20;
	
	private static final Color MOUSESELECTION_FILL_COLOR = new Color(0.2f, 0.2f, 0.8f, 0.4f);
	private static final Color MOUSESELECTION_STROKE_COLOR = new Color(0.2f, 0.2f, 0.8f, 1f);

	private final ShapeRenderer drawer = new ShapeRenderer();
	private SceneCamera camera;

	public CanvasDrawer() {	
	}
	
	public void setCamera(SceneCamera camera) {
		this.camera = camera;		
	}
	
	public void drawBBoxActors(Scene scn) {
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		scn.drawBBoxLines(drawer);
	}


	public void drawSelectedActor(Actor selectedActor) {
		//Gdx.gl20.glLineWidth(3);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
//		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

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
//		Gdx.gl20.glEnable(GL20.GL_BLEND);
//		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		
		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(Color.MAGENTA);
		drawer.rect(0, 0, Ctx.project.getWorld().getWidth(), Ctx.project.getWorld().getHeight());
		drawer.end();		
	}
	
	public void drawPolygon(Polygon p, Color c) {
		float verts[] = p.getTransformedVertices();
		
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		
		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(c);
//		drawer.polygon(verts);
		
		for(int i = 0; i < verts.length;i+= 2)
			drawer.rect(verts[i] - CORNER_DIST / 2, verts[i+1] - CORNER_DIST / 2, CORNER_DIST, CORNER_DIST);
		
		drawer.end();		
	}
}
