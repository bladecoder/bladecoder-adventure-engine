package org.bladecoder.engine.model;

import java.util.ArrayList;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionEndTrigger;
import org.bladecoder.engine.actions.RunVerbAction;
import org.bladecoder.engine.util.EngineLogger;

public class Verb implements ActionCallback {
	private String id;
	
	private ArrayList<Action> actions = new ArrayList <Action>();
	
	int ip = -1;
	
	public Verb() {
	}
	
	public Verb(String id) {
		this.id=id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void add(Action a) {
		actions.add(a);
	}
	
	public ArrayList<Action> getActions() {
		return actions;
	}
	
	public void run() {
		ip = 0;
		nextStep();
	}
	
	public void nextStep() {
		
		boolean stop = false;
		
		while(ip < actions.size() && !stop) {
			Action a = actions.get(ip);
			if(a instanceof ActionEndTrigger) {
				((ActionEndTrigger)a).setCallback(this);
				stop = true;
			}
			
			ip++;
			
			try {
				a.run();
			} catch (Exception e) {
				EngineLogger.error("EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName(), e);
			}		
		}
	}

	@Override
	public void onEvent() {
		nextStep();	
	}


	public void cancel() {
		for(Action c:actions) {
			if(c instanceof RunVerbAction)
				((RunVerbAction)c).cancel();
		}		
		
		ip = actions.size();
	}	
}
