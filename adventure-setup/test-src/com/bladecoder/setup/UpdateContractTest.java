package com.bladecoder.setup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exercise actual plan/apply preservation, conflict, stale-plan and rollback behavior. */
public final class UpdateContractTest {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    private static Map<String,Object> read(Path path)throws Exception{return SetupUpdate.parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8));}
    @SuppressWarnings("unchecked") private static Map<String,Object> row(Map<String,Object> plan,String path) {
        for(Object entry:(List<Object>)plan.get("files")){Map<String,Object> row=(Map<String,Object>)entry;if(path.equals(row.get("path")))return row;}
        throw new AssertionError("No managed row: "+path);
    }
    @SuppressWarnings("unchecked") private static void old(Path root,String path,String text)throws Exception {
        Files.createDirectories(root.resolve(path).getParent());Files.write(root.resolve(path),bytes(text));
        Map<String,Object> manifest=read(root.resolve("setup-manifest.json"));
        ((Map<String,Object>)manifest.get("generated_files")).put(path,AdventureSetup.sha(bytes(text)));
        Files.write(root.resolve("setup-manifest.json"),bytes(AdventureSetup.json(manifest)));
    }
    private static Map<String,String> snapshot(Path root)throws Exception {
        Map<String,String> result=new TreeMap<String,String>();
        try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path path:(Iterable<Path>)paths.filter(Files::isRegularFile)::iterator)result.put(root.relativize(path).toString(),AdventureSetup.sha(Files.readAllBytes(path)));}return result;
    }
    public static int run()throws Exception {
        Path parent=Files.createTempDirectory("toolkit-update-contract-");
        try {
            AdventureSetup generator=new AdventureSetup();SetupSpec spec=new SetupSpec();Path root=parent.resolve("project");generator.generate(spec,root,false,null);SetupUpdate update=new SetupUpdate();
            Map<String,String> pristine=snapshot(root);Map<String,Object> plan=update.plan(root);
            check(pristine.equals(snapshot(root)),"planning writes no project files");
            check(row(plan,"tools/ink_links.py").get("status").equals("unchanged"),"matching bundle is unchanged");
            check(!SetupUpdate.managed("docs/brief.md") && !SetupUpdate.managed("docs/work-log.md") && !SetupUpdate.managed("tools/runtime-scenarios/custom.json") && !SetupUpdate.managed("assets/model/00.chapter.json") && !SetupUpdate.managed("raw/ink/main.ink") && !SetupUpdate.managed("adventure.json") && !SetupUpdate.managed("tools/local.json") && !SetupUpdate.managed("tools/asset-manifests/custom.json"),"authored game/configuration excluded from updates");
            Files.write(root.resolve("tools/ink_links.py"),bytes("local improvement"));
            check(row(update.plan(root),"tools/ink_links.py").get("status").equals("authored"),"local change retained when upstream unchanged");
            old(root,"tools/project.py","old generic helper");
            boolean executable=root.resolve("tools/project.py").toFile().setExecutable(true);
            old(root,"tools/retired.py","old removed helper");
            Files.write(root.resolve("docs/brief.md"),bytes("My authored game"));
            plan=update.plan(root);check(row(plan,"tools/project.py").get("status").equals("update"),"pristine older helper can update");
            Map<String,Object> result=update.apply(root,plan);
            check(Boolean.TRUE.equals(result.get("passed")),"reviewed update applied");
            if(executable)check(Files.isExecutable(root.resolve("tools/project.py")),"update preserves executable permission");
            check(!Files.exists(root.resolve("tools/retired.py")),"pristine retired managed helper removed");
            check(new String(Files.readAllBytes(root.resolve("tools/ink_links.py")),StandardCharsets.UTF_8).equals("local improvement"),"authored helper preserved");
            check(new String(Files.readAllBytes(root.resolve("docs/brief.md")),StandardCharsets.UTF_8).equals("My authored game"),"creative brief preserved");
            check(Boolean.TRUE.equals(result.get("partial")),"kept authored toolkit edits disclosed");
            Map<String,Object> collisionManifest=read(root.resolve("setup-manifest.json"));
            ((Map<String,Object>)collisionManifest.get("generated_files")).remove("tools/project.py");
            Files.write(root.resolve("setup-manifest.json"),bytes(AdventureSetup.json(collisionManifest)));
            Files.write(root.resolve("tools/project.py"),bytes("untracked project helper"));
            check(row(update.plan(root),"tools/project.py").get("status").equals("conflict"),"new bundle helper colliding with untracked local file is conflict");
            old(root,"tools/project.py","old baseline");Files.write(root.resolve("tools/project.py"),bytes("both changed"));
            plan=update.plan(root);check(row(plan,"tools/project.py").get("status").equals("conflict"),"both changed is conflict");
            Map<String,String> before=snapshot(root);
            try{update.apply(root,plan);throw new AssertionError("unresolved conflict accepted");}catch(IOException expected){checks++;}
            check(before.equals(snapshot(root)),"unresolved plan leaves project byte-identical");
            row(plan,"tools/project.py").put("resolution","keep-project");update.apply(root,plan);
            check(new String(Files.readAllBytes(root.resolve("tools/project.py")),StandardCharsets.UTF_8).equals("both changed"),"explicit keep conflict resolution honored");
            old(root,"tools/project.py","old baseline");Files.delete(root.resolve("tools/project.py"));
            plan=update.plan(root);check(row(plan,"tools/project.py").get("status").equals("conflict"),"local deletion conflicts with changed upstream");
            row(plan,"tools/project.py").put("resolution","use-bundle");update.apply(root,plan);
            check(Files.isRegularFile(root.resolve("tools/project.py")),"reviewed restore honored");
            plan=update.plan(root);Files.write(root.resolve(".adventure-setup-update.lock"),bytes("owned by another updater"));
            try{update.apply(root,plan);throw new AssertionError("active update lock ignored");}catch(IOException expected){checks++;}
            check(Files.exists(root.resolve(".adventure-setup-update.lock")),"other updater lock is preserved");Files.delete(root.resolve(".adventure-setup-update.lock"));
            Files.write(root.resolve("tools/project.py"),bytes("changed after review"));
            try{update.apply(root,plan);throw new AssertionError("stale plan accepted");}catch(IOException expected){checks++;}
            plan=update.plan(root);row(plan,"tools/project.py").put("path","../escape");
            try{update.apply(root,plan);throw new AssertionError("tampered path accepted");}catch(IOException expected){checks++;}
            Files.delete(root.resolve("tools/project.py"));Files.createSymbolicLink(root.resolve("tools/project.py"),parent.resolve("outside"));
            try{update.plan(root);throw new AssertionError("managed symlink accepted");}catch(IOException expected){checks++;}
            Files.delete(root.resolve("tools/project.py"));old(root,"tools/project.py","restore pristine older helper");
            old(root,"tools/ink_links.py","restore second older helper");plan=update.plan(root);before=snapshot(root);
            SetupUpdate failing=new SetupUpdate(new SetupUpdate.MutationHook(){int n;public void installed(String path)throws IOException{if(++n==2)throw new IOException("injected write failure");}});
            try{failing.apply(root,plan);throw new AssertionError("injected update failure ignored");}catch(IOException expected){checks++;}
            check(before.equals(snapshot(root)),"failed multi-file update rolls back all installed files and provenance");
            Map<String,Object> metadata=read(root.resolve("adventure.json"));metadata.put("engine_version","9.0.0");Files.write(root.resolve("adventure.json"),bytes(AdventureSetup.json(metadata)));
            try{update.plan(root);throw new AssertionError("incompatible actual engine accepted");}catch(IOException expected){checks++;}
        }finally{AdventureSetup.delete(parent);}
        return checks;
    }
}
