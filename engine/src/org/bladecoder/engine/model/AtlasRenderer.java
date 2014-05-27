package org.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.FATween;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class AtlasRenderer implements SpriteRenderer {

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();
	
	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;

	private AtlasFrameAnimation currentFrameAnimation;

	private TextureRegion tex;
	private boolean flipX;
	private FATween faTween;
	
	private int currentFrameIndex;
	
	private HashMap<String, AtlasCacheEntry> atlasCache = new HashMap<String, AtlasCacheEntry>();

	class AtlasCacheEntry {
		int refCounter;
	}
	

	@Override
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}
	
	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}
	
	@Override
	public String[] getInternalAnimations(String source) {
		retrieveSource(source);
		
		TextureAtlas atlas = EngineAssetManager.getInstance().getTextureAtlas(source);
		
		Array<AtlasRegion> animations = atlas.getRegions();
		ArrayList<String> l = new ArrayList<String>();
		
		for(int i = 0; i< animations.size; i++) {
			AtlasRegion a = animations.get(i);
			if(!l.contains(a.name))
				l.add(a.name);
		}
		
		
		return l.toArray(new String[l.size()]);
	}	


	@Override
	public void update(float delta) {
		if(faTween != null) {
			faTween.update(this, delta);
			if(faTween.isComplete()) {
				faTween = null;
			}
		}
	}
	
	public void setFrame(int i) {
		currentFrameIndex = i;
		tex =  currentFrameAnimation.regions.get(i);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {
		
		x = x - getWidth() / 2 * scale; // SET THE X ORIGIN TO THE CENTER OF THE SPRITE

		if (tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight()
					* scale, Color.RED);
			return;
		}

		if (!flipX) {
			batch.draw(tex, x, y, 0, 0, tex.getRegionWidth(),
					tex.getRegionHeight(), scale, scale, 0);
		} else {
			batch.draw(tex, x + tex.getRegionWidth() * scale, y, 0,
					0, -tex.getRegionWidth(), tex.getRegionHeight(),
					scale, scale, 0);
		}
	}

	@Override
	public float getWidth() {
		if (tex == null)
			return 200;

		return tex.getRegionWidth();
	}

	@Override
	public float getHeight() {
		if (tex == null)
			return 200;

		return tex.getRegionHeight();
	}

	@Override
	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	@Override
	public HashMap<String, FrameAnimation> getFrameAnimations() {
		return (HashMap<String, FrameAnimation>)fanims;
	}

	@Override
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		
		if(id == null)
			id = initFrameAnimation;
		
		AtlasFrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}
		
		if(currentFrameAnimation != null && currentFrameAnimation.disposeWhenPlayed) {
			disposeSource(currentFrameAnimation.source);
			currentFrameAnimation.regions = null;
		}

		currentFrameAnimation = fa;

		// If the source is not loaded. Load it.
		if (currentFrameAnimation != null
				&& currentFrameAnimation.regions == null) {

			retrieveFA(fa);

			if (currentFrameAnimation.regions == null || currentFrameAnimation.regions.size == 0) {
				EngineLogger.error(currentFrameAnimation.id + " has no regions in ATLAS " + currentFrameAnimation.source);
				fanims.remove(currentFrameAnimation.id);
			}
		}

		if (currentFrameAnimation == null) {

			tex = null;

			return;
		}

		if (currentFrameAnimation.regions.size == 1
				|| currentFrameAnimation.duration == 0.0) {

			setFrame(0);

			if (cb != null) {
				ActionCallbackQueue.add(cb);
			}

			return;
		}

		if (repeatType == Tween.FROM_FA) {
			repeatType = currentFrameAnimation.animationType;
			count = currentFrameAnimation.count;
		}
		
		faTween = new FATween();
		faTween.start(this, repeatType, count, currentFrameAnimation.duration, cb);
	}

	public int getNumFrames() {
		return currentFrameAnimation.regions.size;
	}

	@Override
	public String getCurrentFrameAnimationId() {
		if (currentFrameAnimation == null)
			return null;

		String id = currentFrameAnimation.id;

		if (flipX) {
			id = FrameAnimation.getFlipId(id);
		}

		return id;

	}

	@Override
	public void addFrameAnimation(FrameAnimation fa) {
		if(initFrameAnimation == null)
			initFrameAnimation = fa.id; 
			
		fanims.put(fa.id, (AtlasFrameAnimation)fa);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n  Current Anim: ").append(currentFrameAnimation.id);

		sb.append("\n");

		return sb.toString();
	}

	private AtlasFrameAnimation getFrameAnimation(String id) {	
		AtlasFrameAnimation fa = (AtlasFrameAnimation)fanims.get(id);
		flipX = false;

		if (fa == null && id != null) {
			
			// Search for flipped
			String flipId = FrameAnimation.getFlipId(id);

			fa = (AtlasFrameAnimation)fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(FrameAnimation.LEFT)) {
					sb.append(id.substring(0, id.length() - 4));
					sb.append("frontleft");
				} else if (id.endsWith(FrameAnimation.FRONTLEFT)) {
					sb.append(id.substring(0, id.length() - 9));
					sb.append("left");
				} else if (id.endsWith(FrameAnimation.RIGHT)) {
					sb.append(id.substring(0, id.length() - 5));
					sb.append("frontright");
				} else if (id.endsWith(FrameAnimation.FRONTRIGHT)) {
					sb.append(id.substring(0, id.length() - 10));
					sb.append("right");
				}

				String s = sb.toString();

				fa = (AtlasFrameAnimation)fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = FrameAnimation.getFlipId(s);

					fa = (AtlasFrameAnimation)fanims.get(flipId);

					if (fa != null)
						flipX = true;
				}
			}			
		}

		return fa;
	}

	@Override
	public void lookat(Vector2 p0, Vector2 pf) {
		lookat(FrameAnimation.getFrameDirection(p0, pf));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		startFrameAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = FrameAnimation.STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, Tween.FROM_FA, 1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = FrameAnimation.getFrameDirection(p0, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}


	private void loadSource(String source) {
		AtlasCacheEntry entry = atlasCache.get(source);
		
		if(entry == null) {
			entry = new AtlasCacheEntry();
			atlasCache.put(source, entry);
		}

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(source);

		entry.refCounter++;
	}
	
	private void retrieveFA(AtlasFrameAnimation fa) {
		retrieveSource(fa.source);
		fa.regions = EngineAssetManager.getInstance().getRegions(fa.source, fa.id);
	}

	private void retrieveSource(String source) {
		AtlasCacheEntry entry = atlasCache.get(source);
		
		if(entry==null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().getManager().finishLoading();
		}
	}
	
	private void disposeSource(String source) {
		AtlasCacheEntry entry = atlasCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeAtlas(source);
		}

		entry.refCounter--;
	}	
	

	@Override
	public void loadAssets() {
		for (FrameAnimation fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentFrameAnimation != null && !currentFrameAnimation.preload) {
			loadSource(currentFrameAnimation.source);
		} else if (currentFrameAnimation == null && initFrameAnimation != null) {
			FrameAnimation fa = fanims.get(initFrameAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		for (FrameAnimation fa : fanims.values()) {
			if(fa.preload)
				retrieveFA((AtlasFrameAnimation)fa);
		}
		
		if(currentFrameAnimation != null && !currentFrameAnimation.preload) {
			retrieveFA(currentFrameAnimation);
		} else if(currentFrameAnimation == null && initFrameAnimation != null) {
			AtlasFrameAnimation fa = (AtlasFrameAnimation)fanims.get(initFrameAnimation);
			
			if(!fa.preload)
				retrieveFA(fa);		
		}

		if (currentFrameAnimation != null) {		
			setFrame(currentFrameIndex);
		} else if(initFrameAnimation != null){
			startFrameAnimation(initFrameAnimation, Tween.FROM_FA, 1, null);
		}
	}

	@Override
	public void dispose() {
		for (String key : atlasCache.keySet()) {
			EngineAssetManager.getInstance().disposeAtlas(key);
		}
		
		atlasCache.clear();
	}

	@Override
	public void write(Json json) {

		json.writeValue("fanims", fanims);

		String currentFrameAnimationId = null;

		if (currentFrameAnimation != null)
			currentFrameAnimationId = currentFrameAnimation.id;

		json.writeValue("currentFrameAnimation", currentFrameAnimationId,
				currentFrameAnimationId == null ? null : String.class);
		
		json.writeValue("initFrameAnimation", initFrameAnimation);

		json.writeValue("flipX", flipX);
		json.writeValue("currentFrameIndex", currentFrameIndex);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class,
				AtlasFrameAnimation.class, jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = (AtlasFrameAnimation)fanims.get(currentFrameAnimationId);
		
		initFrameAnimation = json.readValue("initFrameAnimation", String.class,
				jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
		currentFrameIndex = json.readValue("currentFrameIndex", Integer.class, jsonData);
	}
}