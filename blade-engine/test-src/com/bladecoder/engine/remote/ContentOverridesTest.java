package com.bladecoder.engine.remote;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.bladecoder.engine.assets.ContentOverrides;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;

/** Real filesystem/hash/transaction checks; no graphics, editor or fake parser. */
final class ContentOverridesTest {
    private static int checks;
    private interface Checked {void run() throws Exception;}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void reject(Checked call) throws Exception {
        try {call.run();throw new AssertionError("Invalid override accepted");}
        catch(IllegalArgumentException expected){checks++;}
        catch(java.io.IOException expected){checks++;}
    }
    private static String hash(byte[] bytes) throws Exception {
        StringBuilder b=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(bytes))b.append(String.format("%02x",v&255));return b.toString();
    }
    private static byte[] model(int x) {
        return ("{\"optionalMetadata\":null,\"scenes\":{\"room\":{\"id\":\"room\",\"actors\":{\"actor\":{\"id\":\"actor\",\"pos\":{\"x\":"+x+",\"y\":15},\"visible\":true,\"verbs\":{}}}}}}").getBytes(StandardCharsets.UTF_8);
    }
    private static Path bundle(Path directory,byte[] original,byte[] changed,String path) throws Exception {
        Files.createDirectories(directory.resolve("files/model"));Files.write(directory.resolve("files/model/test.chapter.json"),changed);
        String text="{\"schema_version\":1,\"id\":\"test\",\"files\":[{\"path\":\""+path+"\",\"base_sha256\":\""+hash(original)+"\",\"sha256\":\""+hash(changed)+"\"}]}";
        Files.write(directory.resolve("manifest.json"),text.getBytes(StandardCharsets.UTF_8));return directory;
    }
    static int run() throws Exception {
        final Path temp=Files.createTempDirectory("blade-override-test-");
        try {
            final Path base=temp.resolve("base");Files.createDirectories(base.resolve("model"));
            final byte[] original=model(12);Files.write(base.resolve("model/test.chapter.json"),original);
            final FileHandleResolver resolver=new FileHandleResolver(){public FileHandle resolve(String path){return new FileHandle(base.resolve(path).toFile());}};
            final Path root=temp.resolve("store");final ContentOverrides store=new ContentOverrides(root,resolver);
            final Path first=bundle(temp.resolve("first"),original,model(24),"model/test.chapter.json");
            check(store.preview(first).get("changes")!=null,"preview returns explicit scalar diffs");
            check(!Files.exists(root.resolve("active.json")),"preview does not activate content");
            store.activate(first);String pointer=new String(Files.readAllBytes(root.resolve("active.json")),StandardCharsets.UTF_8);
            check(store.resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":12"),"activation waits for restart");
            ContentOverrides restarted=new ContentOverrides(root,resolver);
            check(restarted.resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":24"),"override survives restart");
            check(restarted.status().get("startup_error")==null,"valid generation loads cleanly");
            Path activeResource=restarted.resolve("model/test.chapter.json").file().toPath();
            Files.write(activeResource,model(99));
            check(restarted.resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":24"),"live readers stay pinned to validated bytes");
            check(new ContentOverrides(root,resolver).status().get("startup_error")!=null,"tampered generation is rejected on restart");
            Files.write(activeResource,model(24));
            final Path bad=bundle(temp.resolve("bad"),original,model(99),"model/test.chapter.json");
            Files.write(bad.resolve("files/model/test.chapter.json"),model(100));
            reject(new Checked(){public void run() throws Exception{store.activate(bad);}});
            check(pointer.equals(new String(Files.readAllBytes(root.resolve("active.json")),StandardCharsets.UTF_8)),"rejected patch preserves previous pointer");
            final Path incompatible=bundle(temp.resolve("incompatible"),model(13),model(24),"model/test.chapter.json");
            reject(new Checked(){public void run() throws Exception{store.preview(incompatible);}});
            final Path escaped=bundle(temp.resolve("escaped"),original,model(24),"../model/test.chapter.json");
            reject(new Checked(){public void run() throws Exception{store.preview(escaped);}});
            byte[] renamed=new String(model(24),StandardCharsets.UTF_8).replace("\"id\":\"actor\"","\"id\":\"other\"").getBytes(StandardCharsets.UTF_8);
            final Path structural=bundle(temp.resolve("structural"),original,renamed,"model/test.chapter.json");
            reject(new Checked(){public void run() throws Exception{store.preview(structural);}});
            final Path linked=bundle(temp.resolve("linked"),original,model(24),"model/test.chapter.json");
            Files.delete(linked.resolve("files/model/test.chapter.json"));Files.createSymbolicLink(linked.resolve("files/model/test.chapter.json"),base.resolve("model/test.chapter.json"));
            reject(new Checked(){public void run() throws Exception{store.preview(linked);}});
            Path second=bundle(temp.resolve("second"),original,model(30),"model/test.chapter.json");store.activate(second);
            check(new ContentOverrides(root,resolver).resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":30"),"second generation selected");
            store.rollback();check(new ContentOverrides(root,resolver).resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":24"),"rollback restores previous generation");
            store.reset();check(new ContentOverrides(root,resolver).resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":12"),"reset restores packaged base on restart");
            Files.write(root.resolve("active.json"),"{\"generation\":\"../escape\"}".getBytes(StandardCharsets.UTF_8));
            ContentOverrides rejectedStartup=new ContentOverrides(root,resolver);
            check(rejectedStartup.status().get("startup_error")!=null,"invalid startup override reports rejection");
            check(rejectedStartup.resolve("model/test.chapter.json").readString("UTF-8").contains("\"x\":12"),"invalid generation falls back without partial application");
            check(hash(Files.readAllBytes(base.resolve("model/test.chapter.json"))).equals(hash(original)),"packaged source never modified");
            return checks;
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.walk(temp)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(Exception failure){throw new RuntimeException(failure);}});
            }
        }
    }
}
