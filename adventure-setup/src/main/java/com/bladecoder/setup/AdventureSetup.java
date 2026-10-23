package com.bladecoder.setup;

import com.badlogic.gdx.utils.JsonWriter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Shared file generator. No editor lifecycle, native backend or build invocation. */
public final class AdventureSetup {
    private final Properties versions=new Properties();
    public AdventureSetup() throws IOException {
        try(InputStream input=resource("setup-versions.properties")){versions.load(input);}
    }
    public Map<String,Object> identity() {
        Map<String,Object> data=new TreeMap<String,Object>();for(String key:versions.stringPropertyNames())data.put(key,versions.getProperty(key));return data;
    }
    public static String json(Object data) {
        return value(data).toJson(JsonWriter.OutputType.json);
    }
    private static com.badlogic.gdx.utils.JsonValue value(Object data) {
        if(data==null)return new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.nullValue);
        if(data instanceof Map) {
            com.badlogic.gdx.utils.JsonValue result=new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.object);
            for(Map.Entry<?,?> entry:((Map<?,?>)data).entrySet())result.addChild(entry.getKey().toString(),value(entry.getValue()));return result;
        }
        if(data instanceof Iterable) {
            com.badlogic.gdx.utils.JsonValue result=new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
            for(Object entry:(Iterable<?>)data)result.addChild(value(entry));return result;
        }
        if(data instanceof Boolean)return new com.badlogic.gdx.utils.JsonValue((Boolean)data);
        if(data instanceof Double || data instanceof Float){double number=((Number)data).doubleValue();if(!Double.isFinite(number))throw new IllegalArgumentException("Non-finite setup JSON number");return new com.badlogic.gdx.utils.JsonValue(number);}
        if(data instanceof Integer || data instanceof Long)return new com.badlogic.gdx.utils.JsonValue(((Number)data).longValue());
        if(data instanceof String)return new com.badlogic.gdx.utils.JsonValue((String)data);
        throw new IllegalArgumentException("Unsupported setup JSON value: "+data.getClass().getName());
    }
    static byte[] read(InputStream input)throws IOException {
        try(InputStream source=input;ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {
            byte[] block=new byte[8192];int length;while((length=source.read(block))!=-1)buffer.write(block,0,length);return buffer.toByteArray();
        }
    }
    private static InputStream resource(String path)throws IOException {
        InputStream input=AdventureSetup.class.getResourceAsStream("/"+path);
        if(input==null)throw new IOException("Missing setup resource: "+path);return input;
    }
    public Map<String,Object> generate(SetupSpec spec,Path destination,boolean dryRun,String sdk)throws IOException {
        spec.validate();if(spec.engineVersion==null)spec.engineVersion=versions.getProperty("engineVersion");
        if(!spec.engineVersion.equals(versions.getProperty("engineVersion")))throw new IllegalArgumentException("This setup bundle supports engine "+versions.getProperty("engineVersion")+"; select its matching setup bundle for another engine version.");
        destination=destination.toAbsolutePath().normalize();
        if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS))throw new IOException("Destination already exists: "+destination);
        Map<String,byte[]> outputs=render(spec);
        if(sdk!=null) {
            if(!spec.platforms.contains("android"))throw new IllegalArgumentException("SDK path requires android platform");
            outputs.put("local.properties",("sdk.dir="+property(sdk)+"\n").getBytes(StandardCharsets.UTF_8));
        }
        Map<String,Object> hashes=new LinkedHashMap<String,Object>();
        for(Map.Entry<String,byte[]> entry:outputs.entrySet())if(!entry.getKey().equals("local.properties"))hashes.put(entry.getKey(),sha(entry.getValue()));
        Map<String,Object> manifest=new LinkedHashMap<String,Object>();manifest.put("schema_version",1);manifest.put("setup_version",versions.getProperty("setupVersion"));manifest.put("setup_source_sha256",versions.getProperty("setupSourceSha256"));manifest.put("spec",spec.toMap());manifest.put("bundle_identity",identity());manifest.put("generated_files",hashes);
        outputs.put("setup-manifest.json",(json(manifest)+"\n").getBytes(StandardCharsets.UTF_8));
        List<String> generated=new ArrayList<String>(outputs.keySet());generated.add(".agents/skills");Collections.sort(generated);
        Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("schema_version",1);result.put("destination",destination.toString());result.put("dry_run",dryRun);result.put("files",generated);result.put("setup_version",versions.getProperty("setupVersion"));
        if(dryRun)return result;
        Path parent=destination.getParent();if(parent==null)throw new IOException("Destination needs a parent directory");Files.createDirectories(parent);
        Path staging=Files.createTempDirectory(parent,"."+spec.id+"-setup-");
        try {
            for(Map.Entry<String,byte[]> entry:outputs.entrySet()) {
                Path file=staging.resolve(entry.getKey()).normalize();if(!file.startsWith(staging))throw new IOException("Invalid generated path");
                Files.createDirectories(file.getParent());Files.write(file,entry.getValue(),StandardOpenOption.CREATE_NEW);
            }
            Path agents=staging.resolve(".agents");Files.createDirectories(agents);
            try{Files.createSymbolicLink(agents.resolve("skills"),Paths.get("../skills"));}
            catch(IOException | UnsupportedOperationException error){throw new IOException("Cannot create required relative symlink .agents/skills -> ../skills; enable symbolic-link support and retry.",error);}
            if(!staging.resolve("gradlew").toFile().setExecutable(true,false) && !System.getProperty("os.name").startsWith("Windows"))throw new IOException("Cannot mark Gradle wrapper executable");
            Files.createDirectories(staging.resolve("core/src/main/java/"+spec.packageName.replace('.','/')));
            Files.move(staging,destination);
        } finally {delete(staging);}
        return result;
    }
    Map<String,byte[]> render(SetupSpec spec)throws IOException {
        Map<String,byte[]> outputs=new TreeMap<String,byte[]>();
        Map<String,String> replacements=new LinkedHashMap<String,String>();
        replacements.put("%APP_NAME%",spec.id);replacements.put("%PACKAGE%",spec.packageName);replacements.put("%USE_SPINE%",Boolean.toString(spec.spine));
        replacements.put("%BLADE_ENGINE_VERSION%",spec.engineVersion);replacements.put("%LIBGDX_VERSION%",versions.getProperty("gdxVersion"));
        replacements.put("%ROBOVM_VERSION%",versions.getProperty("roboVMVersion"));replacements.put("%ANDROID_GRADLE_PLUGIN_VERSION%",versions.getProperty("androidGradlePluginVersion"));
        replacements.put("%GDX_CONTROLLERS_VERSION%",versions.getProperty("gdxControllersVersion"));replacements.put("%BLADE_INK_VERSION%",versions.getProperty("bladeInkVersion"));replacements.put("%API_LEVEL%",versions.getProperty("androidAPILevel"));
        for(String source:new String(read(resource("setup-templates.txt")),StandardCharsets.UTF_8).split("\n")) {
            if(source.isEmpty() || source.equals("local.properties"))continue;
            if(!spec.spine && (source.startsWith("raw/art/characters/neutral-rig/") || source.startsWith("assets/spine/neutral-rig.") || source.startsWith("assets/atlases/1/neutral-rig.") || (source.startsWith("tools/spine-recipes/") || source.equals("tools/asset-manifests/derived-courier.json"))))continue;
            if(!spec.audio && (source.startsWith("raw/music/") || source.startsWith("raw/sounds/") || source.startsWith("assets/music/") || source.startsWith("assets/sounds/")))continue;
            if(!spec.ink && (source.startsWith("raw/ink/") || source.equals("assets/model/main.ink.json") || source.startsWith("tools/ink-scenarios/")))continue;
            String platform=source.split("/")[0];
            if(Arrays.asList("desktop","android","ios").contains(platform) && !spec.platforms.contains(platform))continue;
            byte[] bytes=read(resource("projectTmpl/"+source));String target=source;
            if(source.equals("gitignore"))target=".gitignore";
            if(source.equals("desktop/src/DesktopLauncher"))target="desktop/src/main/java/com/bladecoder/engine/DesktopLauncher.java";
            else if(source.startsWith("desktop/src/icons/"))target=source.replace("desktop/src/icons/","desktop/src/main/resources/icons/");
            else if(source.equals("android/src/AndroidLauncher"))target="android/src/main/java/com/bladecoder/engine/AndroidLauncher.java";
            else if(source.equals("ios/src/IOSLauncher"))target="ios/src/main/java/com/bladecoder/engine/IOSLauncher.java";
            if(source.endsWith(".gradle") || source.endsWith(".properties") || source.endsWith(".xml")) {
                String text=new String(bytes,StandardCharsets.UTF_8);for(Map.Entry<String,String> replacement:replacements.entrySet())text=text.replace(replacement.getKey(),replacement.getValue());bytes=text.getBytes(StandardCharsets.UTF_8);
            }
            if(source.equals("settings.gradle")) {
                String text="include 'core'";for(String selected:spec.platforms)if(!selected.equals("android"))text+=", '"+selected+"'";
                if(spec.platforms.contains("android"))text+="\nif (gradle.startParameter.taskNames.any { !it.contains(':') || it.startsWith(':android:') || it.startsWith('android:') }) include 'android'";bytes=(text+"\n").getBytes(StandardCharsets.UTF_8);
            }
            if(source.equals("build.gradle") && !spec.platforms.contains("android"))bytes="subprojects { repositories { mavenLocal(); mavenCentral(); maven { url 'https://central.sonatype.com/repository/maven-snapshots/' } } }\n".getBytes(StandardCharsets.UTF_8);
            if(source.equals("assets/model/world")) {
                com.badlogic.gdx.utils.JsonValue model=new com.badlogic.gdx.utils.JsonReader().parse(new String(bytes,StandardCharsets.UTF_8));
                model.remove("width");model.addChild("width",value(spec.width));model.remove("height");model.addChild("height",value(spec.height));bytes=model.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8);
            }
            if(source.equals("assets/model/00.chapter.json")) {
                com.badlogic.gdx.utils.JsonValue model=new com.badlogic.gdx.utils.JsonReader().parse(new String(bytes,StandardCharsets.UTF_8));
                model.remove("bladeEngineVersion");model.addChild("bladeEngineVersion",value(spec.engineVersion));
                com.badlogic.gdx.utils.JsonValue actors=model.get("scenes").get("scene-1").get("actors");
                com.badlogic.gdx.utils.JsonValue room=actors.get("room");room.remove("scaleX");room.addChild("scaleX",new com.badlogic.gdx.utils.JsonValue(spec.width/960.0));room.remove("scaleY");room.addChild("scaleY",new com.badlogic.gdx.utils.JsonValue(spec.height/540.0));
                com.badlogic.gdx.utils.JsonValue pos=actors.get("note").get("pos");pos.remove("x");pos.addChild("x",new com.badlogic.gdx.utils.JsonValue(spec.width*490.0/960));pos.remove("y");pos.addChild("y",new com.badlogic.gdx.utils.JsonValue(spec.height*235.0/540));
                if(!spec.ink) {
                    model.remove("inkManager");com.badlogic.gdx.utils.JsonValue actions=actors.get("note").get("verbs").get("lookat");actions.remove("actions");
                    Map<String,Object> action=new LinkedHashMap<String,Object>();action.put("class","com.bladecoder.engine.actions.LookAtAction");action.put("text","@starter.note.look");action.put("wait",true);actions.addChild("actions",value(Arrays.asList(action)));
                }
                if(spec.audio) {
                    model.remove("sounds");
                    Map<String,Object> sounds=new LinkedHashMap<String,Object>();
                    for(String id:Arrays.asList("starter-pickup","starter-signal")) {
                        Map<String,Object> sound=new LinkedHashMap<String,Object>();sound.put("id",id);sound.put("filename",id+".wav");sound.put("volume",0.7);sounds.put(id,sound);
                    }
                    model.addChild("sounds",value(sounds));
                    Map<String,Object> music=new LinkedHashMap<String,Object>();music.put("filename","starter-loop.mp3");music.put("loop",true);music.put("volume",0.28);
                    model.get("scenes").get("scene-1").addChild("musicDesc",value(music));
                    Map<String,Object> soundAction=new LinkedHashMap<String,Object>();soundAction.put("class","com.bladecoder.engine.actions.PlaySoundAction");soundAction.put("sound","starter-pickup");
                    actors.get("note").get("verbs").get("pickup").get("actions").addChild(value(soundAction));
                }
                if(spec.spine) {
                    Map<String,Object> courier=new LinkedHashMap<String,Object>();courier.put("class","SpriteActor");courier.put("id","courier");courier.put("visible",true);courier.put("interaction",false);courier.put("verbs",new LinkedHashMap<String,Object>());courier.put("layer","dynamic");courier.put("bboxFromRenderer",true);courier.put("bbox",Arrays.asList(-64,0,64,0,64,80,-64,80));
                    Map<String,Object> position=new LinkedHashMap<String,Object>();position.put("x",spec.width*180.0/960);position.put("y",spec.height*210.0/540);courier.put("pos",position);courier.put("scaleX",0.75);courier.put("scaleY",0.75);
                    Map<String,Object> renderer=new LinkedHashMap<String,Object>();renderer.put("class","com.bladecoder.engine.spine.SpineRenderer");renderer.put("initAnimation","idle");renderer.put("orgAlign",4);
                    Map<String,Object> fanims=new LinkedHashMap<String,Object>();for(String id:Arrays.asList("idle","wave")){Map<String,Object> animation=new LinkedHashMap<String,Object>();animation.put("class","SpineAnimationDesc");animation.put("id",id);animation.put("source","neutral-rig");animation.put("atlas","neutral-rig");animation.put("duration",1);animation.put("animationType","REPEAT");animation.put("count",-1);animation.put("preload",true);fanims.put(id,animation);}renderer.put("fanims",fanims);courier.put("renderer",renderer);actors.addChild("courier",value(courier));
                }
                if(spec.interactionMode.equals("single_click")) {
                    com.badlogic.gdx.utils.JsonValue note=actors.get("note"),verbs=note.get("verbs");
                    note.addChild("state",value("unread"));
                    com.badlogic.gdx.utils.JsonValue pickup=verbs.remove("pickup");pickup.addChild("state",value("read"));verbs.addChild("pickup.read",pickup);
                    Map<String,Object> readState=new LinkedHashMap<String,Object>();readState.put("class","com.bladecoder.engine.actions.SetStateAction");readState.put("actor","note");readState.put("state","read");
                    verbs.get("lookat").get("actions").addChild(value(readState));
                }
                bytes=model.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8);
            }
            if(source.equals("gradle.properties"))bytes=(new String(bytes,StandardCharsets.UTF_8)+"\nversion="+spec.gameVersion+"\n").getBytes(StandardCharsets.UTF_8);
            if(source.equals("assets/BladeEngine.properties"))bytes=("title="+property(spec.title)+"\nui_mode="+spec.interactionMode+"\ninventory_pos=down\ndebug=false\nresolutions=1\n").getBytes(StandardCharsets.UTF_8);
            if(outputs.put(target,bytes)!=null)throw new IOException("Duplicate output: "+target);
        }
        for(String source:new String(read(resource("setup-toolkit.txt")),StandardCharsets.UTF_8).split("\n")) {
            if(!source.isEmpty()) {
                byte[] content=read(resource("ai-toolkit/"+source));
                if(source.equals("tools/model-contract/actions.json")) {
                    com.badlogic.gdx.utils.JsonValue contract=new com.badlogic.gdx.utils.JsonReader().parse(new String(content,StandardCharsets.UTF_8));
                    String reviewed=contract.getString("engine_version");
                    if(!reviewed.equals(spec.engineVersion)) {
                        contract.addChild("source_engine_version",value(reviewed));contract.remove("engine_version");contract.addChild("engine_version",value(spec.engineVersion));
                        content=contract.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8);
                    }
                }
                if(outputs.put(source,content)!=null)throw new IOException("Duplicate toolkit output: "+source);
            }
        }
        Map<String,Object> project=spec.toMap();project.put("setup_schema",1);
        Map<String,Object> capabilities=new LinkedHashMap<String,Object>();capabilities.put("ink",spec.ink);capabilities.put("spine",spec.spine);capabilities.put("audio",spec.audio);project.put("capabilities",capabilities);
        if(spec.ink){Map<String,Object> ink=new LinkedHashMap<String,Object>();ink.put("source","raw/ink/main.ink");ink.put("output","assets/model/main.ink.json");project.put("ink",ink);}else project.put("ink",null);
        Map<String,Object> suite=new LinkedHashMap<String,Object>();suite.put("scenario","tools/runtime-scenarios/smoke.json");suite.put("engine_replay",true);
        Map<String,Object> suites=new LinkedHashMap<String,Object>();suites.put("smoke",suite);
        Map<String,Object> tests=new LinkedHashMap<String,Object>();tests.put("suites",suites);
        Map<String,Object> profiles=new LinkedHashMap<String,Object>();
        List<Object> media=new ArrayList<Object>();
        List<Object> fast=new ArrayList<Object>();
        fast.add(qaTask("fast-tests","tests",null));fast.add(qaTask("fast-chapters","cli","chapters"));
        fast.add(qaTask("fast-formats","formats",null));if(spec.ink)fast.add(qaTask("fast-ink-links","cli","ink-links"));
        profiles.put("fast",fast);profiles.put("desktop",Arrays.asList(qaTask("desktop-smoke","suite","smoke")));
        if(spec.audio) {
            Map<String,Object> audio=new LinkedHashMap<String,Object>();audio.put("chapter","00");audio.put("scene","scene-1");audio.put("music","starter-loop.mp3");audio.put("capture_seconds",14);audio.put("min_music_wraps",2);audio.put("scenario","tools/runtime-scenarios/smoke.json");
            List<Object> sounds=new ArrayList<Object>();for(String id:Arrays.asList("starter-pickup","starter-signal")){Map<String,Object> sound=new LinkedHashMap<String,Object>();sound.put("id",id);sound.put("resource",id+".wav");sounds.add(sound);}audio.put("sounds",sounds);
            Map<String,Object> audioProfiles=new LinkedHashMap<String,Object>();audioProfiles.put("starter",audio);tests.put("audio",audioProfiles);
            media.add(qaTask("media-audio","audio","starter"));
        }
        if(spec.spine) {
            Map<String,Object> spine=new LinkedHashMap<String,Object>();spine.put("chapter","00");spine.put("scene","scene-1");spine.put("actor","courier");spine.put("source","neutral-rig");spine.put("atlas","neutral-rig");spine.put("editor_version","4.3.23");spine.put("animations",Arrays.asList("idle","wave"));spine.put("motion_bones",Arrays.asList("root","envelope"));
            Map<String,Object> spineProfiles=new LinkedHashMap<String,Object>();spineProfiles.put("starter",spine);tests.put("spine",spineProfiles);media.add(qaTask("media-spine","spine","starter"));
        }
        if(!media.isEmpty())profiles.put("media",media);
        tests.put("profiles",profiles);project.put("tests",tests);
        outputs.put("adventure.json",(json(project)+"\n").getBytes(StandardCharsets.UTF_8));
        com.badlogic.gdx.utils.JsonValue smoke=new com.badlogic.gdx.utils.JsonReader().parse(new String(read(resource("starter-smoke-"+(spec.ink?"ink":"plain")+".json")),StandardCharsets.UTF_8));
        com.badlogic.gdx.utils.JsonValue setup=smoke.get("setup");setup.remove("locale");setup.addChild("locale",value(spec.language));setup.remove("viewport");setup.addChild("viewport",value(Arrays.asList(spec.width,spec.height)));
        if(!spec.interactionMode.equals("two_buttons")) {
            com.badlogic.gdx.utils.JsonValue steps=smoke.remove("steps"),adapted=new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
            while(steps.size>0) {
                com.badlogic.gdx.utils.JsonValue step=steps.remove(0);String id=step.getString("id");
                if(id.equals("inspect-note") || id.equals("take-note")) {
                    step.remove("button");step.addChild("button",value(0));
                    if(spec.interactionMode.equals("pie")) {
                        step.remove("id");step.addChild("id",value(id+"-menu"));adapted.addChild(step);
                        Map<String,Object> settle=new LinkedHashMap<String,Object>();settle.put("id",id+"-menu-ready");settle.put("type","frames");settle.put("count",8);adapted.addChild(value(settle));
                        Map<String,Object> select=new LinkedHashMap<String,Object>();select.put("id",id);select.put("type","click");select.put("name",id.equals("inspect-note")?"pie.lookat":"pie.pickup");step=value(select);
                    }
                }
                adapted.addChild(step);
            }
            smoke.addChild("steps",adapted);
        }
        outputs.put("tools/runtime-scenarios/smoke.json",smoke.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8));
        return outputs;
    }
    private static Map<String,Object> qaTask(String id,String kind,String value) {
        Map<String,Object> task=new LinkedHashMap<String,Object>();task.put("id",id);task.put("kind",kind);if(value!=null)task.put("value",value);return task;
    }
    private static String property(String value) {
        StringBuilder text=new StringBuilder();for(char c:value.toCharArray()) {
            if(c=='\\' || c=='=' || c==':' || c=='#' || c=='!' || c==' ')text.append('\\');
            if(c>127 || c<32)text.append(String.format("\\u%04x",(int)c));else text.append(c);
        }return text.toString();
    }
    public static String sha(byte[] data)throws IOException {
        try {StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))value.append(String.format("%02x",b&255));return value.toString();}
        catch(java.security.NoSuchAlgorithmException error){throw new IOException(error);}
    }
    static void delete(Path path)throws IOException {
        if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return;
        try(java.util.stream.Stream<Path> files=Files.walk(path)){for(Path file:(Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator)Files.delete(file);}
    }
}
