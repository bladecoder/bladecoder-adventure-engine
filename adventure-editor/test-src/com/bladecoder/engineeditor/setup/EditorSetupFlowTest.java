package com.bladecoder.engineeditor.setup;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.setup.AdventureSetup;
import com.bladecoder.setup.SetupSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Actual editor create/load API on a hidden real GL window; not an editor-widget click test. */
public final class EditorSetupFlowTest {
    private static final Map<String,Object> report=new LinkedHashMap<String,Object>();
    private static final List<String> checks=new ArrayList<String>();
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks.add(label);}
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Select a new evidence directory");
        final Path output=Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectory(output);Files.createDirectory(output.resolve("profile"));
        System.setProperty("user.home",output.resolve("profile").toString());
        report.put("passed",false);report.put("checks",checks);
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(960,540);config.setInitialVisible(false);config.disableAudio(true);
        config.setTitle("Editor setup compatibility verification");
        new Lwjgl3Application(new ApplicationAdapter(){
            @Override public void create() {
                try {
                    Ctx.project=new Project();
                    for(boolean spine:Arrays.asList(false,true)) {
                        String title=spine?"Editor Spine café":"Editor café";
                        Ctx.project.createProject(output.toString(),title,"org.example.editorprobe",null,spine);
                        Path created=output.resolve(title);
                        check(Ctx.project.isLoaded() && Ctx.project.getProjectDir().toPath().toRealPath().equals(created.toRealPath()),"editor creation loads selected project: "+title);
                        check(Ctx.project.getTitle().equals(title),"editor display title preserves Unicode: "+title);
                        check(Ctx.project.getChapter()!=null && Ctx.project.getWorld().getInitChapter().equals("00"),"editor loads generated world and chapter index: "+title);
                        JsonValue manifest=new JsonReader().parse(new String(Files.readAllBytes(created.resolve("setup-manifest.json")),StandardCharsets.UTF_8));
                        SetupSpec spec=SetupSpec.parse(manifest.get("spec").toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json));
                        Path api=output.resolve(spine?"api-spine":"api-minimal");new AdventureSetup().generate(spec,api,false,null);
                        check(Arrays.equals(Files.readAllBytes(created.resolve("setup-manifest.json")),Files.readAllBytes(api.resolve("setup-manifest.json"))),"editor and independent API share exact provenance: "+title);
                    }
                    report.put("passed",true);
                }catch(Throwable error){report.put("error",error.getClass().getSimpleName()+": "+error.getMessage());error.printStackTrace();}
                finally{Gdx.app.exit();}
            }
        },config);
        report.put("scope","Actual editor Project.createProject and loadProject through the shared generator, real hidden GL, isolated editor profile. No GUI widget input or device build is claimed.");
        Files.write(output.resolve("result.json"),(AdventureSetup.json(report)+"\n").getBytes(StandardCharsets.UTF_8));
        if(!Boolean.TRUE.equals(report.get("passed")))System.exit(1);
        System.out.println("EDITOR_SETUP_FLOW_PASS checks="+checks.size());
    }
}
