package com.bladecoder.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class AnimButton extends Button {
	public AnimButton(Skin skin, String styleName) {
		super(skin, styleName);
	}

	@Override
	public void act(float delta) {
		ButtonStyle style = getStyle();
		if (style.up != null && style.up instanceof AnimationDrawable)
			((AnimationDrawable) style.up).act(delta);

		if (style.over != null && style.over instanceof AnimationDrawable)
			((AnimationDrawable) style.over).act(delta);

		if (style.down != null && style.down instanceof AnimationDrawable)
			((AnimationDrawable) style.down).act(delta);

		super.act(delta);
	}
}
