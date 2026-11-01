package com.bladecoder.engine.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Json.Serializable;
import com.bladecoder.engine.assets.AssetConsumer;
import java.util.Collections;
import java.util.Map;

public interface ActorRenderer extends Serializable, AssetConsumer {
  /** Enables or disables renderer side effects while preparing a static preview. */
  default void setPreviewEventsEnabled(boolean enabled) {}

  /** Optional read-only diagnostics, queried on the game thread by the debug controller. */
  default Map<String, Object> getDebugState() {
    return Collections.emptyMap();
  }

  void update(float delta);

  void draw(
      SpriteBatch batch, float x, float y, float scaleX, float scaleY, float rotation, Color tint);

  float getWidth();

  float getHeight();

  int getOrgAlign();

  void setOrgAlign(int align);

  /**
   * Compute the bbox based in the size of the animation/sprite. T
   *
   * @param bbox The polygon to update. It will be updated when an animation starts/finishes.
   */
  void updateBboxFromRenderer(Polygon bbox);

  void setWorld(World world);
}
