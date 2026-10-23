package com.bladecoder.setup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Fault tests for optional publication use a fake command transport, never an account/network. */
final class GitContractTest {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static final class Fake implements SetupGit.Commands {
        Path root;boolean repository=true,origin,dirty,committed=true,authenticated=true,failCreation;int creates,initializes;
        final List<List<String>> calls=new ArrayList<List<String>>();
        public SetupGit.Result run(Path directory,String program,String... arguments)throws IOException {
            List<String> args=Arrays.asList(arguments);calls.add(args);
            if(program.equals("gh")) {
                if(args.get(0).equals("auth"))return new SetupGit.Result(authenticated?0:1,"");
                creates++;origin=true;return new SetupGit.Result(failCreation?1:0,failCreation?"injected remote partial failure":"created");
            }
            if(args.equals(Arrays.asList("rev-parse","--show-toplevel")))return new SetupGit.Result(repository?0:128,root.toString());
            if(args.get(0).equals("init")){initializes++;repository=true;return new SetupGit.Result(0,"");}
            if(args.get(0).equals("symbolic-ref"))return new SetupGit.Result(0,"main");
            if(args.get(0).equals("remote"))return new SetupGit.Result(origin?0:2,origin?"git@example.invalid:owner/game.git":"");
            if(args.get(0).equals("status"))return new SetupGit.Result(0,dirty?" M game.txt":"");
            if(args.equals(Arrays.asList("rev-parse","--verify","HEAD")))return new SetupGit.Result(committed?0:128,"");
            throw new AssertionError("Unexpected external command: "+args);
        }
    }
    static int run()throws Exception {
        Path parent=Files.createTempDirectory("setup-git-contract-");
        try {
            Path root=parent.resolve("game");new AdventureSetup().generate(new SetupSpec(),root,false,null);
            Fake fake=new Fake();fake.root=root;fake.repository=false;SetupGit git=new SetupGit(fake);
            check(Boolean.TRUE.equals(git.initialize(root,"main").get("initialized")) && fake.initializes==1,"explicit init initializes only requested project");
            check(Boolean.FALSE.equals(git.initialize(root,"main").get("initialized")) && fake.initializes==1,"existing repository is not reset");
            try{git.initialize(root,"--bad");throw new AssertionError("unsafe branch accepted");}catch(IllegalArgumentException expected){checks++;}
            fake.root=parent;
            try{git.initialize(root,"main");throw new AssertionError("nested repo initialized implicitly");}catch(IOException expected){checks++;}
            fake.root=root;
            for(String invalid:Arrays.asList("owner/../escape","--public","owner/game/extra"))try{git.createRemote(root,invalid,"private",false,parent.resolve("bad-report"));throw new AssertionError("unsafe repo accepted");}catch(IllegalArgumentException expected){checks++;}
            fake.dirty=true;
            try{git.createRemote(root,"owner/game","private",true,parent.resolve("dirty.json"));throw new AssertionError("dirty push accepted");}catch(IOException expected){checks++;}
            check(fake.creates==0 && !Files.exists(parent.resolve("dirty.json")),"push preconditions fail before publication");fake.dirty=false;
            fake.committed=false;
            try{git.createRemote(root,"owner/game","private",true,parent.resolve("uncommitted.json"));throw new AssertionError("uncommitted push accepted");}catch(IOException expected){checks++;}fake.committed=true;
            fake.authenticated=false;Path unauth=parent.resolve("unauthenticated.json");Map<String,Object> result=git.createRemote(root,"owner/game","private",false,unauth);
            check(Boolean.FALSE.equals(result.get("passed")) && Boolean.FALSE.equals(result.get("remote_may_exist")) && fake.creates==0 && Files.exists(unauth),"unavailable authentication retained without remote mutation");fake.authenticated=true;
            fake.failCreation=true;Path partial=parent.resolve("partial.json");result=git.createRemote(root,"owner/game","private",true,partial);
            check(Boolean.FALSE.equals(result.get("passed")) && Boolean.TRUE.equals(result.get("remote_may_exist")),"partial remote failure disclosed");
            check(Files.exists(root.resolve("assets/model/world")) && new String(Files.readAllBytes(partial),StandardCharsets.UTF_8).contains("Inspect the selected remote"),"partial failure preserves project and records recovery");
            try{git.createRemote(root,"owner/game","private",false,parent.resolve("duplicate.json"));throw new AssertionError("existing origin overwritten");}catch(IOException expected){checks++;}
            fake.origin=false;fake.failCreation=false;result=git.createRemote(root,"owner/game","public",false,parent.resolve("created.json"));
            check(Boolean.TRUE.equals(result.get("passed")) && Boolean.FALSE.equals(result.get("published_commits")),"remote create does not push implicitly");
            check(fake.calls.stream().noneMatch(args -> args.contains("commit") || args.contains("delete") || args.contains("reset")),"no automatic commit, deletion or reset");
        }finally{AdventureSetup.delete(parent);}
        return checks;
    }
}
