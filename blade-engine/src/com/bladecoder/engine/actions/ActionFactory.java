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
package com.bladecoder.engine.actions;

import java.util.HashMap;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

public class ActionFactory {

	private static ClassLoader loader = ActionFactory.class.getClassLoader();
	private static ObjectMap<String, Class<? extends Action>> tagToClass = new ObjectMap<>();

	static {
		addClassTag(AlphaAnimAction.class);
		addClassTag(AnimationAction.class);
		addClassTag(CameraAction.class);
		addClassTag(CancelVerbAction.class);
		addClassTag(ChooseAction.class);
		addClassTag(CommentAction.class);
		addClassTag(DisableActionAction.class);
		addClassTag(DropItemAction.class);
		addClassTag(EndAction.class);
		addClassTag(EndGameAction.class);
		addClassTag(GotoAction.class);
		addClassTag(IfAttrAction.class);
		addClassTag(IfInkVariableAction.class);
		addClassTag(IfPropertyAction.class);
		addClassTag(IfSceneAttrAction.class);
		addClassTag(InkNewStoryAction.class);
		addClassTag(InkRunAction.class);
		addClassTag(InkVariable.class);
		addClassTag(LeaveAction.class);
		addClassTag(LoadChapterAction.class);
		addClassTag(LookAtAction.class);
		addClassTag(MoveToSceneAction.class);
		addClassTag(MusicAction.class);
		addClassTag(MusicVolumeAction.class);
		addClassTag(OpenURLAction.class);
		addClassTag(PickUpAction.class);
		addClassTag(PlaySoundAction.class);
		addClassTag(PositionAction.class);
		addClassTag(PositionAnimAction.class);
		addClassTag(PropertyAction.class);
		addClassTag(RandomPositionAction.class);
		addClassTag(RemoveInventoryItemAction.class);
		addClassTag(RepeatAction.class);
		addClassTag(RotateAction.class);
		addClassTag(RunOnceAction.class);
		addClassTag(RunVerbAction.class);
		addClassTag(SayAction.class);
		addClassTag(SayDialogAction.class);
		addClassTag(ScaleAction.class);
		addClassTag(ScaleAnimActionXY.class);
		addClassTag(ScreenPositionAction.class);
		addClassTag(SetAchievementAction.class);
		addClassTag(SetActorAttrAction.class);
		addClassTag(SetCutmodeAction.class);
		addClassTag(SetDialogOptionAttrAction.class);
		addClassTag(SetPlayerAction.class);
		addClassTag(SetSceneStateAction.class);
		addClassTag(SetStateAction.class);
		addClassTag(SetDescAction.class);
		addClassTag(SetWalkzoneAction.class);
		addClassTag(ShowInventoryAction.class);
		addClassTag(SoundAction.class);
		addClassTag(TalktoAction.class);
		addClassTag(TextAction.class);
		addClassTag(TintAnimAction.class);
		addClassTag(TransitionAction.class);
		addClassTag(WaitAction.class);
	}

	private static void addClassTag(Class<? extends Action> cls) {
		tagToClass.put(ActionUtils.getName(cls), cls);
	}

	public static ObjectMap<String, Class<? extends Action>> getClassTags() {
		return tagToClass;
	}

	public static void setActionClassLoader(ClassLoader loader) {
		ActionFactory.loader = loader;
	}

	public static ClassLoader getActionClassLoader() {
		return loader;
	}

	public static Action create(String tag, HashMap<String, String> params)
			throws ClassNotFoundException, ReflectionException {

		Action a = null;

		Class<?> c = tagToClass.get(tag);

		if (c == null) {
			c = Class.forName(tag, true, loader);
		}

		a = (Action) ClassReflection.newInstance(c);

		if (params != null) {

			for (String key : params.keySet()) {
				String value = params.get(key);

				try {
					ActionUtils.setParam(a, key, value);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					EngineLogger.error("Error Setting Action Param - Action:" + tag + " Param: " + key + " Value: "
							+ value + " Msg: NOT FOUND " + e.getMessage());
				}
			}
		}

		return a;
	}
}
