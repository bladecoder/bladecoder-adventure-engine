package com.bladecoder.engine.assets;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.bladecoder.ink.runtime.Story;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Explicit, restart-bound content overrides. Never writes to packaged assets or savegames. */
public final class ContentOverrides implements FileHandleResolver {
    private static final long FILE_LIMIT=16*1024*1024, BUNDLE_LIMIT=64*1024*1024;
    private final Path root;
    private final FileHandleResolver packaged;
    private Map<String,FileHandle> loaded=Collections.emptyMap();
    private String loadedGeneration, startupError;

    public ContentOverrides(Path directory,FileHandleResolver packaged) throws IOException {
        this.root=directory.toAbsolutePath().normalize();this.packaged=packaged;
        Files.createDirectories(root);
        try {
            JsonValue pointer=pointer();
            if(pointer!=null) {
                String generation=generation(pointer.getString("generation"));
                Plan plan=validate(root.resolve(generation));
                Map<String,FileHandle> files=new LinkedHashMap<String,FileHandle>();
                for(String path:plan.files.keySet())files.put(path,new PinnedFileHandle(root.resolve(generation).resolve("files").resolve(path),plan.files.get(path)));
                loaded=Collections.unmodifiableMap(files);loadedGeneration=generation;
            }
        } catch(Exception failure) {startupError=failure.getClass().getSimpleName()+": "+failure.getMessage();}
    }
    @Override public FileHandle resolve(String fileName) {
        FileHandle replacement=loaded.get(fileName);return replacement==null?packaged.resolve(fileName):replacement;
    }
    public Map<String,Object> status() {
        Map<String,Object> result=map("enabled",true,"supported_resources",java.util.Arrays.asList("existing actor scalar fields in chapter JSON","compatible compiled Ink stories"),"root",root.toString(),"loaded_generation",loadedGeneration,"loaded_files",new ArrayList<String>(loaded.keySet()),
            "startup_error",startupError,"boundary","restart required; live resources are unchanged");
        try {JsonValue pending=pointer();result.put("next_start_generation",pending==null?null:pending.getString("generation"));}
        catch(Exception failure){result.put("pointer_error",failure.getMessage());}
        return result;
    }
    public Map<String,Object> preview(Path bundle) throws Exception {return validate(bundle).report;}
    public Map<String,Object> activate(Path bundle) throws Exception {
        Plan plan=validate(bundle);String id=UUID.randomUUID().toString();Path target=root.resolve(id);
        Files.createDirectory(target);
        try {
            for(Map.Entry<String,byte[]> entry:plan.files.entrySet()) {
                Path path=target.resolve("files").resolve(entry.getKey());Files.createDirectories(path.getParent());Files.write(path,entry.getValue());
            }
            Files.write(target.resolve("manifest.json"),plan.manifest);
            // Revalidate the immutable generation before the atomic pointer update.
            validate(target);
            JsonValue previous=pointer();writePointer(id,previous==null?null:previous.getString("generation"));
        } catch(Exception failure) {throw new IOException("Override not activated; previous pointer preserved. Staged generation: "+id,failure);}
        Map<String,Object> report=new LinkedHashMap<String,Object>(plan.report);report.put("generation",id);report.put("boundary","restart required");return report;
    }
    public Map<String,Object> reset() throws IOException {
        Files.deleteIfExists(root.resolve("active.json"));return status();
    }
    public Map<String,Object> rollback() throws Exception {
        JsonValue old=pointer();if(old==null || !old.has("previous") || old.get("previous").isNull())throw new IllegalArgumentException("No previous override generation");
        String previous=generation(old.getString("previous"));validate(root.resolve(previous));writePointer(previous,old.getString("generation"));return status();
    }
    public String exportManifest() throws IOException {
        JsonValue pointer=pointer();if(pointer==null)throw new IllegalStateException("No selected override generation");
        return new String(read(root.resolve(generation(pointer.getString("generation"))).resolve("manifest.json"),1024*1024),StandardCharsets.UTF_8);
    }
    private void writePointer(String id,String previous) throws IOException {
        JsonValue json=new JsonValue(JsonValue.ValueType.object);json.addChild("generation",new JsonValue(id));
        json.addChild("previous",previous==null?new JsonValue(JsonValue.ValueType.nullValue):new JsonValue(previous));
        Path temporary=Files.createTempFile(root,"active-",".tmp");
        try {
            Files.write(temporary,json.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8));
            Files.move(temporary,root.resolve("active.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temporary);}
    }
    private JsonValue pointer() throws IOException {
        Path path=root.resolve("active.json");
        return Files.exists(path,LinkOption.NOFOLLOW_LINKS)?new JsonReader().parse(new String(read(path,4096),StandardCharsets.UTF_8)):null;
    }
    private static String generation(String name) {
        if(name==null || !name.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))throw new IllegalArgumentException("Invalid override generation");return name;
    }
    private Plan validate(Path directory) throws Exception {
        Path bundle=directory.toAbsolutePath().normalize();
        byte[] manifest=read(safe(bundle,"manifest.json"),1024*1024);
        JsonValue spec=new JsonReader().parse(new String(manifest,StandardCharsets.UTF_8));
        if(!spec.isObject())throw new IllegalArgumentException("Override manifest must be an object");
        for(JsonValue field:spec)if(!field.name.equals("schema_version") && !field.name.equals("id") && !field.name.equals("files"))throw new IllegalArgumentException("Unknown override manifest field");
        if(!spec.require("schema_version").isLong() || spec.getLong("schema_version")!=1)throw new IllegalArgumentException("Unsupported override schema");
        if(!spec.getString("id").matches("[a-z0-9][a-z0-9-]{0,63}"))throw new IllegalArgumentException("Invalid override id");
        JsonValue entries=spec.require("files");if(!entries.isArray() || entries.size<1 || entries.size>64)throw new IllegalArgumentException("Use 1–64 override files");
        Map<String,byte[]> files=new LinkedHashMap<String,byte[]>();List<Object> changes=new ArrayList<Object>();long total=0;
        for(JsonValue entry:entries) {
            for(JsonValue field:entry)if(!field.name.equals("path") && !field.name.equals("base_sha256") && !field.name.equals("sha256"))throw new IllegalArgumentException("Unknown override file field");
            String path=entry.getString("path");
            if(!path.matches("model/[A-Za-z0-9_.-]+\\.(chapter|ink)\\.json") || path.contains(".."))throw new IllegalArgumentException("Unsupported override resource path: "+path);
            if(files.containsKey(path))throw new IllegalArgumentException("Duplicate override path");
            String expectedBase=entry.getString("base_sha256"),expectedOutput=entry.getString("sha256");
            if(!expectedBase.matches("[a-f0-9]{64}") || !expectedOutput.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Missing SHA-256 fingerprint");
            FileHandle original=packaged.resolve(path);
            if(!original.exists() || original.length()>FILE_LIMIT)throw new IllegalArgumentException("Missing/oversized packaged base: "+path);
            byte[] base=original.readBytes();if(!hash(base).equals(expectedBase))throw new IllegalArgumentException("Packaged base fingerprint mismatch: "+path);
            byte[] bytes=read(safe(bundle,"files/"+path),FILE_LIMIT);total+=bytes.length;if(total>BUNDLE_LIMIT)throw new IllegalArgumentException("Override bundle size exceeded");
            if(!hash(bytes).equals(expectedOutput))throw new IllegalArgumentException("Replacement fingerprint mismatch: "+path);
            String text=new String(bytes,StandardCharsets.UTF_8);
            if(path.endsWith(".ink.json")) {
                new Story(text); // Real declared Ink runtime validates compiled story compatibility.
                changes.add(map("resource",path,"kind","compiled Ink replacement","base_sha256",expectedBase,"sha256",expectedOutput));
            } else {
                JsonValue before=new JsonReader().parse(new String(base,StandardCharsets.UTF_8)),after=new JsonReader().parse(text);
                diff(before,after,"",path,changes);
            }
            files.put(path,bytes);
        }
        return new Plan(manifest,files,map("id",spec.getString("id"),"files",new ArrayList<String>(files.keySet()),"changes",changes,
            "manifest_sha256",hash(manifest),"boundary","restart required","validation","Packaged base/output hashes, scalar model policy and real Ink loader; not a playthrough"));
    }
    private static void diff(JsonValue before,JsonValue after,String pointer,String resource,List<Object> changes) {
        if(before.type()!=after.type() && !(before.isNumber() && after.isNumber()))throw new IllegalArgumentException("Model type changed at "+pointer);
        if(before.isObject() || before.isArray()) {
            if(before.size!=after.size)throw new IllegalArgumentException("Model structure changed at "+pointer);
            int i=0;
            for(JsonValue child:before) {
                JsonValue other=before.isArray()?after.get(i):after.get(child.name);
                if(other==null)throw new IllegalArgumentException("Model identifier removed at "+pointer);
                diff(child,other,pointer+"/"+(before.isArray()?i:child.name.replace("~","~0").replace("/","~1")),resource,changes);i++;
            }
        } else if(!java.util.Objects.equals(before.toJson(JsonWriter.OutputType.json),after.toJson(JsonWriter.OutputType.json))) {
            if(!pointer.matches("/scenes/[^/]+/actors/[^/]+/(pos/(x|y)|visible|state|desc|scaleX|scaleY|rot|zIndex|walkingSpeed)"))
                throw new IllegalArgumentException("Unsupported model change at "+pointer);
            if(after.isNumber() && (!Double.isFinite(after.asDouble()) || Math.abs(after.asDouble())>10000000))throw new IllegalArgumentException("Invalid numeric model override");
            if((pointer.endsWith("/scaleX") || pointer.endsWith("/scaleY") || pointer.endsWith("/walkingSpeed")) && after.asDouble()<=0)throw new IllegalArgumentException("Scale/speed must be positive");
            if(pointer.endsWith("/desc") && after.asString().startsWith("@"))throw new IllegalArgumentException("Use literal text for changed descriptions; translation keys require a source rebuild");
            changes.add(map("resource",resource,"path",pointer,"before",before.toJson(JsonWriter.OutputType.json),"after",after.toJson(JsonWriter.OutputType.json)));
        }
    }
    private static Path safe(Path root,String relative) throws IOException {
        Path path=root.resolve(relative).normalize();if(!path.startsWith(root))throw new IOException("Override path escapes bundle");
        Path current=root;
        if(Files.isSymbolicLink(root))throw new IOException("Override bundle cannot be a symlink");
        for(Path part:root.relativize(path)){current=current.resolve(part);if(Files.isSymbolicLink(current))throw new IOException("Override path contains a symlink");}
        return path;
    }
    private static byte[] read(Path path,long maximum) throws IOException {
        if(Files.isSymbolicLink(path) || !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) || Files.size(path)>maximum)throw new IOException("Missing, linked or oversized override file: "+path);
        byte[] bytes=Files.readAllBytes(path);if(bytes.length>maximum)throw new IOException("Override file grew beyond limit");return bytes;
    }
    private static String hash(byte[] bytes) throws Exception {
        StringBuilder text=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))text.append(String.format("%02x",b&255));return text.toString();
    }
    private static Map<String,Object> map(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;
    }
    /** Active model/Ink readers see the validated bytes, even if a generation is edited later. */
    private static final class PinnedFileHandle extends FileHandle {
        private final byte[] bytes;
        PinnedFileHandle(Path path,byte[] bytes){super(path.toFile());this.bytes=bytes.clone();}
        @Override public java.io.InputStream read(){return new java.io.ByteArrayInputStream(bytes);}
        @Override public long length(){return bytes.length;}
        @Override public boolean exists(){return true;}
        @Override public boolean isDirectory(){return false;}
        @Override public java.io.OutputStream write(boolean append){throw new com.badlogic.gdx.utils.GdxRuntimeException("Active content is a validated read-only snapshot");}
    }
    private static final class Plan {
        final byte[] manifest;final Map<String,byte[]> files;final Map<String,Object> report;
        Plan(byte[] manifest,Map<String,byte[]> files,Map<String,Object> report){this.manifest=manifest;this.files=files;this.report=report;}
    }
}
