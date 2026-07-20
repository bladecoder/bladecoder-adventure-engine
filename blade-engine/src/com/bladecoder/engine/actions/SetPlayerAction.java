package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets the scene player")
public class SetPlayerAction implements Action {

	@ActionProperty(type = Type.SCENE_CHARACTER_ACTOR)
	@ActionPropertyDescription("The scene player")
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("The inventory 'id' for the player. If empty, the inventory will not change.")
	private String inventory;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = actor == null ? w.getCurrentScene() : actor.getScene(w);
		BaseActor a = actor == null || actor.getActorId() == null ? null : s.getActor(actor.getActorId(), true);

		s.setPlayer((CharacterActor) a);

		if (inventory != null && !inventory.equals(w.getCurrentInventory())) {
			Inventory old = w.getInventory();

			w.setInventory(inventory);
			w.getInventory().loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			w.getInventory().retrieveAssets();

			old.dispose();

		}

		return false;
	}

}
