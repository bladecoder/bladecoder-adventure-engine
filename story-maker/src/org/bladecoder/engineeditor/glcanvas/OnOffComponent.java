package org.bladecoder.engineeditor.glcanvas;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class OnOffComponent {	
	private static final float TEXT_MARGIN = 4;
	
	private static final Texture texOn = Assets.inst().get("res/images/on.png",
			Texture.class);
	
	private static final Texture texOff = Assets.inst().get("res/images/off.png",
			Texture.class);
	
	private static BitmapFont font = new BitmapFont();
	private static float textH = font.getLineHeight();
	
	private Rectangle bbox = new Rectangle();
	private boolean state = true;
	private String text;
	
	public OnOffComponent() {
		float width = texOn.getWidth();
		float height = texOn.getHeight();
		
		bbox.set(0,0,width,height);		
	}
		
	public void draw(SpriteBatch batch) {
		
		if(text != null) {
			font.draw(batch, text, bbox.x, bbox.y + textH);
		}
		
		Texture tex = state?texOn:texOff;
		
		float x = bbox.x;
		if(bbox.width > tex.getWidth()) {
			x += (bbox.width - tex.getWidth()) / 2;
		}
		
		batch.draw(tex, x, bbox.y + textH + TEXT_MARGIN);
	}


	public boolean getState() {
		return state;
	}


	public void setState(boolean on) {
		this.state = on;
	}
	
	public void setPos(float x, float y) {
		bbox.x = x;
		bbox.y = y;
	}
	
	public void setText(String t) {
		text = t;
		
		TextBounds b = font.getBounds(t);
		
		if(b.width > bbox.width) bbox.width=b.width;
		bbox.height += b.height + TEXT_MARGIN;
	}
	
	public void click(float x, float y) {
		if(bbox.contains(x,y)) {
			state = !state;
		}
	}
	
	public Rectangle getBbox() {
		return bbox;
	}
}
