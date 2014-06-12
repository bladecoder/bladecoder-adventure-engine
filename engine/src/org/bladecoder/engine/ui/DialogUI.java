package org.bladecoder.engine.ui;

import java.util.ArrayList;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.i18n.I18N;
import org.bladecoder.engine.model.Dialog;
import org.bladecoder.engine.model.DialogOption;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class DialogUI  {
	
	private static final String FONT_STYLE = "DIALOG_FONT";
	private final static Color BG_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.7f);
	public static final String DIALOG_END_COMMAND = "dialog_end";
	
	private BitmapFont font = null;
	
	SceneScreen sceneScreen;
	
	int viewPortWidth, viewPortHeight;
	
	Recorder recorder;

	public DialogUI(SceneScreen sceneScreen, Recorder recorder) {
		this.recorder = recorder;
		this.sceneScreen = sceneScreen;
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
	
	public void createAssets() {
		if(font != null)
			font.dispose();
			
		font = EngineAssetManager.getInstance().loadFont(FONT_STYLE);		
	}


	public void resize(int width, int height) {
		viewPortWidth = width;
		viewPortHeight = height;
	}
	
	public void dispose() {
		font.dispose();
		font = null;
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
			sceneScreen.runCommand(DIALOG_END_COMMAND, null);
		}
	}
	
	public void touchEvent(int type, float x0, float y0, int pointer, int button) {
		switch (type) {
		case TouchEventListener.TOUCH_UP:
			int i = getOption(x0, y0);
			
			if(i >= 0) {
				select(i);
			}
			
			break;
		}
	}
}
