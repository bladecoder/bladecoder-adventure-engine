package com.bladecoder.setup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Explicit optional repository operations; generation never calls these. */
public final class SetupGit {
    interface Commands { Result run(Path root,String program,String... arguments) throws IOException; }
    static final class Result {
        final int code;final String output;
        Result(int code,String output){this.code=code;this.output=output;}
    }
    private final Commands commands;
    public SetupGit(){this(new SystemCommands());}
    SetupGit(Commands commands){this.commands=commands;}
    private static final class SystemCommands implements Commands {
        public Result run(Path root,String program,String... arguments) throws IOException {
            List<String> command=new ArrayList<String>();command.add(program);command.addAll(Arrays.asList(arguments));
            Path log=Files.createTempFile("adventure-repository-",".log");Process child=null;
            try {
                ProcessBuilder builder=new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
                // Scope Git to this selected working directory, regardless of caller repository variables.
                for(String key:new ArrayList<String>(builder.environment().keySet()))if(key.startsWith("GIT_"))builder.environment().remove(key);
                builder.environment().put("GIT_TERMINAL_PROMPT","0");builder.environment().put("GH_PROMPT_DISABLED","1");
                child=builder.start();child.getOutputStream().close();
                if(!child.waitFor(120,TimeUnit.SECONDS)){child.destroyForcibly();child.waitFor();throw new IOException("Repository command timed out; inspect its selected destination before retrying");}
                byte[] data=new byte[8192];int length;try(java.io.InputStream input=Files.newInputStream(log)){length=input.read(data);}
                String output=length<0?"":new String(data,0,length,StandardCharsets.UTF_8);
                return new Result(child.exitValue(),output.trim());
            }catch(InterruptedException error){if(child!=null)child.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Repository command interrupted; inspect destination before retrying",error);}
            finally{Files.deleteIfExists(log);}
        }
    }
    private static Path project(Path selected)throws IOException {
        Path root=selected.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(root) || !Files.isRegularFile(root.resolve("adventure.json")) || !Files.isRegularFile(root.resolve("setup-manifest.json")))throw new IOException("Select a generated adventure project");
        return root.toRealPath();
    }
    private Result git(Path root,String... arguments)throws IOException{return commands.run(root,"git",arguments);}
    private boolean ownRepository(Path root)throws IOException {
        Result result=git(root,"rev-parse","--show-toplevel");
        if(result.code!=0)return false;
        if(!Paths.get(result.output).toRealPath().equals(root))throw new IOException("Project is inside a different Git repository; nested initialization is not automatic");
        return true;
    }
    public Map<String,Object> initialize(Path selected,String branch)throws IOException {
        if(branch==null || !branch.matches("[A-Za-z][A-Za-z0-9_-]*"))throw new IllegalArgumentException("Use a portable initial branch name");
        Path root=project(selected);boolean exists=ownRepository(root);
        if(!exists) {
            if(Files.exists(root.resolve(".git"),LinkOption.NOFOLLOW_LINKS))throw new IOException("Existing .git entry cannot be initialized automatically");
            Result result=git(root,"init","--initial-branch="+branch);
            if(result.code!=0)throw new IOException("Git initialization failed; generated files are preserved. "+result.output);
        }
        Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("operation","git-init");result.put("initialized",!exists);
        Result current=git(root,"symbolic-ref","--short","HEAD");result.put("branch",current.code==0?current.output:null);result.put("committed",false);result.put("published",false);return result;
    }
    public Map<String,Object> createRemote(Path selected,String repository,String visibility,boolean push,Path reportPath)throws IOException {
        if(repository==null || !repository.matches("[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9][A-Za-z0-9_.-]*"))throw new IllegalArgumentException("Select OWNER/REPOSITORY explicitly");
        if(!Arrays.asList("private","public").contains(visibility))throw new IllegalArgumentException("Select private or public visibility explicitly");
        Path root=project(selected);
        if(!ownRepository(root))throw new IOException("Initialize the local Git repository first");
        if(git(root,"remote","get-url","origin").code==0)throw new IOException("Origin already exists; inspect it rather than creating another remote");
        if(push && (git(root,"rev-parse","--verify","HEAD").code!=0 || !git(root,"status","--porcelain").output.isEmpty()))throw new IOException("Publishing commits requires a committed clean working tree; setup never commits game content automatically");
        Path report=reportPath.toAbsolutePath().normalize();
        if(report.startsWith(root))throw new IOException("Store the publication report outside the source repository to preserve its reviewed state");
        Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("schema_version",1);result.put("operation","github-create");result.put("repository",repository);result.put("visibility",visibility);result.put("push_selected",push);result.put("passed",false);result.put("status","prepared");result.put("remote_may_exist",false);
        Files.write(report,(AdventureSetup.json(result)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
        try {
            if(commands.run(root,"gh","auth","status").code!=0)throw new IOException("GitHub CLI authentication is unavailable; configure it separately");
            result.put("status","creating");result.put("remote_may_exist",true);save(report,result);
            List<String> arguments=new ArrayList<String>(Arrays.asList("repo","create",repository,"--"+visibility,"--source",root.toString(),"--remote","origin"));if(push)arguments.add("--push");
            Result created=commands.run(root,"gh",arguments.toArray(new String[0]));
            if(created.code!=0)throw new IOException("GitHub repository creation/publication failed: "+created.output);
            Result remote=git(root,"remote","get-url","origin");
            if(remote.code!=0)throw new IOException("Remote creation returned success but origin could not be verified");
            result.put("origin",remote.output);result.put("passed",true);result.put("status","completed");result.put("published_commits",push);
        }catch(IOException failure) {
            result.put("status","failed");result.put("error",failure.getMessage());result.put("recovery","Generated files and local commits are preserved. Inspect the selected remote and origin before retrying; no remote rollback/deletion is automatic.");
        }
        save(report,result);return result;
    }
    private static void save(Path report,Map<String,Object> result)throws IOException {
        Path staged=Files.createTempFile(report.getParent(),".adventure-publication-",".json");
        try {
            Files.write(staged,(AdventureSetup.json(result)+"\n").getBytes(StandardCharsets.UTF_8));
            try{Files.move(staged,report,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(staged,report,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(staged);}
    }
}
