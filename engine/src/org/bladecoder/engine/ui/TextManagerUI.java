package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.assets.UIAssetConsumer;
import org.bladecoder.engine.model.Text;
import org.bladecoder.engine.model.TextManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engine.util.TextUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * TextManagerUI draws texts and dialogs on screen.
 * 
 * For now, only one subtitle is displayed in the screen.
 * 
 * @author rgarcia
 * 
 */
public class TextManagerUI implements UIAssetConsumer {
	private static final float RECT_MARGIN = 18f; // TODO: MARGIN DEPENDS ON RESOLUTION/DPI!
	private static final float RECT_BORDER = 2f;
	private static final int FONT_SIZE = 18;
	private static final int FONT_SIZE_LOWRES = 11;

	private static final String FONT_FILE = "fonts/ArchitectsDaughter_fix.ttf";

	private BitmapFont font = null;
	private float maxRectangleWidth;
	private float maxTalkWidth;

	private AtlasRegion bubblePointer;
	private float scale = 1f;
	
	Rectangle viewPort;

	public TextManagerUI() {
	}

	public void draw(SpriteBatch batch) {
		Text currentSubtitle = World.getInstance().getTextManager().getCurrentSubtitle();

		if (currentSubtitle != null) {
			float posx = currentSubtitle.x;
			float posy = currentSubtitle.y;
			
			Vector3 p = World.getInstance().getSceneCamera().scene2screen(posx, posy, viewPort);

			if (posx == TextManager.POS_CENTER || posx == TextManager.POS_SUBTITLE)
				posx = TextUtils.getCenterX(font, currentSubtitle.str, maxRectangleWidth, (int)viewPort.width);
			else
				posx = p.x;

			if (posy == TextManager.POS_CENTER)
				posy = TextUtils.getCenterY(font, currentSubtitle.str, maxRectangleWidth, (int)viewPort.height);
			else if (posy == TextManager.POS_SUBTITLE)
				posy = TextUtils.getSubtitleY(font, currentSubtitle.str, maxRectangleWidth, (int)viewPort.height);
			else
				posy = p.y;

			font.setColor(currentSubtitle.color);

			if (currentSubtitle.type == Text.Type.RECTANGLE) {

				TextBounds b = font.getWrappedBounds(currentSubtitle.str, maxRectangleWidth);

				RectangleRenderer.draw(batch, posx - RECT_MARGIN - RECT_BORDER,
						posy - b.height - RECT_MARGIN - RECT_BORDER, b.width
								+ (RECT_MARGIN + RECT_BORDER) * 2, b.height
								+ (RECT_MARGIN + RECT_BORDER) * 2, Color.BLACK);

				RectangleRenderer.draw(batch, posx - RECT_MARGIN, posy
						- b.height - RECT_MARGIN, b.width + RECT_MARGIN * 2, b.height + RECT_MARGIN
						* 2, Color.WHITE);

				font.drawWrapped(batch, currentSubtitle.str, posx, posy,
						b.width, HAlignment.CENTER);
			} else if (currentSubtitle.type == Text.Type.TALK) {
				TextBounds b = font.getWrappedBounds(currentSubtitle.str, maxTalkWidth);
				
				posx = posx - b.width / 2;
				posy += b.height + bubblePointer.getRegionHeight() * scale + RECT_MARGIN;

				float x = posx - RECT_MARGIN;
				float y = posy - b.height - RECT_MARGIN;
				float width = b.width + RECT_MARGIN * 2;
				float height = b.height + RECT_MARGIN * 2;
				
				float  dx = 0,dy = 0;

				// check if the text exits the screen
				if (x < 0) {
					dx = -x + RECT_MARGIN;
				} else if (x + width > viewPort.width) {
					dx = -(x + width - viewPort.width + RECT_MARGIN);
				}
				
				if (y + height > viewPort.height) {
					dy = -(y + height - viewPort.height);
				}

				batch.draw(bubblePointer, x + (width - bubblePointer.getRegionWidth()) / 2, y
						- bubblePointer.getRegionHeight() + 1 + dy, bubblePointer.getRegionWidth() / 2,
						bubblePointer.getRegionHeight(), bubblePointer.getRegionWidth(),
						bubblePointer.getRegionHeight(), scale, scale, 0);
				RectangleRenderer.draw(batch, x + dx, y + dy, width, height, Color.WHITE);

				font.drawWrapped(batch, currentSubtitle.str, posx + dx,
						posy + dy, b.width, HAlignment.CENTER);

			} else {
				TextBounds b = font.getWrappedBounds(currentSubtitle.str, maxRectangleWidth);
				font.drawWrapped(batch, currentSubtitle.str, posx, posy,
						b.width, HAlignment.CENTER);
			}

		}
	}

	@Override
	public void createAssets() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(EngineAssetManager
				.getInstance().getAsset(FONT_FILE));

		// For small screens we use small fonts to limit the space used for the
		// text in the screen
		if (Gdx.graphics.getWidth() < 800)
			font = generator.generateFont(FONT_SIZE_LOWRES);
		else
			font = generator.generateFont(FONT_SIZE);

		generator.dispose();

	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {
		bubblePointer = atlas.findRegion("bubblepointer");
	}

	public void resize(Rectangle v) {
		viewPort = v;

		scale = viewPort.width / World.getInstance().getWidth();
		maxRectangleWidth = viewPort.width / 1.7f;
		maxTalkWidth = viewPort.width / 3;
	}

	@Override
	public void dispose() {
		font.dispose();
	}
}
