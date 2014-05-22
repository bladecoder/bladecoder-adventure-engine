package org.bladecoder.engineeditor.utils;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Message extends Label {
	static public float fadeDuration = 0.4f;

	public Message(Skin skin) {
		super("", skin);
	}
	
	public void show (Stage stage, String text) {
		clearActions();
		
		setText(text);

		pack();
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		stage.addActor(this);
		
		if (fadeDuration > 0) {
			getColor().a = 0;
			addAction(Actions.fadeIn(fadeDuration, Interpolation.fade));
		}
	}
	
	public void show (Stage stage, String text, float duration) {
		clearActions();
		
		setText(text);

		pack();
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		stage.addActor(this);
		
		if (fadeDuration > 0) {
			getColor().a = 0;
			addAction(sequence(Actions.fadeIn(fadeDuration, Interpolation.fade), 
					Actions.delay(duration, sequence(fadeOut(fadeDuration, Interpolation.fade), Actions.removeActor()))));
		}
	}

	public void hide () {
		setText("");
		
		if (fadeDuration > 0) {
			addAction(sequence(fadeOut(fadeDuration, Interpolation.fade), Actions.removeActor()));
		} else
			remove();
	}	

}
