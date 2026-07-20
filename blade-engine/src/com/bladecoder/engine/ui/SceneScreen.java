package com.bladecoder.engine.ui;

import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.World;

public interface SceneScreen extends BladeScreen {
	enum ActionButton {
		LOOKAT, ACTION, INVENTORY, NONE
	}

	UI getUI();

	World getWorld();

	Viewport getViewport();

	InteractiveActor getCurrentActor();

	void actorClick(InteractiveActor actor, ActionButton button);

	void runVerb(InteractiveActor a, String verb, String target);

	float getSpeed();

	void setSpeed(float speed);
}
