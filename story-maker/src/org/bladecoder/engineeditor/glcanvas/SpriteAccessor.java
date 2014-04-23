package org.bladecoder.engineeditor.glcanvas;

import aurelienribon.tweenengine.TweenAccessor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
public class SpriteAccessor implements TweenAccessor<Sprite> {
	public static final int POS_XY = 1;
	public static final int POS_X = 10;
	public static final int POS_Y = 11;
	public static final int CPOS_XY = 2;
	public static final int SCALE_XY = 3;
	public static final int ROTATION = 4;
	public static final int OPACITY = 5;
	public static final int TINT = 6;

	@Override
	public int getValues(Sprite target, int tweenType, float[] returnValues) {
		switch (tweenType) {
			case POS_XY:
				returnValues[0] = target.getX();
				returnValues[1] = target.getY();
				return 2;

			case POS_X: returnValues[0] = target.getX(); return 1;
			case POS_Y: returnValues[0] = target.getY(); return 1;

			case CPOS_XY:
				returnValues[0] = target.getX() + target.getWidth()/2;
				returnValues[1] = target.getY() + target.getHeight()/2;
				return 2;

			case SCALE_XY:
				returnValues[0] = target.getScaleX();
				returnValues[1] = target.getScaleY();
				return 2;

			case ROTATION: returnValues[0] = target.getRotation(); return 1;
			case OPACITY: returnValues[0] = target.getColor().a; return 1;

			case TINT:
				returnValues[0] = target.getColor().r;
				returnValues[1] = target.getColor().g;
				returnValues[2] = target.getColor().b;
				return 3;

			default: assert false; return -1;
		}
	}

	@Override
	public void setValues(Sprite target, int tweenType, float[] newValues) {
		switch (tweenType) {
			case POS_XY: target.setPosition(newValues[0], newValues[1]); break;
			case POS_X: target.setPosition(newValues[0], target.getY()); break;
			case POS_Y: target.setPosition(target.getX(), newValues[0]); break;
			case CPOS_XY: target.setPosition(newValues[0] - target.getWidth()/2, newValues[1] - target.getHeight()/2); break;
			case SCALE_XY: target.setScale(newValues[0], newValues[1]); break;
			case ROTATION: target.setRotation(newValues[0]); break;

			case OPACITY:
				Color c = target.getColor();
				c.set(c.r, c.g, c.b, newValues[0]);
				target.setColor(c);
				break;

			case TINT:
				c = target.getColor();
				c.set(newValues[0], newValues[1], newValues[2], c.a);
				target.setColor(c);
				break;

			default: assert false;
		}
	}
}
