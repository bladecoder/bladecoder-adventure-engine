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
package com.bladecoder.engine.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.SpritePosTween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.WalkTween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.EngineLogger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class CharacterActor extends SpriteActor {
    public final static float DEFAULT_WALKING_SPEED = 1000f; // Speed units:
    // pix/sec.

    public final static String DEFAULT_STAND_ANIM = "stand";
    public final static String DEFAULT_WALK_ANIM = "walk";
    public final static String DEFAULT_TALK_ANIM = "talk";

    private float walkingSpeed = DEFAULT_WALKING_SPEED;
    private Color textColor;
    private String textStyle;
    private Vector2 talkingTextPos;

    private String standAnim = DEFAULT_STAND_ANIM;
    private String walkAnim = DEFAULT_WALK_ANIM;
    private String talkAnim = DEFAULT_TALK_ANIM;

    private HashMap<String, Dialog> dialogs;

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;

        setDirtyProp(DirtyProps.TEXT_COLOR);
    }

    public String getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(String textStyle) {
        this.textStyle = textStyle;
        setDirtyProp(DirtyProps.TEXT_STYLE);
    }

    public String getStandAnim() {
        return standAnim;
    }

    public void setStandAnim(String standAnim) {
        this.standAnim = standAnim;
    }

    public String getWalkAnim() {
        return walkAnim;
    }

    public void setWalkAnim(String walkAnim) {
        this.walkAnim = walkAnim;
    }

    public String getTalkAnim() {
        return talkAnim;
    }

    public void setTalkAnim(String talkAnim) {
        this.talkAnim = talkAnim;
    }

    public Dialog getDialog(String dialog) {
        return dialogs.get(dialog);
    }

    public void addDialog(Dialog d) {
        if (dialogs == null)
            dialogs = new HashMap<>();

        dialogs.put(d.getId(), d);
    }

    public void setWalkingSpeed(float s) {
        walkingSpeed = s;

        setDirtyProp(DirtyProps.WALKING_SPEED);
    }

    public float getWalkingSpeed() {
        return walkingSpeed;
    }

    public Vector2 getTalkingTextPos() {
        return talkingTextPos;
    }

    public void setTalkingTextPos(Vector2 talkingTextPos) {
        this.talkingTextPos = talkingTextPos;
        setDirtyProp(DirtyProps.TALKING_TEXT_POS);
    }

    public void lookat(Vector2 p) {
        if (!(renderer instanceof AnimationRenderer))
            return;

        inAnim();
        removeTween(SpritePosTween.class);
        ((AnimationRenderer) renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null,
                new Vector2(getBBox().getX(), getBBox().getY()), p);
        outAnim(Tween.Type.SPRITE_DEFINED);
    }

    public void lookat(String direction) {
        if (!(renderer instanceof AnimationRenderer))
            return;

        inAnim();
        removeTween(SpritePosTween.class);
        ((AnimationRenderer) renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null, direction);
        outAnim(Tween.Type.SPRITE_DEFINED);
    }

    public void stand() {
        if (!(renderer instanceof AnimationRenderer))
            return;

        inAnim();
        removeTween(SpritePosTween.class);
        ((AnimationRenderer) renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null, null);
        outAnim(Tween.Type.SPRITE_DEFINED);
    }

    public void talk() {
        if (!(renderer instanceof AnimationRenderer))
            return;

        inAnim();
        removeTween(SpritePosTween.class);
        ((AnimationRenderer) renderer).startAnimation(talkAnim, Tween.Type.SPRITE_DEFINED, -1, null, null);
        outAnim(Tween.Type.SPRITE_DEFINED);
    }

    public void startWalkAnim(Vector2 p0, Vector2 pf) {
        if (!(renderer instanceof AnimationRenderer))
            return;

        inAnim();
        ((AnimationRenderer) renderer).startAnimation(walkAnim, Tween.Type.SPRITE_DEFINED, -1, null, p0, pf);
        outAnim(Tween.Type.SPRITE_DEFINED);
    }

    /**
     * Walking Support
     *
     * @param pf Final position to walk
     * @param cb The action callback
     */
    public void goTo(Vector2 pf, ActionCallback cb, boolean ignoreWalkZone) {
        EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

        Vector2 p0 = new Vector2(getBBox().getX(), getBBox().getY());

        // stop previous movement
        if (tweens.size() > 0) {
            removeTween(SpritePosTween.class);
            stand();
        }

        ArrayList<Vector2> walkingPath;

        // Doesn't move if dst is less than 2px
        if (p0.dst(pf) < 2.0f) {
            setPosition(pf.x, pf.y);

            // call the callback
            if (cb != null) {
                cb.resume();
            }

            return;
        }

        if (scene.getWalkZone() != null && !ignoreWalkZone) {
            walkingPath = scene.getPolygonalNavGraph().findPath(p0.x, p0.y, pf.x, pf.y);
        } else {
            walkingPath = new ArrayList<>(2);
            walkingPath.add(p0);
            walkingPath.add(new Vector2(pf));
        }

        if (walkingPath == null || walkingPath.size() == 0) {
            // call the callback even when the path is empty
            if (cb != null) {
                cb.resume();
            }

            return;
        }

        WalkTween t = new WalkTween();

        t.start(this, walkingPath, walkingSpeed, cb);
        addTween(t);
    }

    /**
     * If the character is walking, the character position is set to the final
     * position and the walk is finish.
     * <p>
     * This is used to fast walk between scenes. Used when double clicking.
     */
    public void fastWalk() {
        for (Tween<SpriteActor> t : tweens) {
            if (t instanceof WalkTween) {
                WalkTween wt = (WalkTween) t;
                wt.completeNow(this);
                break;
            }
        }
    }

    public HashMap<String, Dialog> getDialogs() {
        return dialogs;
    }

    @Override
    public String toString() {

        return super.toString() + "  Walking Speed: " + walkingSpeed +
                "\nText Color: " + textColor;
    }

    @Override
    public void write(Json json) {
        super.write(json);

        if (dialogs != null)
            json.writeValue("dialogs", dialogs, HashMap.class, Dialog.class);

        BladeJson bjson = (BladeJson) json;
        if (bjson.getMode() == Mode.MODEL) {
            if (textStyle != null)
                json.writeValue("textStyle", textStyle);

            if (textColor != null)
                json.writeValue("textColor", textColor);

            if (talkingTextPos != null) {
                float worldScale = EngineAssetManager.getInstance().getScale();
                json.writeValue("talkingTextPos",
                        new Vector2(talkingTextPos.x / worldScale, talkingTextPos.y / worldScale));
            }
        } else {
            if (!DEFAULT_STAND_ANIM.equals(standAnim))
                json.writeValue("standAnim", standAnim);

            if (!DEFAULT_WALK_ANIM.equals(walkAnim))
                json.writeValue("walkAnim", walkAnim);

            if (!DEFAULT_TALK_ANIM.equals(talkAnim))
                json.writeValue("talkAnim", talkAnim);

            if (isDirty(DirtyProps.TEXT_STYLE))
                json.writeValue("textStyle", textStyle);

            if (isDirty(DirtyProps.TEXT_COLOR))
                json.writeValue("textColor", textColor);

            if (isDirty(DirtyProps.TALKING_TEXT_POS)) {
                float worldScale = EngineAssetManager.getInstance().getScale();
                json.writeValue("talkingTextPos",
                        new Vector2(talkingTextPos.x / worldScale, talkingTextPos.y / worldScale));
            }
        }

        if (bjson.getMode() == Mode.MODEL || isDirty(DirtyProps.WALKING_SPEED))
            json.writeValue("walkingSpeed", walkingSpeed);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);

        BladeJson bjson = (BladeJson) json;
        if (bjson.getMode() == Mode.MODEL) {
            dialogs = json.readValue("dialogs", HashMap.class, Dialog.class, jsonData);

            if (dialogs != null) {
                for (Dialog d : dialogs.values())
                    d.setActor(this);
            }

        } else {
            if (dialogs != null) {
                JsonValue dialogsValue = jsonData.get("dialogs");

                for (Dialog d : dialogs.values()) {
                    String id = d.getId();
                    JsonValue dValue = dialogsValue.get(id);

                    if (dValue != null)
                        d.read(json, dValue);
                }
            }

            standAnim = json.readValue("standAnim", String.class, DEFAULT_STAND_ANIM, jsonData);
            walkAnim = json.readValue("walkAnim", String.class, DEFAULT_WALK_ANIM, jsonData);
            talkAnim = json.readValue("talkAnim", String.class, DEFAULT_TALK_ANIM, jsonData);
        }

        textStyle = json.readValue("textStyle", String.class, textStyle, jsonData);
        walkingSpeed = json.readValue("walkingSpeed", float.class, walkingSpeed, jsonData);
        textColor = json.readValue("textColor", Color.class, textColor, jsonData);
        talkingTextPos = json.readValue("talkingTextPos", Vector2.class, talkingTextPos, jsonData);

        if (talkingTextPos != null) {
            float worldScale = EngineAssetManager.getInstance().getScale();
            talkingTextPos.x *= worldScale;
            talkingTextPos.y *= worldScale;
        }
    }

}