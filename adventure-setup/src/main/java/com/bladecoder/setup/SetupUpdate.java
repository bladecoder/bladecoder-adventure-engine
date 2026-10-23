package com.bladecoder.setup;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Three-way, file-only toolkit updates. Game sources and project recipes are never managed. */
public final class SetupUpdate {
    private final AdventureSetup setup;
    interface MutationHook { void installed(String path) throws IOException; }
    private final MutationHook hook;
    public SetupUpdate() throws IOException { this(new MutationHook() { public void installed(String path) {} }); }
    SetupUpdate(MutationHook hook) throws IOException { setup=new AdventureSetup();this.hook=hook; }

    static boolean managed(String path) {
        if(path.startsWith("skills/") || path.startsWith(".agents/skills/"))return true;
        if(Arrays.asList("AGENTS.md","README.md","tools/skills-discovery.json").contains(path))return true;
        if(path.startsWith("docs/") && path.endsWith("-workflow.md"))return true;
        if(path.equals("docs/chapter-format.md"))return true;
        // Explicit extensions/directories; never treat project data as generic toolkit content.
        if(!path.startsWith("tools/"))return false;
        String tail=path.substring(6);
        if(!tail.contains("/"))return tail.endsWith(".py") || tail.equals("README.md") || tail.equals("automation.gradle") || tail.equals("requirements-visual.txt") || tail.equals("local.example.json");
        return path.startsWith("tools/java/") || path.startsWith("tools/tests/") || path.startsWith("tools/optional-tests/") || path.startsWith("tools/licenses/") || path.startsWith("tools/model-contract/");
    }
    private static Path safe(Path root,String name) throws IOException {
        if(name.isEmpty() || name.contains("\\") || name.contains(":") || name.startsWith("/") || !Paths.get(name).normalize().toString().replace('\\','/').equals(name) || name.equals(".") || name.startsWith("../"))throw new IOException("Unsafe managed path: "+name);
        Path path=root;
        for(String part:name.split("/")) {
            path=path.resolve(part);
            if(Files.isSymbolicLink(path))throw new IOException("Symlink in managed path: "+name);
            if(!path.equals(root.resolve(name)) && Files.exists(path) && !Files.isDirectory(path))throw new IOException("Non-directory managed parent: "+name);
        }
        if(Files.exists(path) && !Files.isRegularFile(path))throw new IOException("Managed path is not a regular file: "+name);
        return path;
    }
    private static String hash(Path path) throws IOException { return Files.exists(path)?AdventureSetup.sha(Files.readAllBytes(path)):null; }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> object(Object value,String label) {
        if(!(value instanceof Map))throw new IllegalArgumentException(label+" must be an object");return (Map<String,Object>)value;
    }
    private static Object decoded(JsonValue value) {
        if(value.isObject()) {
            Map<String,Object> result=new TreeMap<String,Object>();
            for(JsonValue child=value.child;child!=null;child=child.next) {
                if(result.containsKey(child.name))throw new IllegalArgumentException("Duplicate JSON key: "+child.name);
                result.put(child.name,decoded(child));
            }return result;
        }
        if(value.isArray()){List<Object> result=new ArrayList<Object>();for(JsonValue child:value)result.add(decoded(child));return result;}
        if(value.isString())return value.asString();if(value.isBoolean())return value.asBoolean();if(value.isNull())return null;
        if(value.isLong())return value.asLong();throw new IllegalArgumentException("Unsupported update JSON number");
    }
    static Map<String,Object> parse(String text) { return object(decoded(new JsonReader().parse(text)),"Update JSON"); }
    private static Map<String,Object> read(Path path) throws IOException { return parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)); }
    private static Path project(Path path) throws IOException {
        Path root=path.toAbsolutePath().normalize();
        if(!Files.isDirectory(root) || Files.isSymbolicLink(root))throw new IOException("Project must be an existing directory");
        return root.toRealPath();
    }
    private SetupSpec spec(Path root,Map<String,Object> manifest) throws IOException {
        if(!Long.valueOf(1).equals(manifest.get("schema_version")))throw new IOException("Unsupported setup manifest schema");
        SetupSpec spec=SetupSpec.parse(AdventureSetup.json(manifest.get("spec")));
        String engine=(String)setup.identity().get("engineVersion");
        if(!engine.equals(spec.engineVersion))throw new IOException("Toolkit update does not migrate engine versions; select a matching bundle");
        Map<String,Object> current=read(safe(root,"adventure.json"));
        Properties props=new Properties();try(java.io.InputStream input=Files.newInputStream(safe(root,"gradle.properties"))){props.load(input);}
        if(!engine.equals(current.get("engine_version")) || !engine.equals(props.getProperty("bladeEngineVersion")))throw new IOException("Current project engine differs from the bundle; an explicit engine migration is required");
        return spec;
    }
    public Map<String,Object> plan(Path directory) throws IOException {
        Path root=project(directory);Path manifestPath=safe(root,"setup-manifest.json");Map<String,Object> manifest=read(manifestPath);
        SetupSpec spec=spec(root,manifest);Map<String,byte[]> incoming=setup.render(spec);
        Map<String,Object> baseline=object(manifest.get("generated_files"),"generated_files");
        SortedSet<String> names=new TreeSet<String>();names.addAll(incoming.keySet());names.addAll(baseline.keySet());
        List<Object> rows=new ArrayList<Object>();
        for(String name:names)if(managed(name)) {
            Object before=baseline.get(name);
            if(before!=null && (!(before instanceof String) || !((String)before).matches("[a-f0-9]{64}")))throw new IOException("Invalid baseline hash: "+name);
            String local=hash(safe(root,name));String next=incoming.containsKey(name)?AdventureSetup.sha(incoming.get(name)):null;
            String status=Objects.equals(local,next)?"unchanged":Objects.equals(before,next)?"authored":Objects.equals(local,before)?"update":"conflict";
            Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("path",name);row.put("baseline_sha256",before);row.put("local_sha256",local);row.put("bundle_sha256",next);row.put("status",status);
            row.put("resolution",status.equals("conflict")?"unresolved":status.equals("authored")?"keep-project":"use-bundle");rows.add(row);
        }
        Map<String,Object> plan=new LinkedHashMap<String,Object>();plan.put("schema_version",1L);plan.put("operation","toolkit-update");plan.put("bundle",setup.identity());
        plan.put("manifest_sha256",hash(manifestPath));plan.put("project_metadata_sha256",hash(safe(root,"adventure.json")));plan.put("gradle_properties_sha256",hash(safe(root,"gradle.properties")));plan.put("files",rows);
        return plan;
    }
    @SuppressWarnings("unchecked")
    public Map<String,Object> apply(Path directory,Map<String,Object> reviewed) throws IOException {
        Path root=project(directory);Path lock=root.resolve(".adventure-setup-update.lock");
        Files.write(lock,new byte[0],StandardOpenOption.CREATE_NEW);
        try {
            Map<String,Object> expected=plan(root);Map<String,Object> comparable=parse(AdventureSetup.json(reviewed));
            if(!(comparable.get("files") instanceof List))throw new IOException("Reviewed plan needs file rows");
            List<Object> supplied=(List<Object>)comparable.get("files"), actual=(List<Object>)expected.get("files");
            if(supplied.size()!=actual.size())throw new IOException("Reviewed plan file list differs from current plan");
            List<String> choices=new ArrayList<String>();
            for(int i=0;i<supplied.size();i++) {
                Map<String,Object> row=object(supplied.get(i),"file row"), original=object(actual.get(i),"file row");Object choice=row.get("resolution");
                if(!Arrays.asList("keep-project","use-bundle").contains(choice))throw new IOException("Resolve every update conflict before applying");
                choices.add((String)choice);row.put("resolution",original.get("resolution"));
            }
            if(!expected.equals(comparable))throw new IOException("Stale or modified update plan; generate and review a new plan (only resolutions may be edited)");
            Map<String,Object> manifest=read(safe(root,"setup-manifest.json"));Map<String,byte[]> incoming=setup.render(spec(root,manifest));
            Map<String,Object> baseline=object(manifest.get("generated_files"),"generated_files");
            Map<String,byte[]> writes=new LinkedHashMap<String,byte[]>();List<String> kept=new ArrayList<String>();
            for(int i=0;i<actual.size();i++) {
                Map<String,Object> row=object(actual.get(i),"file row");String name=(String)row.get("path");
                if(choices.get(i).equals("keep-project"))kept.add(name);
                else if(!Objects.equals(row.get("local_sha256"),row.get("bundle_sha256")))writes.put(name,incoming.get(name));
                if(incoming.containsKey(name))baseline.put(name,AdventureSetup.sha(incoming.get(name)));else baseline.remove(name);
            }
            Map<String,Object> record=new LinkedHashMap<String,Object>();record.put("bundle",setup.identity());record.put("kept_project",kept);record.put("partial",!kept.isEmpty());
            manifest.put("last_toolkit_update",record);
            writes.put("setup-manifest.json",(AdventureSetup.json(manifest)+"\n").getBytes(StandardCharsets.UTF_8));
            transaction(root,writes);
            Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("operation","toolkit-update");result.put("written",new ArrayList<String>(writes.keySet()));result.put("kept_project",kept);result.put("partial",!kept.isEmpty());return result;
        } finally { Files.delete(lock); }
    }
    private void transaction(Path root,Map<String,byte[]> writes) throws IOException {
        Path stage=Files.createTempDirectory(root.getParent(),".adventure-toolkit-update-");List<String> installed=new ArrayList<String>();Set<String> existed=new HashSet<String>();boolean retain=false;
        try {
            for(Map.Entry<String,byte[]> entry:writes.entrySet()) {
                Path target=safe(root,entry.getKey());
                if(Files.exists(target)){Path backup=stage.resolve("backup").resolve(entry.getKey());Files.createDirectories(backup.getParent());Files.copy(target,backup,StandardCopyOption.COPY_ATTRIBUTES);existed.add(entry.getKey());}
                if(entry.getValue()!=null){Path payload=stage.resolve("payload").resolve(entry.getKey());Files.createDirectories(payload.getParent());Files.write(payload,entry.getValue());
                    if(existed.contains(entry.getKey()))try{Files.setPosixFilePermissions(payload,Files.getPosixFilePermissions(target));}catch(UnsupportedOperationException ignored){}}
            }
            for(Map.Entry<String,byte[]> entry:writes.entrySet()) {
                Path target=safe(root,entry.getKey());Files.createDirectories(target.getParent());
                if(entry.getValue()==null)Files.deleteIfExists(target);else move(stage.resolve("payload").resolve(entry.getKey()),target);
                installed.add(entry.getKey());hook.installed(entry.getKey());
            }
        } catch(IOException | RuntimeException error) {
            Collections.reverse(installed);
            for(String name:installed)try {
                Path target=safe(root,name);if(existed.contains(name))move(stage.resolve("backup").resolve(name),target);else Files.deleteIfExists(target);
            }catch(IOException | RuntimeException rollback){retain=true;error.addSuppressed(rollback);}
            if(retain)throw new IOException("Toolkit update failed and rollback was incomplete; recover backups from "+stage,error);
            throw error;
        } finally { if(!retain)AdventureSetup.delete(stage); }
    }
    private static void move(Path source,Path target) throws IOException {
        try{Files.move(source,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException error){Files.move(source,target,StandardCopyOption.REPLACE_EXISTING);}
    }
}
