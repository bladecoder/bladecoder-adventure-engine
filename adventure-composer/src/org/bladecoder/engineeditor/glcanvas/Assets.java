package org.bladecoder.engineeditor.glcanvas;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class Assets extends AssetManager {
	private static Assets instance = new Assets();
	public static Assets inst() {return instance;}

	public void initialize() {
		String[] texturesNearest = new String[] {
			"res/images/transparent-light.png",
			"res/images/transparent-dark.png",
			"res/images/white.png",
			"res/images/ic_add.png",
			"res/images/ic_edit.png",
			"res/images/ic_delete.png",
			"res/images/ic_copy.png",
			"res/images/ic_paste.png",
			"res/images/ic_add_disabled.png",
			"res/images/ic_edit_disabled.png",
			"res/images/ic_delete_disabled.png",
			"res/images/ic_copy_disabled.png",
			"res/images/ic_paste_disabled.png",
			"res/images/ic_up.png",
			"res/images/ic_up_disabled.png",
			"res/images/ic_down.png",
			"res/images/ic_down_disabled.png",
			"res/images/ic_check.png",
			"res/images/ic_check_disabled.png",
			"res/images/ic_up.png",
			"res/images/ic_up_disabled.png",
			"res/images/ic_down.png",
			"res/images/ic_down_disabled.png",
			"res/images/ic_left.png",
			"res/images/ic_left_disabled.png",
			"res/images/ic_right.png",
			"res/images/ic_right_disabled.png",
			"res/images/ic_custom.png",
			"res/images/ic_lookat.png",
			"res/images/ic_goto.png",
			"res/images/ic_pickup.png",
			"res/images/ic_leave.png",
			"res/images/ic_init.png",
			"res/images/ic_test.png",
			"res/images/ic_talkto.png",
			"res/images/ic_player.png",
			"res/images/ic_fg_actor.png",
			"res/images/ic_sprite_actor.png",
			"res/images/ic_base_actor.png",
			"res/images/ic_character_actor.png",
			"res/images/ic_repeat.png",
			"res/images/ic_yoyo.png",
			"res/images/ic_open.png",
			"res/images/ic_closed.png",
			"res/images/ic_new.png",
			 "res/images/ic_load.png",
			 "res/images/ic_save.png",
			 "res/images/ic_exit.png",
			 "res/images/ic_play.png",
			 "res/images/ic_package.png",
			 "res/images/ic_assets.png",
			 "res/images/ic_atlases.png", 
			 "res/images/ic_save_disabled.png",
			 "res/images/ic_play_disabled.png",
			 "res/images/ic_package_disabled.png",
			 "res/images/ic_assets_disabled.png",
			 "res/images/title.png"
		};

		String[] texturesLinear = new String[] {

		};

		for (String tex : texturesNearest) load(tex, Texture.class);
		for (String tex : texturesLinear) load(tex, Texture.class);

		while (update() == false) {}

		for (String tex : texturesLinear) {
			get(tex, Texture.class).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}
	}

}
