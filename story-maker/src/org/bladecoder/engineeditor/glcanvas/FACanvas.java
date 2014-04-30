package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteAtlasRenderer;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.SpriteSpineRenderer;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.CreateEditFADialog;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FACanvas extends ApplicationAdapter {
	private OrthographicCamera screenCamera;
	private SpriteBatch batch;
	private String source;
	private FrameAnimation fa;
	private String type;
	private SpriteRenderer renderer;
	CreateEditFADialog createEditFADialog;
	
	private boolean sourceChanged = false;
	private boolean faChanged = false;
	

	public FACanvas(CreateEditFADialog createEditFADialog) {
		this.createEditFADialog = createEditFADialog;
	}

	@Override
	public void create() {
		screenCamera = new OrthographicCamera();

		batch = new SpriteBatch();
	}
	
	public void setSource(String type, String source) {
		this.source = source;
		this.type = type;
		this.sourceChanged = true;
	}
	
	public void setFrameAnimation(String id, String speedStr, String typeStr) {
		if (source!=null && id != null && !source.isEmpty() && !id.isEmpty()) {
			faChanged = true;
			
			int type = EngineTween.REPEAT;
			float speed = 2.0f;
			
			if(!speedStr.isEmpty())
				speed = Float.parseFloat(speedStr);
			
			if(typeStr.equals("yoyo"))
				type = EngineTween.YOYO;
			
			if(renderer instanceof SpriteAtlasRenderer)
				fa = new AtlasFrameAnimation();
			else 
				fa = new FrameAnimation();
			
			fa.set(id, source, speed, 0.0f, Tween.INFINITY, type,
					null, null, null, true, false);			
		}
	}
	
	private void testSourceChanged() {
		if(sourceChanged) {
			sourceChanged = false;
			
			if(renderer != null) {
				renderer.dispose();
				renderer = null;
			}
			
			
			if(type.equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
				renderer = new Sprite3DRenderer();
			} else if(type.equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
				renderer = new SpriteSpineRenderer();
			} else {
				renderer = new SpriteAtlasRenderer();
			}
			
			createEditFADialog.fillAnimations(renderer.getInternalAnimations(source));			
		}		
	}
	
	private void testFaChanged() {
		if(faChanged) {
			faChanged = false;
			
			renderer.addFrameAnimation(fa);
			
			renderer.retrieveAssets();
			
			renderer.startFrameAnimation(fa.id, EngineTween.FROM_FA, 1, null);			
		}
	}

	@Override
	public void render() {
		testSourceChanged();
		testFaChanged();
		
		if(renderer == null)
			return;
		
		renderer.update(Gdx.graphics.getDeltaTime());
		
		GL20 gl = Gdx.gl20;
		gl.glClearColor(Color.MAGENTA.r, Color.MAGENTA.g, Color.MAGENTA.b, 1);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		float scalew =   Gdx.graphics.getWidth() /  renderer.getWidth();
		float scaleh =   Gdx.graphics.getHeight() /  renderer.getHeight();

		// SCREEN CAMERA
		batch.setProjectionMatrix(screenCamera.combined);
		batch.begin();
		renderer.draw(batch, 0f, 0f, 0f, 0f, scalew>scaleh?scaleh:scalew);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		GL20 gl = Gdx.gl20;
		gl.glViewport(0, 0, width, height);
		resetCameras();
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
	
	@Override
	public void dispose() {
		renderer.dispose();
	}
}
