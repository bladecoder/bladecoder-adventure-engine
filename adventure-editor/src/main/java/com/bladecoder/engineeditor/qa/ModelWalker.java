package com.bladecoder.engineeditor.qa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;

public class ModelWalker {

	private final ArrayList<SceneVisitor> sceneVisitors = new ArrayList<SceneVisitor>();
	private final ArrayList<EndVisitor> endVisitors = new ArrayList<EndVisitor>();
	private final ArrayList<StartVisitor> startVisitors = new ArrayList<StartVisitor>();
	private final ArrayList<ActionVisitor> actionVisitors = new ArrayList<ActionVisitor>();
	private final ArrayList<VerbVisitor> verbVisitors = new ArrayList<VerbVisitor>();
	private final ArrayList<ActorVisitor> actorVisitors = new ArrayList<ActorVisitor>();
	private final ArrayList<DialogVisitor> dialogVisitors = new ArrayList<DialogVisitor>();
	private final ArrayList<DialogOptionVisitor> optionVisitors = new ArrayList<DialogOptionVisitor>();

	public void addVisitor(Visitor v) {
		if (v instanceof SceneVisitor)
			sceneVisitors.add((SceneVisitor) v);

		if (v instanceof EndVisitor)
			endVisitors.add((EndVisitor) v);

		if (v instanceof StartVisitor)
			startVisitors.add((StartVisitor) v);

		if (v instanceof ActionVisitor)
			actionVisitors.add((ActionVisitor) v);

		if (v instanceof VerbVisitor)
			verbVisitors.add((VerbVisitor) v);

		if (v instanceof ActorVisitor)
			actorVisitors.add((ActorVisitor) v);
		
		if (v instanceof DialogVisitor)
			dialogVisitors.add((DialogVisitor) v);
		
		if (v instanceof DialogOptionVisitor)
			optionVisitors.add((DialogOptionVisitor) v);
	}

	public void walk(World w) {
		Map<String, Scene> scenes = World.getInstance().getScenes();

		for (StartVisitor sv : startVisitors)
			sv.start(w);

		for (Scene scn : scenes.values()) {
			for (SceneVisitor sv : sceneVisitors)
				sv.visit(scn);

			Map<String, BaseActor> actors = scn.getActors();

			// SCENE VERBS
			HashMap<String, Verb> verbs = scn.getVerbManager().getVerbs();

			for (Verb v : verbs.values()) {

				for (VerbVisitor vv : verbVisitors)
					vv.visit(scn, null, v);

				ArrayList<Action> actions = v.getActions();

				for (Action act : actions) {
					for (ActionVisitor av : actionVisitors)
						av.visit(scn, null, v, act);
				}
			}

			for (BaseActor a : actors.values()) {
				for (ActorVisitor av : actorVisitors)
					av.visit(a);

				if (a instanceof InteractiveActor) {
					InteractiveActor ia = (InteractiveActor) a;

					// ACTOR VERBS
					verbs = ia.getVerbManager().getVerbs();

					for (Verb v : verbs.values()) {

						for (VerbVisitor vv : verbVisitors)
							vv.visit(scn, ia, v);

						ArrayList<Action> actions = v.getActions();

						for (Action act : actions) {
							for (ActionVisitor av : actionVisitors)
								av.visit(scn, ia, v, act);
						}
					}
				}

				// DIALOGS
				if (a instanceof CharacterActor) {
					HashMap<String, Dialog> dialogs = ((CharacterActor) a).getDialogs();

					if (dialogs != null) {
						for (Dialog d : dialogs.values()) {
							
							for (DialogVisitor dv : dialogVisitors)
								dv.visit((CharacterActor) a, d);
							
							ArrayList<DialogOption> options = d.getOptions();

							for (DialogOption o : options) {
								for (DialogOptionVisitor ov : optionVisitors)
									ov.visit((CharacterActor) a, d, o);
							}
						}
					}
				}
			}
		}

		for (EndVisitor ev : endVisitors)
			ev.end(w);
	}
}
