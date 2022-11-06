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
package com.bladecoder.engine.anim;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.InterpolationMode;

import java.util.ArrayList;

/**
 * Tween for frame animation
 */
public class WalkTween extends SpritePosTween implements Serializable {

    private ArrayList<Vector2> walkingPath;
    private int currentStep = 0;
    private float speed = 0;

    private ActionCallback walkCb;

    public WalkTween() {
    }

    public void start(CharacterActor target, ArrayList<Vector2> walkingPath, float speed, ActionCallback cb) {
        this.target = target;
        this.walkingPath = walkingPath;
        this.speed = speed;
        this.currentStep = 0;

        if (cb != null) {
            walkCb = cb;
        }

        restart();
        walkToNextStep(target);
    }

    private void walkToNextStep(CharacterActor target) {
        Vector2 p0 = walkingPath.get(currentStep);
        Vector2 pf = walkingPath.get(currentStep + 1);

        float s0 = 1.0f;
        float sf = 1.0f;

        if (target.getFakeDepth()) {
            s0 = target.getScene().getFakeDepthScale(p0.y);
            sf = target.getScene().getFakeDepthScale(pf.y);
        }

        float sdiff = Math.abs(s0 - sf);
        if (sdiff > .05f) {
            // cut the path in two parts if the difference in scale is big
            Vector2 pi = new Vector2((pf.x + p0.x) / 2, (pf.y + p0.y) / 2);

            if (EngineLogger.debugMode()) {
                String debugText = String.format(
                        "WalkTween insert point: sdiff=%.2f, p0=(%.0f,%.0f), pf=(%.0f,%.0f), pi=(%.0f,%.0f)", sdiff,
                        p0.x, p0.y, pf.x, pf.y, pi.x, pi.y);

                EngineLogger.debug(debugText);
            }

            walkingPath.add(currentStep + 1, pi);

            walkToNextStep(target);
            return;
        }

        if (currentStep == 0 || ((AnimationRenderer) target.getRenderer()).changeDir(p0, pf)) {
            target.startWalkAnim(p0, pf);
        }

        // t = dst/((vf+v0)/2)
        float segmentDuration = p0.dst(pf) / (EngineAssetManager.getInstance().getScale() * speed * (s0 + sf) / 2);

        start(target, Type.NO_REPEAT, 1, pf.x, pf.y, segmentDuration, InterpolationMode.LINEAR,
                InterpolationMode.LINEAR, currentStep == walkingPath.size() - 2 ? walkCb : null);
    }

    private void segmentEnded(CharacterActor target) {

        currentStep++;

        if (currentStep < walkingPath.size() - 1) {
            walkToNextStep(target);
        } else { // WALK ENDED
            target.stand();

            // smooth animation to center camera when walking with UI/clicking (walkCb==null in that case)
            if (walkCb == null && target == target.getScene().getCameraFollowActor()) {
                target.getScene().getCamera()
                        .startAnimation(target.getX(), target.getY(), target.getScene().getCamera().getZoom(), 1f,
                                InterpolationMode.POW2OUT, null);
            }
        }
    }

    public void completeNow(CharacterActor target) {
        currentStep = walkingPath.size();

        Vector2 p = walkingPath.get(currentStep - 1);

        target.setPosition(p.x, p.y);
        target.stand();

        if (walkCb != null) {
            ActionCallback tmpcb = walkCb;
            walkCb = null;
            tmpcb.resume();
        }
    }

    @Override
    public void updateTarget() {
        super.updateTarget();

        if (isComplete())
            segmentEnded((CharacterActor) target);
    }

    @Override
    public void write(Json json) {
        super.write(json);

        json.writeValue("path", walkingPath);
        json.writeValue("currentStep", currentStep);
        json.writeValue("speed", speed);

        if (walkCb != null) {
            World w = ((BladeJson) json).getWorld();
            Scene s = ((BladeJson) json).getScene();
            json.writeValue("walkCb", ActionCallbackSerializer.serialize(w, s, walkCb));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);

        walkingPath = json.readValue("path", ArrayList.class, Vector2.class, jsonData);
        currentStep = json.readValue("currentStep", Integer.class, jsonData);
        speed = json.readValue("speed", Float.class, jsonData);

        World w = ((BladeJson) json).getWorld();
        Scene s = ((BladeJson) json).getScene();
        walkCb = ActionCallbackSerializer.find(w, s, json.readValue("walkCb", String.class, jsonData));
    }
}
