package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Cancels a running verb.
 *
 * @author rgarcia
 */
@ActionDescription("Stops the named verb if it is in execution.")
public class CancelVerbAction implements Action {
    @ActionPropertyDescription("The target actor. Empty for the current actor.")
    @ActionProperty(type = Type.ACTOR)
    private String actor;

    @ActionProperty
    @ActionPropertyDescription("The verb to stop. Empty for the current verb.")
    private String verb;

    @ActionPropertyDescription("If the verb is 'use', the target actor")
    @ActionProperty(type = Type.ACTOR)
    private String target;

    private World w;

    @Override
    public void init(World w) {
        this.w = w;
    }

    @Override
    public boolean run(VerbRunner cb) {

        VerbRunner v = null;

        if (verb == null) {
            v = cb;
        }

        if (v == null && actor != null) {
            BaseActor a = w.getCurrentScene()
                    .getActor(actor, true);
            v = ((InteractiveActor) a).getVerb(verb, target);
        }

        if (v == null) {
            v = w.getCurrentScene().getVerb(verb);
        }

        if (v == null) {
            v = w.getVerbManager().getVerb(verb, null, null);
        }

        if (v != null) {
            // Cancel possible pending timer
            w.getCurrentScene().getTimers().removeTimerWithCb(v);
            v.cancel();
        } else
            EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actor);

        return false;
    }

}
