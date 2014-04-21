package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.UIAssetConsumer;
import org.bladecoder.engine.model.BaseActor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;

public class PieMenu implements TouchEventListener, UIAssetConsumer  {
	
	private boolean visible = false;
	
	private AtlasRegion piePiece;
	private AtlasRegion rightIcon;
	private AtlasRegion leftIcon;
	
	private AtlasRegion talktoIcon;
	private AtlasRegion pickupIcon;
	
	private float scale;
	private float x=0, y=0;
	private String selected;
	
	private BaseActor actor = null;
	
	private String rightVerb;
	private String leftVerb;
	
	Recorder recorder;
	
	private final static Color COLOR = new Color(0.0f, 0.0f, 0.0f, 0.7f);
	
	int viewPortWidth, viewPortHeight;
	
	public PieMenu(Recorder recorder) {
		this.recorder = recorder;
	}

	public void draw(SpriteBatch batch) {
		if(visible) {
			
			float rot, leftX, leftY, rightX, rightY;
			
			// check that the pie fits in the screen
			if(x < piePiece.getRegionWidth() * scale) {
				rot = -90;
				leftX = x + (piePiece.getRegionWidth() - leftIcon.getRegionWidth() ) * scale/2;
				leftY = y + (piePiece.getRegionHeight() - leftIcon.getRegionHeight() )*scale/2;
				rightX = x + (piePiece.getRegionWidth() - rightIcon.getRegionWidth() ) * scale/2;
				rightY = y - (piePiece.getRegionHeight() + rightIcon.getRegionHeight() )*scale/2;
			} else if(y > viewPortHeight - piePiece.getRegionHeight() * scale) {
				rot = -180;
				leftX = x - (piePiece.getRegionWidth() + leftIcon.getRegionWidth() ) * scale / 2;
				leftY = y - (piePiece.getRegionHeight() + leftIcon.getRegionHeight() )*scale/2;
				rightX = x + (piePiece.getRegionWidth() - rightIcon.getRegionWidth() ) * scale/2;
				rightY = y - (piePiece.getRegionHeight() + rightIcon.getRegionHeight() )*scale/2;
			} else {
				rot = 0;
				leftX = x - (piePiece.getRegionWidth() + leftIcon.getRegionWidth() ) * scale / 2;
				leftY = y + (piePiece.getRegionHeight() - leftIcon.getRegionHeight() ) * scale /2;
				rightX = x + (piePiece.getRegionWidth() - rightIcon.getRegionWidth() ) * scale/2;
				rightY = y + (piePiece.getRegionHeight() - rightIcon.getRegionHeight() )*scale/2;
			}
			
			
			if(selected != leftVerb)	batch.setColor(COLOR);
			else batch.setColor(Color.LIGHT_GRAY);
			
			batch.draw(piePiece, x - piePiece.getRegionWidth(), y,
					piePiece.getRegionWidth(), 0,
					piePiece.getRegionWidth(),piePiece.getRegionHeight(),scale,scale,rot);
			
			
			if(selected != rightVerb)	batch.setColor(COLOR);
			else batch.setColor(Color.LIGHT_GRAY);
			
			batch.draw(piePiece, x - piePiece.getRegionWidth(), y,
					piePiece.getRegionWidth(), 0,
					piePiece.getRegionWidth(),piePiece.getRegionHeight(),-scale,scale,rot);
			
			batch.setColor(Color.WHITE);
			
			batch.draw(leftIcon, 
					leftX, 
					leftY,
					0, 0,
					leftIcon.getRegionWidth(),leftIcon.getRegionHeight(),scale,scale,0);
			
			batch.draw(rightIcon, 
					rightX, 
					rightY,
					0, 0,
					rightIcon.getRegionWidth(),rightIcon.getRegionHeight(),scale,scale,0);
		}
	}
		
	public void hide() {
		visible = false;
		actor = null;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void show(BaseActor a, float x, float y) {
		visible = true;
		this.x = x;
		this.y = y;
		actor = a;
		
		leftVerb = "lookat";
		
		if(a.getVerb("talkto") != null) {
			rightVerb = "talkto";
			rightIcon = talktoIcon; 
		} else {
			rightVerb = "pickup";
			rightIcon = pickupIcon;
		}
	}
	
	@Override
	public void retrieveAssets(TextureAtlas atlas) {
		piePiece = atlas.findRegion("pie");
		//rightIcon = atlas.findRegion("pickup");
		leftIcon = atlas.findRegion("lookat");		
		
		talktoIcon = atlas.findRegion("talkto");
		pickupIcon = atlas.findRegion("pickup");
	}
	
	public void resize(Rectangle v) {
		viewPortWidth = (int)v.width;
		viewPortHeight = (int)v.height;
		
		scale = (viewPortHeight / 5) / piePiece.getRegionHeight();
		
		// the minimum height of the pie menu is 1/2"
		if (scale * piePiece.getRegionHeight() < 160.0f * Gdx.graphics.getDensity() / 2f) {
			scale = 160.0f * Gdx.graphics.getDensity() / 2f
					/ piePiece.getRegionHeight();
		}
	}
	
	@Override
	public void touchEvent(int type, float x2, float y2, int pointer, int button) {
		
		switch (type) {
		case TOUCH_UP:
					
			if(selected != null && actor != null) {
				if(recorder.isRecording()) {
					recorder.add(actor.getId(), selected, null);
				}
				
				actor.runVerb(selected);
			}
			
			hide();
			selected = null;
			break;
			
		case TOUCH_DOWN:
			Rectangle bboxLeft;
			Rectangle bboxRight;
			
			// check that the pie fits in the screen
			if(x < piePiece.getRegionWidth() * scale) { // exits for the left screen border
				bboxLeft = new Rectangle(x, y, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);
				bboxRight = new Rectangle(x, y - piePiece.getRegionWidth() * scale, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);				
			} else if(y > viewPortHeight - piePiece.getRegionHeight() * scale) {
				bboxLeft = new Rectangle(x - piePiece.getRegionWidth() * scale, y - piePiece.getRegionWidth() * scale, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);
				bboxRight = new Rectangle(x, y - piePiece.getRegionWidth() * scale, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);				
			} else {
				bboxLeft = new Rectangle(x - piePiece.getRegionWidth() * scale, y, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);
				bboxRight = new Rectangle(x, y, piePiece.getRegionWidth() * scale, piePiece.getRegionHeight()* scale);				
			}
			
			if(bboxLeft.contains(x2, y2)) {
				selected = leftVerb;				
			} else if(bboxRight.contains(x2, y2)) {
				selected = rightVerb;
			} else {
				selected = null;
			}			
			
			break;
		}
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void createAssets() {

	}

}
