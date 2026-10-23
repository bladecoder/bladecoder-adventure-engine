package com.bladecoder.setup;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Compile the bundled example at build time, never during file generation. */
public final class StarterCompiler {
    public static void main(String[] args)throws Exception {
        Path source=Paths.get(args[0]),target=Paths.get(args[1]);
        Compiler.Options options=new Compiler.Options();options.sourceFilename=source.toString();options.countAllVisits=false;
        Story story=new Compiler(new String(Files.readAllBytes(source),StandardCharsets.UTF_8),options).compile();
        if(story==null)throw new IllegalArgumentException("Starter story did not compile");
        String json=story.toJson();new Story(json);Files.createDirectories(target.getParent());Files.write(target,json.getBytes(StandardCharsets.UTF_8));
    }
}
