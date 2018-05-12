package com.bladecoder.engine.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.MusicVolumeTween;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Simple music engine.
 *
 * Plays a music file, if another music is playing, stops it before playing the
 * new music.
 *
 * @author rgarcia
 */
public class MusicManager implements Serializable, AssetConsumer {
	private MusicDesc desc = null;

	private Music music = null;

	private float currentMusicDelay = 0;
	private boolean isPlayingSer = false;
	private float musicPosSer = 0;
	transient private boolean isPaused = false;
	private MusicVolumeTween volumeTween;

	private final Task backgroundLoadingTask = new Task() {
		@Override
		public void run() {
			if (EngineAssetManager.getInstance().isLoading()) {
				cancel();
				Timer.post(backgroundLoadingTask);
			} else {
				retrieveAssets();
			}
		}
	};

	public void playMusic() {
		if (music != null && !music.isPlaying()) {

			try {
				music.play();
				music.setLooping(desc.isLoop());
				music.setVolume(desc.getVolume());
			} catch (Exception e) {

				// DEAL WITH OPENAL BUG
				if (Gdx.app.getType() == ApplicationType.Desktop && e.getMessage().contains("40963")) {
					EngineLogger.debug("!!!!!!!!!!!!!!!!!!!!!!!ERROR playing music trying again...!!!!!!!!!!!!!!!");
					setMusic(desc);

					return;
				}

				EngineLogger.error("Error Playing music: " + desc.getFilename(), e);
			}
		}
	}

	public void pauseMusic() {
		if (music != null && music.isPlaying()) {
			music.pause();
			isPaused = true;
		}
	}

	public void resumeMusic() {
		if (music != null && isPaused) {
			music.play();
			isPaused = false;
		}
	}

	public void stopMusic() {
		if (music != null)
			music.stop();
	}

	public void setMusic(MusicDesc d) {
		EngineLogger.debug(">>>SETTING MUSIC.");
		stopMusic();
		volumeTween = null;
		currentMusicDelay = 0;

		if (d != null) {
			if (desc != null) {
				dispose();
			}

			desc = new MusicDesc(d);

			// Load the music file in background to avoid
			// blocking the UI
			loadTask();
		} else {
			dispose();
			desc = null;
		}
	}

	private void loadTask() {
		loadAssets();

		backgroundLoadingTask.cancel();
		// Timer.schedule(backgroundLoadingTask, 0.2f);
		Timer.post(backgroundLoadingTask);
	}

	public void setVolume(float volume) {
		if (desc != null)
			desc.setVolume(volume);

		if (music != null && music.isPlaying())
			music.setVolume(volume);
	}

	public float getVolume() {
		if (desc != null)
			return desc.getVolume();

		return 1f;
	}

	public void leaveScene(MusicDesc newMusicDesc) {

		if (desc != null && !desc.isStopWhenLeaving()
				&& (newMusicDesc == null || newMusicDesc.getFilename().equals(desc.getFilename())))
			return;

		if (desc != null) {
			currentMusicDelay = 0f;
			stopMusic();
			dispose();
		}

		if (newMusicDesc != null) {
			desc = new MusicDesc(newMusicDesc);
		} else {
			desc = null;
		}
	}

	public void update(float delta) {
		// music delay update
		if (music != null) {
			if (!music.isPlaying()) {

				boolean initialTime = false;

				if (currentMusicDelay <= desc.getInitialDelay())
					initialTime = true;

				currentMusicDelay += delta;

				if (initialTime) {
					if (currentMusicDelay > desc.getInitialDelay())
						playMusic();
				} else {
					if (desc.getRepeatDelay() >= 0
							&& currentMusicDelay > desc.getRepeatDelay() + desc.getInitialDelay()) {
						currentMusicDelay = desc.getInitialDelay();
						playMusic();
					}
				}
			}

			if (volumeTween != null) {
				volumeTween.update(delta);
				if (volumeTween.isComplete()) {
					volumeTween = null;
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (music != null) {
			EngineLogger.debug("DISPOSING MUSIC: " + desc.getFilename());
			EngineAssetManager.getInstance().disposeMusic(desc.getFilename());
			music = null;
			desc = null;
			volumeTween = null;
		}
	}

	@Override
	public void loadAssets() {
		if (music == null && desc != null) {
			EngineLogger.debug("LOADING MUSIC: " + desc.getFilename());
			EngineAssetManager.getInstance().loadMusic(desc.getFilename());
		}
	}

	@Override
	public void retrieveAssets() {
		if (music == null && desc != null) {
			// Check if not loaded, this happens when setting a cached scene
			if (!EngineAssetManager.getInstance().isLoaded(EngineAssetManager.MUSIC_DIR + desc.getFilename())) {
				// Load the music file in background to avoid
				// blocking the UI
				loadTask();
				return;
			}

			EngineLogger.debug("RETRIEVING MUSIC: " + desc.getFilename());

			music = EngineAssetManager.getInstance().getMusic(desc.getFilename());

			if (isPlayingSer) {
				playMusic();

				if (music != null) {
					music.setPosition(musicPosSer);
					musicPosSer = 0f;
				}

				isPlayingSer = false;
			}
		}
	}

	public void fade(float volume, float duration, ActionCallback cb) {
		volumeTween = new MusicVolumeTween();
		volumeTween.start(volume, duration, InterpolationMode.FADE, cb);
	}

	@Override
	public void write(Json json) {
		json.writeValue("desc", desc);
		json.writeValue("currentMusicDelay", currentMusicDelay);
		json.writeValue("isPlaying", music != null && (music.isPlaying() || isPaused));
		json.writeValue("musicPos", music != null && (music.isPlaying() || isPaused) ? music.getPosition() : 0f);

		if (volumeTween != null)
			json.writeValue("volumeTween", volumeTween);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		desc = json.readValue("desc", MusicDesc.class, jsonData);
		currentMusicDelay = json.readValue("currentMusicDelay", float.class, jsonData);
		isPlayingSer = json.readValue("isPlaying", boolean.class, jsonData);
		musicPosSer = json.readValue("musicPos", float.class, jsonData);

		volumeTween = json.readValue("volumeTween", MusicVolumeTween.class, jsonData);
	}
}
