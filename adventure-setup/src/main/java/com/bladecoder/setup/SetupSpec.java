package com.bladecoder.setup;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.*;

/** Versioned project inputs. Destinations and personal tool/SDK paths are separate. */
public final class SetupSpec {
    public String id = "my-adventure", title = "My Adventure", packageName = "org.example.adventure";
    public String engineVersion, gameVersion = "0.1.0", language = "en", interactionMode = "two_buttons", artStorage = "repository";
    public int width = 960, height = 540;
    public boolean ink = true, spine, audio;
    public List<String> platforms = new ArrayList<String>(Arrays.asList("desktop"));

    public static SetupSpec parse(String text) {
        JsonValue root = new JsonReader().parse(text);
        if (!root.isObject()) throw new IllegalArgumentException("Setup specification must be an object");
        Set<String> allowed = new HashSet<String>(Arrays.asList("schema_version","id","title","package","engine_version","game_version","language","viewport","interaction_mode","art_storage","platforms","ink","spine","audio"));
        Set<String> seen=new HashSet<String>();
        for (JsonValue value=root.child; value!=null; value=value.next) {
            if(!seen.add(value.name))throw new IllegalArgumentException("Duplicate setup setting: "+value.name);
            if (!allowed.contains(value.name)) throw new IllegalArgumentException("Unknown setup setting: "+value.name);
        }
        if (!root.has("schema_version") || !root.get("schema_version").isLong() || root.getLong("schema_version")!=1)
            throw new IllegalArgumentException("Use setup schema_version 1");
        SetupSpec spec = new SetupSpec();
        spec.id=string(root,"id",spec.id);spec.title=string(root,"title",spec.title);
        spec.packageName=string(root,"package",spec.packageName);spec.engineVersion=string(root,"engine_version",null);
        spec.gameVersion=string(root,"game_version",spec.gameVersion);spec.language=string(root,"language",spec.language);
        spec.interactionMode=string(root,"interaction_mode",spec.interactionMode);spec.artStorage=string(root,"art_storage",spec.artStorage);
        spec.ink=bool(root,"ink",true);spec.spine=bool(root,"spine",false);spec.audio=bool(root,"audio",false);
        if (root.has("platforms")) {
            JsonValue list=root.get("platforms");if(!list.isArray())throw new IllegalArgumentException("platforms must be an array");
            spec.platforms.clear();for(JsonValue value:list){if(!value.isString())throw new IllegalArgumentException("platforms must contain strings");spec.platforms.add(value.asString());}
        }
        if(root.has("viewport")) {
            JsonValue view=root.get("viewport");
            if(!view.isArray() || view.size!=2 || !view.get(0).isLong() || !view.get(1).isLong())throw new IllegalArgumentException("viewport must contain two integers");
            if(view.getLong(0)<320 || view.getLong(0)>8192 || view.getLong(1)<200 || view.getLong(1)>8192)throw new IllegalArgumentException("Invalid viewport dimensions");
            spec.width=view.getInt(0);spec.height=view.getInt(1);
        }
        spec.validate();return spec;
    }
    private static String string(JsonValue root,String name,String fallback) {
        if(!root.has(name))return fallback;
        if(!root.get(name).isString())throw new IllegalArgumentException(name+" must be a string");return root.getString(name);
    }
    private static boolean bool(JsonValue root,String name,boolean fallback) {
        if(!root.has(name))return fallback;
        if(!root.get(name).isBoolean())throw new IllegalArgumentException(name+" must be a boolean");return root.getBoolean(name);
    }
    public void validate() {
        if(id==null || !id.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*"))throw new IllegalArgumentException("id must be a lowercase portable project identifier");
        if(title==null || title.trim().isEmpty() || title.length()>120 || title.matches("(?s).*[\\p{Cntrl}].*"))throw new IllegalArgumentException("Invalid project title");
        Set<String> keywords=new HashSet<String>(Arrays.asList("abstract","assert","boolean","break","byte","case","catch","char","class","const","continue","default","do","double","else","enum","extends","final","finally","float","for","goto","if","implements","import","instanceof","int","interface","long","native","new","package","private","protected","public","return","short","static","strictfp","super","switch","synchronized","this","throw","throws","transient","try","void","volatile","while","true","false","null","_"));
        if(packageName==null || !packageName.matches("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+"))throw new IllegalArgumentException("Invalid Java package");
        for(String part:packageName.split("\\."))if(keywords.contains(part))throw new IllegalArgumentException("Java keyword in package: "+part);
        if(engineVersion!=null && !version(engineVersion) || !version(gameVersion))throw new IllegalArgumentException("Invalid version identifier");
        if(!language.matches("[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*"))throw new IllegalArgumentException("Invalid primary language");
        if(width<320 || height<200 || width>8192 || height>8192)throw new IllegalArgumentException("Invalid viewport dimensions");
        if(!Arrays.asList("two_buttons","pie","single_click").contains(interactionMode))throw new IllegalArgumentException("Unknown interaction mode");
        if(!Arrays.asList("repository","external").contains(artStorage))throw new IllegalArgumentException("Invalid art storage policy");
        if(platforms.isEmpty() || new HashSet<String>(platforms).size()!=platforms.size() || !Arrays.asList("desktop","android","ios").containsAll(platforms) || !platforms.contains("desktop"))throw new IllegalArgumentException("Select desktop and optional android/ios platforms");
    }
    private static boolean version(String text){return text!=null && text.matches("[0-9]+(?:\\.[0-9]+)+(?:[-+][A-Za-z0-9.-]+)?");}
    public Map<String,Object> toMap() {
        Map<String,Object> data=new LinkedHashMap<String,Object>();data.put("schema_version",1);data.put("id",id);data.put("title",title);data.put("package",packageName);
        data.put("engine_version",engineVersion);data.put("game_version",gameVersion);data.put("language",language);data.put("viewport",Arrays.asList(width,height));
        data.put("interaction_mode",interactionMode);data.put("art_storage",artStorage);data.put("platforms",new ArrayList<String>(platforms));data.put("ink",ink);data.put("spine",spine);data.put("audio",audio);return data;
    }
}
