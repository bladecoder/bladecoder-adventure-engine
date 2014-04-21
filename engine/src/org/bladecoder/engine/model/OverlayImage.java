package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/** 
 * An Overlay Image is used to show an image over the scene.
 * 
 * An Overlay Image has a time to show on screen but can also be disposed by click.
 * 
 * @author rgarcia
 */
public class OverlayImage implements Disposable, Serializable {
	
	private transient Texture tex;
	private Vector2 pos;
	private float timer;
	private String filename;
	private boolean closeOnClick = true;
	
	private ActionCallback cb;
	
	public void update(float delta) {
		timer = timer - delta;
		
		if(isFinish() && cb != null) {
			ActionCallback cb2 = cb;
			cb = null;
			cb2.onEvent();
		}
	}
	
	public void draw(SpriteBatch batch) {
		if(timer >= 0)
			batch.draw(tex, pos.x, pos.y);
	}
	
	public void create(String filename, Vector2 pos, float timer, boolean closeOnClick, ActionCallback cb) {
		this.filename = filename;
		this.pos = pos;
		this.timer = timer;
		this.closeOnClick = closeOnClick;
		this.cb = cb;
		
		retrieveAssets();
	}
	
	public void click() {
		if(closeOnClick) timer = -1;
	}
	
	public void retrieveAssets() {	
		tex = EngineAssetManager.getInstance().getOverlay(filename);	
	}
	
	public boolean isFinish() {
		return (timer < 0);
	}
		
	@Override
	public void dispose() {
		timer = -1;
		tex.dispose();
	}	
	
	@Override
	public void write(Json json) {	
		json.writeValue("pos", pos);
		json.writeValue("timer", timer);
		json.writeValue("filename", filename);
		json.writeValue("closeOnClick", closeOnClick);
		json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null : String.class);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		pos = json.readValue("pos", Vector2.class, jsonData);
		timer = json.readValue("timer", Float.class, jsonData);
		filename = json.readValue("filename", String.class, jsonData);
		closeOnClick = json.readValue("closeOnClick", Boolean.class, jsonData);
		String cbSer = json.readValue("cb", String.class, jsonData);
		if(cbSer != null)
			cb = ActionCallbackSerialization.find(cbSer);
	}	
}
