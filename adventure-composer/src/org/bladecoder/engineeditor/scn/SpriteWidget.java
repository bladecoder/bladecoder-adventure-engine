package org.bladecoder.engineeditor.scn;

import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.AtlasRenderer;
import org.bladecoder.engine.model.SpineRenderer;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.EditSpriteDialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

public class SpriteWidget extends Widget {
	private String source;
	private FrameAnimation fa;
	private SpriteRenderer renderer;
	EditSpriteDialog editFADialog;
	

	public SpriteWidget(EditSpriteDialog createEditFADialog) {
		this.editFADialog = createEditFADialog;
	}

	
	public void setSource(String type, String source) {
		this.source = source;
		
		if(renderer != null) {
			renderer.dispose();
			renderer = null;
		}
		
		
		if(type.equals(ChapterDocument.SPRITE3D_ACTOR_TYPE)) {
			renderer = new Sprite3DRenderer();
			((Sprite3DRenderer)renderer).setSpriteSize(new Vector2( Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		} else if(type.equals(ChapterDocument.SPINE_ACTOR_TYPE)) {
			renderer = new SpineRenderer();
		} else {
			renderer = new AtlasRenderer();
		}
		
		renderer.loadAssets();
		EngineAssetManager.getInstance().getManager().finishLoading();
		renderer.retrieveAssets();
	}
	
	public String[] getAnimations() {
		return renderer.getInternalAnimations(source);
	}
	
	public void setFrameAnimation(String id, String speedStr, String typeStr) {
		if (source!=null && id != null && !source.isEmpty() && !id.isEmpty()) {
			
			int type = Tween.REPEAT;
			float speed = 2.0f;
			
			if(!speedStr.isEmpty())
				speed = Float.parseFloat(speedStr);
			
			if(typeStr.equals("yoyo"))
				type = Tween.PINGPONG;
			
			if(renderer instanceof AtlasRenderer)
				fa = new AtlasFrameAnimation();
			else 
				fa = new FrameAnimation();
			
			fa.set(id, source, speed, 0.0f, Tween.INFINITY, type,
					null, null, null, false, true);
			
			renderer.addFrameAnimation(fa);
			
			renderer.startFrameAnimation(fa.id, Tween.FROM_FA, 1, null);
		}
	}
	
	public void draw (Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);
		
		if(renderer == null || renderer.getCurrentFrameAnimation() == null)
			return;
		
		Color tmp = batch.getColor();
		batch.setColor(Color.WHITE);
		
		renderer.update(Gdx.graphics.getDeltaTime());
		
		RectangleRenderer.draw((SpriteBatch)batch, getX(), getY(), getWidth(), getHeight(), Color.MAGENTA);
		
		float scalew =   getWidth() /  renderer.getWidth();
		float scaleh =   getHeight() /  renderer.getHeight();
		float scale = scalew>scaleh?scaleh:scalew;

		renderer.draw((SpriteBatch)batch, getX() + renderer.getWidth() * scale /2, getY(), scale);
		batch.setColor(tmp);
	}

	public void dispose() {
		renderer.dispose();
	}
}
