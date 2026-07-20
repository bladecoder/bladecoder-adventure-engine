package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;

@ActionDescription("Execute actions inside the If/EndIf if the scene attribute has the specified value.")
public class IfSceneAttrAction extends AbstractIfAction {

	public enum SceneAttr {
		STATE, CURRENT_SCENE, PLAYER, IN_CUTMODE
	}

	@ActionPropertyDescription("The scene to check its attribute")
	@ActionProperty(type = Type.SCENE)
	private String scene;

	@ActionProperty(required = true, defaultValue = "state")
	@ActionPropertyDescription("The scene attribute")
	private SceneAttr attr;

	@ActionProperty
	@ActionPropertyDescription("The attribute value")
	private String value;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = (scene != null && !scene.isEmpty()) ? w.getScene(scene) : w.getCurrentScene();

		if (attr == SceneAttr.STATE) {
			if (!ActionUtils.compareNullStr(value, s.getState())) {
				gotoElse(cb);
			}
		} else if (attr == SceneAttr.CURRENT_SCENE) {
			String scn = w.getCurrentScene().getId();

			if (((value != null && !value.isEmpty()) && !ActionUtils.compareNullStr(value, scn))
					|| (scene != null && !scene.isEmpty() && !ActionUtils.compareNullStr(s.getId(), scn))) {
				gotoElse(cb);
			}
		} else if (attr == SceneAttr.PLAYER) {
			CharacterActor player = s.getPlayer();

			String id = player != null ? player.getId() : null;

			if (!ActionUtils.compareNullStr(value, id)) {
				gotoElse(cb);
			}
		} else if (attr == SceneAttr.IN_CUTMODE) {
			boolean val = Boolean.parseBoolean(value);

			if (val != w.inCutMode()) {
				gotoElse(cb);
			}
		}

		return false;
	}
}
