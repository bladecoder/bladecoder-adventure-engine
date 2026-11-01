package com.bladecoder.engine.model;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Manages scene sounds.
 *
 * @author rgarcia
 */
public class SceneSoundManager implements Serializable, AssetConsumer {

  private HashMap<String, LoadedSound> loadedSounds = new HashMap<>(0);
  private final World w;
  private boolean enabled = true;

  // the global configurable by user volume
  public static float VOLUME_MULTIPLIER = 1f;

  public SceneSoundManager(World w) {
    this.w = w;
  }

  public void addSoundToLoad(SoundDesc s) {
    loadedSounds.put(s.getId(), new LoadedSound(s));
  }

  public void playSound(String id) {
    if (!enabled) return;

    LoadedSound s = loadedSounds.get(id);

    if (s == null) {
      // Not loaded, load and add to the loaded list.
      SoundDesc sd = w.getSounds().get(id);

      if (sd != null) {
        addSoundToLoad(sd);
        s = loadedSounds.get(id);

        EngineLogger.debug("LOADING SOUND: " + s.desc.getId() + " - " + s.desc.getFilename());

        try {
          EngineAssetManager.getInstance().loadSound(s.desc.getFilename());
        } catch (FileNotFoundException e) {
          EngineLogger.error("Sound file not found:" + e.getMessage());
          return;
        }

        EngineAssetManager.getInstance().finishLoading();
        s.sound = EngineAssetManager.getInstance().getSound(s.desc.getFilename());
      }
    }

    if (s != null && s.sound != null) {
      EngineLogger.diagnostic(
          "audio.sfx.requested",
          "bus",
          "sfx",
          "id",
          id,
          "resource",
          s.desc.getFilename(),
          "loop",
          s.desc.getLoop(),
          "volume",
          s.desc.getVolume(),
          "bus_multiplier",
          VOLUME_MULTIPLIER,
          "effective_volume",
          s.desc.getVolume() * VOLUME_MULTIPLIER,
          "pitch",
          s.desc.getPitch(),
          "pan",
          s.desc.getPan());
      s.lastInstance =
          s.desc.getLoop()
              ? s.sound.loop(
                  s.desc.getVolume() * VOLUME_MULTIPLIER, s.desc.getPitch(), s.desc.getPan())
              : s.sound.play(
                  s.desc.getVolume() * VOLUME_MULTIPLIER, s.desc.getPitch(), s.desc.getPan());
      s.playing = s.lastInstance >= 0;
      s.paused = false;
      EngineLogger.diagnostic(
          "audio.sfx.backend_start",
          "bus",
          "sfx",
          "id",
          id,
          "resource",
          s.desc.getFilename(),
          "instance",
          s.lastInstance,
          "accepted",
          s.playing,
          "audible_output_verified",
          false);

    } else {
      EngineLogger.error("Sound Not Found: " + id);
    }
  }

  /** Prevents scene sounds from playing, stopping any sounds already active. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) stop();
  }

  /**
   * Deletes the current sound played by the actor.
   *
   * <p>Note that actor sounds are like: actor_id
   *
   * @param actor
   */
  public void stopCurrentSound(String actor) {
    for (LoadedSound s : loadedSounds.values()) {
      String start = actor + "_";

      if (s.desc.getId().startsWith(start) && s.playing) stopSound(s.desc.getId());
    }
  }

  public void stopSound(String id) {
    LoadedSound s = loadedSounds.get(id);

    if (s != null && s.sound != null) {
      EngineLogger.diagnostic("audio.sfx.stopped", "id", s.desc.getId());
      s.sound.stop();
      s.playing = false;
      s.paused = false;
    } else {
      EngineLogger.debug("Sound Not Found: " + id);
    }
  }

  public void stop() {
    for (LoadedSound s : loadedSounds.values()) {
      if (s.playing && s.sound != null) stopSound(s.desc.getId());
    }
  }

  public void resume() {
    for (LoadedSound s : loadedSounds.values()) {
      if (s.playing && s.sound != null) {
        s.sound.resume();
        s.paused = false;
        EngineLogger.diagnostic("audio.sfx.resumed", "bus", "sfx", "id", s.desc.getId());
      }
    }
  }

  public void pause() {
    for (LoadedSound s : loadedSounds.values()) {
      if (s.playing && s.sound != null) {
        s.sound.pause();
        s.paused = true;
        EngineLogger.diagnostic("audio.sfx.paused", "bus", "sfx", "id", s.desc.getId());
      }
    }
  }

  @Override
  public void dispose() {
    for (LoadedSound s : loadedSounds.values()) {

      if (s.sound != null) stopSound(s.desc.getId());

      EngineAssetManager.getInstance().disposeSound(s.desc.getFilename());
    }
  }

  @Override
  public void loadAssets() {
    Iterator<LoadedSound> iterator = loadedSounds.values().iterator();
    while (iterator.hasNext()) {
      LoadedSound s = iterator.next();
      try {
        EngineAssetManager.getInstance().loadSound(s.desc.getFilename());
      } catch (FileNotFoundException e) {
        iterator.remove();
        EngineLogger.error("Sound file not found:" + e.getMessage());
      }
    }
  }

  @Override
  public void retrieveAssets() {
    for (LoadedSound s : loadedSounds.values()) {
      s.sound = EngineAssetManager.getInstance().getSound(s.desc.getFilename());

      // restore playing looping
      if (s.playing) {
        s.playing = false;

        if (s.desc.getLoop()) playSound(s.desc.getId());
      }
    }
  }

  /** Sound has no isPlaying API. A successful start cannot establish natural completion. */
  public Map<String, Object> getDebugState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("bus", "sfx");
    state.put("bus_multiplier", VOLUME_MULTIPLIER);
    state.put("completion_observable", false);
    state.put("audible_output_verified", false);
    List<Object> sounds = new ArrayList<>();
    for (LoadedSound s : loadedSounds.values()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", s.desc.getId());
      item.put("resource", s.desc.getFilename());
      item.put("loaded", s.sound != null);
      item.put("backend", s.sound == null ? null : s.sound.getClass().getName());
      item.put("instance", s.lastInstance);
      item.put("start_accepted", s.lastInstance >= 0);
      item.put("restore_loop_requested", s.playing && s.desc.getLoop());
      item.put("paused", s.paused);
      item.put("loop", s.desc.getLoop());
      item.put("volume", s.desc.getVolume());
      item.put("effective_volume", s.desc.getVolume() * VOLUME_MULTIPLIER);
      item.put("pitch", s.desc.getPitch());
      item.put("pan", s.desc.getPan());
      sounds.add(item);
    }
    state.put("sounds", sounds);
    return state;
  }

  @Override
  public void write(Json json) {
    json.writeValue("loadedSounds", loadedSounds, loadedSounds.getClass(), LoadedSound.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void read(Json json, JsonValue jsonData) {
    loadedSounds = json.readValue("loadedSounds", HashMap.class, LoadedSound.class, jsonData);

    if (loadedSounds == null) loadedSounds = new HashMap<>(0);

    // Retrieve desc from World sound description.
    for (Entry<String, LoadedSound> e : loadedSounds.entrySet()) {
      e.getValue().desc = w.getSounds().get(e.getKey());
    }
  }

  public static class LoadedSound {
    transient SoundDesc desc;
    transient Sound sound;
    transient long lastInstance = -1;
    transient boolean paused = false;

    // flag to restore the sound when looping.
    boolean playing = false;

    public LoadedSound(SoundDesc s) {
      desc = s;
    }

    public LoadedSound() {}
  }
}
