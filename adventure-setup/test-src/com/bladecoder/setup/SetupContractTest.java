package com.bladecoder.setup;

import java.nio.file.*;
import java.util.*;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Standalone behavioral checks, deliberately separate from JUnit discovery. */
public final class SetupContractTest {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    public static void main(String[] args)throws Exception {
        for(String invalid:Arrays.asList("{}","{schema_version:2}","{schema_version:4294967297}","{schema_version:1,id:../escape}","{schema_version:1,package:org.class.game}","{schema_version:1,ink:1}","{schema_version:1,platforms:[desktop,desktop]}","{schema_version:1,viewport:[4294968256,540]}","{schema_version:1,unknown:true}","{schema_version:1,id:first,id:second}")) {
            try{SetupSpec.parse(invalid);throw new AssertionError("accepted invalid specification: "+invalid);}catch(IllegalArgumentException expected){checks++;}
        }
        Path parent=Files.createTempDirectory("setup-contract-");
        try {
            AdventureSetup generator=new AdventureSetup();SetupSpec spec=SetupSpec.parse("{schema_version:1,id:neutral-probe,package:org.example.neutral,ink:false}");spec.title="L'aventure & café";
            SetupSpec incompatible=SetupSpec.parse("{schema_version:1,engine_version:4.4.0}");Path unsupported=parent.resolve("unsupported");
            try{generator.generate(incompatible,unsupported,false,null);throw new AssertionError("incompatible engine accepted");}catch(IllegalArgumentException expected){checks++;}
            check(!Files.exists(unsupported),"incompatible engine leaves no project output");
            Path target=parent.resolve("space and café");
            Map<String,Object> dry=generator.generate(spec,target,true,null);
            check(!Files.exists(target),"dry run creates no project");check(((List<?>)dry.get("files")).contains("setup-manifest.json"),"dry run reports manifest");check(((List<?>)dry.get("files")).contains(".agents/skills"),"dry run reports agent discovery symlink");
            generator.generate(spec,target,false,null);
            check(Files.exists(target.resolve("gradlew")),"wrapper generated");
            JsonValue actionContract=new JsonReader().parse(new String(Files.readAllBytes(target.resolve("tools/model-contract/actions.json")),"UTF-8"));
            check(actionContract.getString("engine_version").equals(spec.engineVersion),"action metadata targets the exact paired bundle engine, retaining reviewed-source identity on version rebinding");
            Path discovery=target.resolve(".agents/skills");
            check(Files.isSymbolicLink(discovery) && Files.readSymbolicLink(discovery).equals(Paths.get("../skills")),"agent discovery is a relative symlink to canonical skills");
            check(!Files.exists(target.resolve("tools/skills-discovery.json")),"generated project does not include obsolete discovery-copy manifest");
            String playerSkill=new String(Files.readAllBytes(target.resolve("skills/adventure-player/SKILL.md")),"UTF-8");
            String playerDiscovery=new String(Files.readAllBytes(target.resolve(".agents/skills/adventure-player/SKILL.md")),"UTF-8");
            check(playerSkill.contains("Do not inspect story/model files") && !playerSkill.contains("canonical skill"),"player skill remains canonical and player-only");
            check(playerDiscovery.equals(playerSkill),"agent discovery reads the canonical skill without a copied wrapper");
            check(Files.exists(target.resolve("AGENTS.md")) && Files.exists(target.resolve("docs/brief.md")) && Files.exists(target.resolve("docs/work-log.md")),"agent handoff is generated with actual repository paths");

            check(!Files.exists(target.resolve("android")) && !Files.exists(target.resolve("ios")),"only selected platforms generated");
            String build=new String(Files.readAllBytes(target.resolve("build.gradle")),"UTF-8");
            check(!build.contains("com.android.tools.build"),"desktop-only build has no Android plugin dependency");
            Properties props=new Properties();try(java.io.InputStream input=Files.newInputStream(target.resolve("assets/BladeEngine.properties"))){props.load(input);}
            check(spec.title.equals(props.getProperty("title")),"Unicode and special title round-trips through properties");
            JsonValue manifest=new JsonReader().parse(new String(Files.readAllBytes(target.resolve("setup-manifest.json")),"UTF-8"));
            check(!manifest.toString().contains(parent.toString()),"portable manifest excludes local destination");
            for(JsonValue entry=manifest.get("generated_files").child;entry!=null;entry=entry.next)check(AdventureSetup.sha(Files.readAllBytes(target.resolve(entry.name))).equals(entry.asString()),"generated file hash: "+entry.name);
            try{generator.generate(spec,target,false,null);throw new AssertionError("existing directory overwritten");}catch(java.io.IOException expected){checks++;}
            check(Files.exists(target.resolve("gradlew")),"existing project preserved after rejection");
            Path link=parent.resolve("linked");
            try{Files.createSymbolicLink(link,target);try{generator.generate(spec,link,false,null);throw new AssertionError("symlink overwritten");}catch(java.io.IOException expected){checks++;}}catch(UnsupportedOperationException ignored){}
            Path another=parent.resolve("another");generator.generate(spec,another,false,null);
            check(Arrays.equals(Files.readAllBytes(target.resolve("setup-manifest.json")),Files.readAllBytes(another.resolve("setup-manifest.json"))),"same spec yields identical file provenance");
            check(!Files.exists(target.resolve("assets/music")) && !Files.exists(target.resolve("raw/music")),"disabled audio has no scaffold/output dependency");
            SetupSpec audio=SetupSpec.parse("{schema_version:1,audio:true}");Path audioTarget=parent.resolve("audio");generator.generate(audio,audioTarget,false,null);
            check(Files.exists(audioTarget.resolve("assets/music/starter-loop.mp3")) && Files.exists(audioTarget.resolve("raw/music/starter-loop/compose.py")),"selected audio includes runtime cue and editable source");
            for(String id:Arrays.asList("starter-pickup","starter-signal")) {
                byte[] wav=Files.readAllBytes(audioTarget.resolve("assets/sounds/"+id+".wav"));
                check(wav[34]==16 && wav[35]==0,"starter SFX has PCM16 sample width");
            }
            check(!Files.exists(target.resolve("assets/spine")) && !Files.exists(target.resolve("raw/art/characters/neutral-rig")),"disabled Spine omits rig sources and runtime skeleton");
            SetupSpec spine=SetupSpec.parse("{schema_version:1,spine:true}");Path spineTarget=parent.resolve("spine");generator.generate(spine,spineTarget,false,null);
            check(Files.exists(spineTarget.resolve("raw/art/characters/neutral-rig/neutral-rig.spine")) && Files.exists(spineTarget.resolve("assets/spine/neutral-rig.skel")),"selected Spine includes original editable rig and runtime export");
            JsonValue spineMetadata=new JsonReader().parse(new String(Files.readAllBytes(spineTarget.resolve("adventure.json")),"UTF-8"));
            check(spineMetadata.get("tests").get("spine").get("starter").getString("actor").equals("courier"),"selected Spine declares its own diagnostic actor");
            for(String mode:Arrays.asList("pie","single_click")) {
                SetupSpec variant=SetupSpec.parse("{schema_version:1,interaction_mode:"+mode+"}");
                Path output=parent.resolve(mode);generator.generate(variant,output,false,null);
                JsonValue scenario=new JsonReader().parse(new String(Files.readAllBytes(output.resolve("tools/runtime-scenarios/smoke.json")),"UTF-8"));
                JsonValue chapter=new JsonReader().parse(new String(Files.readAllBytes(output.resolve("assets/model/00.chapter.json")),"UTF-8"));
                if(mode.equals("pie"))check(scenario.toString().contains("pie.lookat") && scenario.toString().contains("pie.pickup"),"pie smoke selects actual menu widgets");
                else {
                    JsonValue note=chapter.get("scenes").get("scene-1").get("actors").get("note");
                    check(note.getString("state").equals("unread") && note.get("verbs").has("pickup.read") && !note.get("verbs").has("pickup"),"single click first examines then enables pickup");
                }
            }
        }finally{AdventureSetup.delete(parent);}
        checks+=UpdateContractTest.run();
        checks+=GitContractTest.run();
        System.out.println("SETUP_CONTRACT_PASS checks="+checks);
    }
}
