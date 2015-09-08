/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.ui;

import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.model.InteractiveActor;

public interface SceneScreen extends BladeScreen {
	UI getUI();

	Viewport getViewport();

	InteractiveActor getCurrentActor();

	void showMenu();

	void actorClick(InteractiveActor actor, int button);

	void runVerb(InteractiveActor a, String verb, String target);

	float getSpeed();

	void setSpeed(float speed);
}
