package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.assets.EngineAssetManager;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class FACanvas extends ApplicationAdapter {
	private OrthographicCamera screenCamera;
	private SpriteBatch batch;
	private AtlasFARenderer faRenderer;
	
	AtlasFrameAnimation fa;
	TextureAtlas ta;

	@Override
	public void create() {
//		Assets.inst().initialize();
//		Tween.registerAccessor(Sprite.class, new SpriteAccessor());

		screenCamera = new OrthographicCamera();

		batch = new SpriteBatch();
		faRenderer = new AtlasFARenderer();
	}

	public void setFrameAnimation(String atlas, String id, String speedStr, String typeStr) {
		if (atlas!=null && id != null && !atlas.isEmpty() && !id.isEmpty()) {
			int type = EngineTween.REPEAT;
			float speed = 2.0f;
			
			if(!speedStr.isEmpty())
				speed = Float.parseFloat(speedStr);
			
			if(typeStr.equals("yoyo"))
				type = EngineTween.YOYO;			
			
			fa = new AtlasFrameAnimation(id, atlas, speed, 0.0f, Tween.INFINITY, type,
					null, null, null);
		}
	}
	
	public void setFrameAnimationInternal() {
		if (fa != null) {
			if(ta != null)
				ta.dispose();
			
			ta = new TextureAtlas(EngineAssetManager.getInstance().getResAsset("atlases/" + fa.atlas + ".atlas"));
			
			fa.regions =  ta.findRegions(fa.id);
			faRenderer.setFrameAnimation(fa);
			fa = null;
		}
	}

	@Override
	public void render() {
		setFrameAnimationInternal();

		GL20 gl = Gdx.gl20;
		gl.glClearColor(.5f, .5f, .5f, 1);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// SCREEN CAMERA
		batch.setProjectionMatrix(screenCamera.combined);
		batch.begin();
		faRenderer.draw(batch);
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
		ta.dispose();
	}
}
