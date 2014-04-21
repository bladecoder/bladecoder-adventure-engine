package org.bladecoder.engine.model;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Dialog implements Serializable   {

	public final static String DEFAULT_DIALOG_VERB = "dialog";
	
	private ArrayList<DialogOption> options = new ArrayList<DialogOption>();
	
	private DialogOption currentOption;
	
	private String id;
	private String actor;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}
	
	public void selectOption(int i) {
		
		currentOption = getVisibleOptions().get(i);
		
		String v = currentOption.getVerbId();
		
		if(v == null) v = DEFAULT_DIALOG_VERB;
		
		// TODO: DELETE REFERENCE TO WORLD FROM DIALOG
		BaseActor a = World.getInstance().getCurrentScene().getActor(actor);
		a.runVerb(v);
		
		if(currentOption.getNext() != null) {
			String next = currentOption.getNext();
			
			if(next.equalsIgnoreCase("parent"))
				currentOption = currentOption.getParent();
			else
				currentOption = findSerOption(next);
		}
		
	}
	
	public void addOption(DialogOption o) {
		options.add(o);
	}
	
	public boolean ended() {
		 return (getNumVisibleOptions() == 0);
	}

	private ArrayList<DialogOption> getOptions() {
		if(currentOption == null) return options;
			
		return currentOption.options;
	}
	
	public ArrayList<DialogOption> getVisibleOptions() {
		ArrayList<DialogOption> current;
		ArrayList<DialogOption> visible = new ArrayList<DialogOption>();
		
		if(currentOption == null) current = options;
		else current =  currentOption.options;
		
		for(DialogOption o: current) {
			if(o.isVisible()) visible.add(o);
		}
		
		return visible;
	}	
	
	public void reset() {
		currentOption = null;
	}
	
	public int getNumVisibleOptions() {
		int num = 0;
		
		for(DialogOption o:getOptions()) {
			if(o.isVisible()) num++;
		}
		
		return num;
	}
	
	public void setCurrentOption(DialogOption o) {
		currentOption = o;
	}
	
	public DialogOption getCurrentOption() {
		return currentOption;
	}

	
	// ---------------------- SERIALIZATION --------------------------
	
	@Override
	public void write(Json json) {
		json.writeValue("id", id);
		json.writeValue("actor", actor);
		json.writeValue("options", options);
		
		String serCurrent = serOption(currentOption);		
		json.writeValue("currentOption", serCurrent, serCurrent == null?null:String.class);	
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
//	public void read(Json json, OrderedMap<String, Object> jsonData) {
		id = json.readValue("id", String.class, jsonData);
		actor = json.readValue("actor", String.class, jsonData);
		options = json.readValue("options", ArrayList.class, DialogOption.class, jsonData);
		String currentSer = json.readValue("currentOption", String.class, jsonData);
		currentOption = findSerOption(currentSer);
		
		// Recalculate parents
		setParents(options, null);
	}

	private void setParents(ArrayList<DialogOption> list, DialogOption parent) {
		for(DialogOption o:list) {
			o.setParent(parent);
			setParents(o.getOptions(), o);
		}		
	}
	
	public String serOption(DialogOption o) {
		
		if(o == null) return null;
		
		StringBuffer sb = new StringBuffer();
		
		DialogOption co = o;
		DialogOption parent;
		
		do {
			parent = co.getParent();
			
			int i;
			
			if(parent == null) {
				i = options.indexOf(co);
				sb.insert(0, i);
			} else {
				i = parent.getOptions().indexOf(co);
				co = parent;
				sb.insert(0, i);
				sb.insert(0, ".");
			}						
		
		} while(parent != null);
		
		return sb.toString();
	}
	
	
	public DialogOption findSerOption(String currentSer) {
		if(currentSer == null) return null;
		
		String []list = currentSer.split("[.]");
		
		DialogOption o = null;
		
		for(String s:list) {
			int i = Integer.parseInt(s);
			
			if(o == null) o = options.get(i);
			else o = o.getOptions().get(i);
		}
		
		return o;
	}

}
