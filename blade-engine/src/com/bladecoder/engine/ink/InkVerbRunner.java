package com.bladecoder.engine.ink;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

import java.util.ArrayList;

/**
 * This VerbRunner instead of increment the IP, it executes the current action
 * and deletes it from the action list.
 * <p>
 * It only maintains the executed action in the action list if it is of
 * ActionCallback type because ActionCallback actions can be called back and
 * could be lost after save/load.
 * <p>
 * Backward compatibility note: This class can load and execute old savegames
 * where not multiflow was implemented.
 *
 * @author rgarcia
 */
public class InkVerbRunner implements VerbRunner, Serializable {
    private final ArrayList<Action> actions = new ArrayList<>(1);
    private final java.util.IdentityHashMap<Action,java.util.Map<String,Object>> actionSources=new java.util.IdentityHashMap<Action,java.util.Map<String,Object>>();
    public void addAction(Action action,String path) {
        actions.add(action);
        java.util.Map<String,Object> source=new java.util.LinkedHashMap<String,Object>();
        source.put("resource","model/"+inkManager.getStoryName()+".ink.json");source.put("ink_path",path);source.put("flow",flow);source.put("mapping","runtime_line_boundary");
        actionSources.put(action,source);
    }
    public java.util.Map<String,Object> getActionSource(Action action){return actionSources.get(action);}
    private void removeCurrentAction(){actionSources.remove(actions.remove(ip));}
    private int ip = 0;
    private boolean cancelled = false;
    private final String flow;
    private ActionCallback cb;

    // Depending on the reading order of Inventory, InkManager and Actor verbs,
    // the verbCallbacks may not exist. So, we search the Cb lazily when needed.
    private String sCb;

    private final InkManager inkManager;
    private final World w;

    public InkVerbRunner(World w, InkManager inkManager, String flow, ActionCallback cb, String sCb) {
        this.flow = flow;
        this.cb = cb;
        this.sCb = sCb;
        this.inkManager = inkManager;
        this.w = w;
    }

    public String getFlow() {
        return flow;
    }

    public boolean isFinish() {
        return actions.isEmpty() || ip >= actions.size() || cancelled;
    }

    @Override
    public void resume() {
        if (cancelled)
            return;

        EngineLogger.diagnostic("ink.action.completed","runner_id",Integer.toHexString(System.identityHashCode(this)),"flow",flow,"ip",ip,"action",actions.get(ip).getClass().getName(),"source_ref",getActionSource(actions.get(ip)),"completion","callback");
        // We store in the array only the ActionCallback actions
        // any other can be discarded after execution
        if (actions.get(ip) instanceof ActionCallback) {
            ip++;
        } else {
            removeCurrentAction();
        }

        nextStep();
    }

    @Override
    public ArrayList<Action> getActions() {
        return actions;
    }

    @Override
    public void run(String currentTarget, ActionCallback cb) {
        ip = 0;
        nextStep();
    }

    public void runCurrentAction() {
        nextStep();
    }

    @Override
    public int getIP() {
        return ip;
    }

    @Override
    public void setIP(int ip) {
        this.ip = ip;
    }

    @Override
    public void cancel() {
        cancelled = true;
        actionSources.clear();
        ip = actions.size();
        cb = null;
        sCb = null;
    }

    @Override
    public String getCurrentTarget() {
        return null;
    }

    public void callCb() {
        if (cb != null || sCb != null) {
            if (cb == null) {
                cb = ActionCallbackSerializer.find(w, w.getCurrentScene(), sCb);
            }

            ActionCallback tmpcb = cb;
            cb = null;
            sCb = null;
            tmpcb.resume();
        }
    }

    private void nextStep() {
        if (cancelled)
            return;

        try {
            inkManager.getStory().switchFlow(flow);
        } catch (Exception e1) {
            EngineLogger.error("InkManager: " + e1.getMessage());
            return;
        }

        boolean stop = false;

        while (ip < actions.size() && !stop && !cancelled) {
            Action a = actions.get(ip);
            int executingIP=ip;java.util.Map<String,Object> source=getActionSource(a);

            try {
                EngineLogger.diagnostic("ink.action.started", "runner_id",Integer.toHexString(System.identityHashCode(this)),"flow", flow, "ip", executingIP, "action", a.getClass().getName(),"source_ref",source);
                boolean pending = a.run(this);
                EngineLogger.diagnostic(pending ? (ip==executingIP && actions.contains(a)?"ink.action.awaiting":"ink.action.returned") : "ink.action.completed", "runner_id",Integer.toHexString(System.identityHashCode(this)),"flow", flow, "ip", executingIP, "action", a.getClass().getName(),"source_ref",source);
                if (pending) stop = true;
                else {
                    // We store in the array only the ActionCallback actions
                    // any other can be discarded after execution
                    if (a instanceof ActionCallback) {
                        ip++;
                    } else {
                        removeCurrentAction();
                    }
                }
            } catch (Exception e) {
                EngineLogger.error("EXCEPTION EXECUTING ACTION: InkManager - " + ip + " - "
                        + a.getClass().getSimpleName() + " - " + e.getMessage(), e);
                ip++;
            }
        }

        if (ip >= actions.size() && !stop)
            inkManager.continueMaximally(this);

    }

    @Override
    public void write(Json json) {
        BladeJson bjson = (BladeJson) json;
        World w = bjson.getWorld();

        if (cb == null && sCb != null)
            cb = ActionCallbackSerializer.find(w, w.getCurrentScene(), sCb);

        if (cb != null)
            json.writeValue("cb", ActionCallbackSerializer.serialize(w, w.getCurrentScene(), cb));

        // SAVE ACTIONS
        json.writeArrayStart("actions");
        for (Action a : getActions()) {
            ActionUtils.writeJson(a, json);
        }
        json.writeArrayEnd();

        json.writeValue("ip", getIP());

        json.writeArrayStart("actionsSer");
        for (Action a : getActions()) {
            if (a instanceof Serializable) {
                json.writeObjectStart();
                ((Serializable) a).write(json);
                json.writeObjectEnd();
            }
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        BladeJson bjson = (BladeJson) json;
        World w = bjson.getWorld();

        sCb = json.readValue("cb", String.class, jsonData);

        // READ ACTIONS
        JsonValue actionsValue = jsonData.get("actions");

        for (int i = 0; i < actionsValue.size; i++) {
            JsonValue aValue = actionsValue.get(i);

            Action a = ActionUtils.readJson(w, json, aValue);
            getActions().add(a);
        }

        setIP(json.readValue("ip", Integer.class, jsonData));

        actionsValue = jsonData.get("actionsSer");

        int i = 0;

        for (Action a : getActions()) {
            if (a instanceof Serializable && i < actionsValue.size) {
                if (actionsValue.get(i) == null)
                    break;

                ((Serializable) a).read(json, actionsValue.get(i));
                i++;
            }
        }

    }
}
