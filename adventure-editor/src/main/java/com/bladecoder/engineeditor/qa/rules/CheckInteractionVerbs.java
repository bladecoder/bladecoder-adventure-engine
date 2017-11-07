package com.bladecoder.engineeditor.qa.rules;

import java.util.HashMap;

import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.qa.ActorVisitor;

public class CheckInteractionVerbs implements ActorVisitor {

	private InteractiveActor ia;
	private boolean hasLookat;
	private boolean hasPickup;
	private boolean hasTalkto;
	private boolean hasLeave;
	private boolean hasEnterExit;
	private boolean hasUse;

	@Override
	public void visit(BaseActor a) {
		if (a instanceof InteractiveActor) {
			ia = (InteractiveActor) a;
			hasLookat = false;
			hasPickup = false;
			hasTalkto = false;
			hasLeave = false;
			hasEnterExit = false;
			hasUse = false;

			if (ia.getInteraction()) {
				
				HashMap<String, Verb> verbs = ia.getVerbManager().getVerbs();
				
				for(Verb v:verbs.values())
					checkVerb(v);
				
				// TODO: check states and verbs for states.

				if (hasLeave || hasEnterExit)
					return;
				
				// discard inventory actors
				if(hasLookat) {
					if(ia instanceof SpriteActor) {
						SpriteActor sa = (SpriteActor)ia;
						if(sa.getRenderer() instanceof AtlasRenderer) {
							AtlasRenderer r = (AtlasRenderer)sa.getRenderer();
							HashMap<String, AnimationDesc> animations = r.getAnimations();
							
							if(animations.size() == 0) {
								EditorLogger.error("CheckInteractionVerbs: Actor with no animations! - "  + sa.getScene().getId() + "." + sa.getId());
							}
							
							if(!hasUse)
								EditorLogger.msg("CheckInteractionVerbs: Inventory item should has default 'use' - "  + sa.getScene().getId() + "." + sa.getId());
							
							if(animations.get(r.getInitAnimation()).source.contains("inventory"))
								return;
						}
					}
				}

				// check for lookat and pickup/talk verbs
				if (!hasLookat || (!hasPickup && !hasTalkto)) {
					EditorLogger.msg("CheckInteractionVerbs: " + a.getScene().getId() + "." + a.getId());
				}
			}
		}
	}

	public void checkVerb(Verb v) {
		if (v.getId().equals("lookat"))
			hasLookat = true;
		else if (v.getId().equals("pickup"))
			hasPickup = true;
		else if (v.getId().equals("talkto"))
			hasTalkto = true;
		else if (v.getId().equals("leave"))
			hasLeave = true;
		else if (v.getId().equals("use"))
			hasUse = true;
		else if (v.getId().equals("enter") || v.getId().equals("exit"))
			hasEnterExit = true;
	}

}
