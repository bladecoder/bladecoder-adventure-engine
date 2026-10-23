import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Use the cached compiler version declared by the game, with normal INCLUDE resolution. */
class InkCompiler {
    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args[0]);
        Compiler.Options options = new Compiler.Options();
        options.sourceFilename = source.toString();
        options.countAllVisits = false;
        Compiler compiler = new Compiler(new String(Files.readAllBytes(source), StandardCharsets.UTF_8), options);
        Story story = compiler.compile();
        if (story == null) throw new IllegalArgumentException("Ink compilation produced no story");
        Files.write(Paths.get(args[1]), story.toJson().getBytes(StandardCharsets.UTF_8));
    }
}
