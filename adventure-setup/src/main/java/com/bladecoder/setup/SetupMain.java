package com.bladecoder.setup;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** File-only setup CLI. Later verification/publication operations are separate. */
public final class SetupMain {
    public static void main(String[] args) {
        int status=run(args);if(status!=0)System.exit(status);
    }
    static int run(String[] args) {
        try {
            if(args.length==1 && args[0].equals("--version")){System.out.println(AdventureSetup.json(new AdventureSetup().identity()));return 0;}
            if(args.length==0 || Arrays.asList(args).contains("--help")) {
                System.out.println("Usage: adventure-setup create --spec FILE --out NEW_DIRECTORY [--dry-run] [--sdk LOCAL_PATH] [--json]\nadventure-setup plan-update --project DIRECTORY --out NEW_PLAN_FILE [--json]\nadventure-setup apply-update --project DIRECTORY --plan REVIEWED_PLAN_FILE [--json]\nadventure-setup git-init --project DIRECTORY [--branch main] [--json]\nadventure-setup github-create --project DIRECTORY --repository OWNER/NAME --visibility private|public --out NEW_REPORT [--push] [--json]\nGeneration does not build, install tools, initialize Git or launch an agent.");return 0;
            }
            if(args[0].equals("git-init") || args[0].equals("github-create")) {
                boolean remote=args[0].equals("github-create"),push=false;Map<String,String> options=new HashMap<String,String>();
                for(int i=1;i<args.length;i++) {
                    String flag=args[i];if(flag.equals("--json"))continue;if(remote && flag.equals("--push")){if(push)throw new IllegalArgumentException("Duplicate --push");push=true;continue;}
                    if(!(remote?Arrays.asList("--project","--repository","--visibility","--out"):Arrays.asList("--project","--branch")).contains(flag) || options.containsKey(flag) || ++i>=args.length || args[i].startsWith("--"))throw new IllegalArgumentException("Invalid or missing argument: "+flag);
                    options.put(flag,args[i]);
                }
                if(!options.containsKey("--project") || remote && options.size()!=4)throw new IllegalArgumentException("Repository operation is missing explicit required options");
                SetupGit git=new SetupGit();Map<String,Object> result=remote?git.createRemote(Paths.get(options.get("--project")),options.get("--repository"),options.get("--visibility"),push,Paths.get(options.get("--out"))):git.initialize(Paths.get(options.get("--project")),options.containsKey("--branch")?options.get("--branch"):"main");
                System.out.println(AdventureSetup.json(result));return Boolean.TRUE.equals(result.get("passed"))?0:1;
            }
            if(args[0].equals("plan-update") || args[0].equals("apply-update")) {
                Map<String,String> options=new HashMap<String,String>();
                for(int i=1;i<args.length;i++) {
                    String flag=args[i];if(flag.equals("--json"))continue;
                    if(!Arrays.asList("--project",args[0].equals("plan-update")?"--out":"--plan").contains(flag) || options.containsKey(flag) || ++i>=args.length || args[i].startsWith("--"))throw new IllegalArgumentException("Invalid or missing argument: "+flag);
                    options.put(flag,args[i]);
                }
                if(options.size()!=2)throw new IllegalArgumentException("Update requires --project and --out/--plan");
                SetupUpdate update=new SetupUpdate();Map<String,Object> result;
                if(args[0].equals("plan-update")) {
                    result=update.plan(Paths.get(options.get("--project")));
                    Files.write(Paths.get(options.get("--out")),(AdventureSetup.json(result)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
                } else result=update.apply(Paths.get(options.get("--project")),SetupUpdate.parse(new String(Files.readAllBytes(Paths.get(options.get("--plan"))),StandardCharsets.UTF_8)));
                System.out.println(AdventureSetup.json(result));return 0;
            }
            if(!args[0].equals("create"))throw new IllegalArgumentException("Unknown operation: "+args[0]);
            Map<String,String> values=new HashMap<String,String>();boolean dry=false;
            for(int i=1;i<args.length;i++) {
                String flag=args[i];if(flag.equals("--dry-run")){dry=true;continue;}if(flag.equals("--json"))continue;
                if(!Arrays.asList("--spec","--out","--sdk").contains(flag) || values.containsKey(flag) || ++i>=args.length || args[i].startsWith("--"))throw new IllegalArgumentException("Invalid or missing argument: "+flag);
                values.put(flag,args[i]);
            }
            if(!values.containsKey("--spec") || !values.containsKey("--out"))throw new IllegalArgumentException("create requires --spec and --out");
            SetupSpec spec=SetupSpec.parse(new String(Files.readAllBytes(Paths.get(values.get("--spec"))),StandardCharsets.UTF_8));
            Map<String,Object> result=new AdventureSetup().generate(spec,Paths.get(values.get("--out")),dry,values.get("--sdk"));
            result.put("passed",true);System.out.println(AdventureSetup.json(result));return 0;
        } catch(Exception error) {
            Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("passed",false);result.put("error",error.getClass().getSimpleName()+": "+error.getMessage());System.out.println(AdventureSetup.json(result));return 1;
        }
    }
}
