package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription(name="SceneState", value="Sets the scene state")
public class SetSceneStateAction implements Action {
	@ActionPropertyDescription("The scene")
	@ActionProperty(type = Type.SCENE)
	private String scene;

	@ActionProperty
	@ActionPropertyDescription("The scene 'state'")
	private String state;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {			
		Scene s = (scene != null && !scene.isEmpty())? w.getScene(scene): w.getCurrentScene();
		
		s.setState(state);
		
		return false;
	}


}
