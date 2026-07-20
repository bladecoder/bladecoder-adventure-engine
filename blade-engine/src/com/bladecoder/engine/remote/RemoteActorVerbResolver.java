package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.List;

import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Verb;

/** Resolves the actor verbs that the default UI makes available. */
final class RemoteActorVerbResolver {
    private RemoteActorVerbResolver() {
    }

    static List<String> getAvailableVerbs(InteractiveActor actor, boolean inventoryItem) {
        List<String> verbs = new ArrayList<String>();

        if (actor.getVerb(Verb.LEAVE_VERB) != null) {
            verbs.add(Verb.LEAVE_VERB);
            return verbs;
        }

        if (inventoryItem) {
            verbs.add(Verb.USE_VERB);
            if (actor.getVerb(Verb.ACTION_VERB) != null)
                verbs.add(Verb.ACTION_VERB);
            else if (actor.getVerb(Verb.LOOKAT_VERB) != null)
                verbs.add(Verb.LOOKAT_VERB);
            else
                verbs.add(Verb.PICKUP_VERB);
            return verbs;
        }

        if (actor.getVerb(Verb.ACTION_VERB) != null) {
            verbs.add(Verb.ACTION_VERB);
            return verbs;
        }

        verbs.add(Verb.LOOKAT_VERB);
        if (actor.getVerb(Verb.TALKTO_VERB) != null)
            verbs.add(Verb.TALKTO_VERB);
        else
            verbs.add(Verb.PICKUP_VERB);
        return verbs;
    }

    static boolean canRun(InteractiveActor actor, boolean inventoryItem, String verb) {
        return getAvailableVerbs(actor, inventoryItem).contains(verb);
    }
}
