package org.bladecoder.engine.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class TextUtils {
	
	public static void drawCentered(SpriteBatch batch, BitmapFont font, CharSequence str, float x, float y) {
		float x2, y2;
		
		TextBounds b = font.getMultiLineBounds(str);
		
		x2 = x - b.width/2;
		y2 = y - b.height/2;
				
		font.drawMultiLine(batch, str, x2, y2, b.width, HAlignment.CENTER);
	}
	
	public static void drawCenteredScreenY(SpriteBatch batch, BitmapFont font, CharSequence str, float x, 
			int viewportHeight) {
		float y;
		
		TextBounds b = font.getMultiLineBounds(str);
	
		y = (viewportHeight - b.height)/2;
		
		font.drawMultiLine(batch, str, x, y, b.width, HAlignment.CENTER);
	}
	
	public static void drawCenteredScreenX(SpriteBatch batch, BitmapFont font, CharSequence str, float y, 
			int viewportWidth) {

		float x;
		
		TextBounds b = font.getMultiLineBounds(str);
		
		x = (viewportWidth - b.width)/2;
		
		font.drawMultiLine(batch, str, x, y, b.width, HAlignment.CENTER);
	}
	
	public static float getCenterX(BitmapFont font, CharSequence str, 
			int viewportWidth) {
		float x;
		
		TextBounds b = font.getMultiLineBounds(str);
		
		x = (viewportWidth - b.width)/2;
		
		return x;
	}
	
	public static float getCenterX(BitmapFont font, CharSequence str, float maxLength, 
			int viewportWidth) {
		float x;
		
		TextBounds b = font.getWrappedBounds(str, maxLength);
		
		x = (viewportWidth - b.width)/2;
		
		return x;
	}
	
	public static float getCenterY(BitmapFont font, CharSequence str, int viewportHeight) {
		
		float y;
		
		TextBounds b = font.getMultiLineBounds(str);
		
		y = (viewportHeight + b.height)/2;
		
		return y;
	}
	
	public static float getCenterY(BitmapFont font, CharSequence str, float maxLength, int viewportHeight) {
		float y;
		
		TextBounds b = font.getWrappedBounds(str, maxLength);
		
		y = (viewportHeight + b.height)/2;
		
		return y;
	}

	public static Vector2 getCenter(BitmapFont font, CharSequence str, 
			int viewportWidth, int viewportHeight) {
		Vector2 p = new Vector2();
		
		TextBounds b = font.getMultiLineBounds(str);
		
		p.x = (viewportWidth - b.width)/2;
		p.y = (viewportHeight + b.height)/2;
		
		return p;
	}

	public static float getSubtitleY(BitmapFont font, String str,  int viewportHeight) {
		TextBounds b = font.getMultiLineBounds(str);
		
		float y = viewportHeight - b.height - viewportHeight/10;
		
		return y;
	}	
	
	public static float getSubtitleY(BitmapFont font, String str, float maxLength, int viewportHeight) {
		
		float y = viewportHeight - viewportHeight/10;
		
		return y;
	}		
	
}
