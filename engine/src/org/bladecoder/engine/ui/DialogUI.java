package org.bladecoder.engine.ui;

import java.util.ArrayList;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.assets.UIAssetConsumer;
import org.bladecoder.engine.i18n.I18N;
import org.bladecoder.engine.model.Dialog;
import org.bladecoder.engine.model.DialogOption;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

public class DialogUI implements TouchEventListener, UIAssetConsumer  {
	
	private static final String FONT_FILE = "fonts/ArchitectsDaughter_fix.ttf";
	private final static Color BG_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.7f);
	public static final String DIALOG_END_COMMAND = "dialog_end";
	
	private static final int FONT_SIZE = 20;
	private static final int FONT_SIZE_LOWRES = 13;
	
	private BitmapFont font = null;
	
	CommandListener l;
	
	int viewPortWidth, viewPortHeight;
	
	Recorder recorder;

	public DialogUI(Recorder recorder) {
		this.recorder = recorder;
	}
	
	public void setCommandListener(CommandListener l) {
		this.l = l;
	}

	public void draw(SpriteBatch batch, int inputX, int inputY) {
		ArrayList<DialogOption> options = World.getInstance().getCurrentDialog().getVisibleOptions();
		
		if(options.size() == 0) return;
		else if(options.size() == 1) { // If only has one option, autoselect it
			select(0);
			return;
		}

		float lineHeight = font.getLineHeight();
		float y = lineHeight * options.size();
		
		int selected = getOption(inputX, inputY);
		
		RectangleRenderer.draw(batch, 0, 0, viewPortWidth, 15 + lineHeight * options.size(), BG_COLOR);
		
		for(int i = 0; i < options.size(); i++) {
			DialogOption o = options.get(i);
			String str = o.getText();
			
			if(str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));
						
			if(i == selected) {
				font.setColor(Color.LIGHT_GRAY);
				font.draw(batch, str, 10, y);				
			} else {
				font.setColor(Color.WHITE);
				font.draw(batch, str, 10, y);
			}
			
			y-= lineHeight;
		}
	}
	
	@Override
	public void createAssets() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(EngineAssetManager.getInstance().getAsset(FONT_FILE));
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
	}

	public void resize(Rectangle v) {
		viewPortWidth = (int)v.width;
		viewPortHeight = (int)v.height;
	}
	
	@Override
	public void dispose() {
		font.dispose();
	}

	private int getOption(float x, float y) {
		float lineHeight = font.getLineHeight();
		
		int selectedLine = (int)(y / lineHeight);
		
		return World.getInstance().getCurrentDialog().getNumVisibleOptions() - selectedLine - 1;
	}
	
	private void select(int i) {
		Dialog d = World.getInstance().getCurrentDialog();
		
		// RECORD
		if(recorder.isRecording()) {			
			recorder.add(i);
		}
		
		d.selectOption(i);
		
		if(World.getInstance().getCurrentDialog().ended()) {
			l.runCommand(DIALOG_END_COMMAND, null);
		}
	}
	
	@Override
	public void touchEvent(int type, float x0, float y0, int pointer, int button) {
		switch (type) {
		case TOUCH_UP:
			int i = getOption(x0, y0);
			
			if(i >= 0) {
				select(i);
			}
			
			break;
		}
	}
}
