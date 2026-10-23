import com.bladecoder.ink.runtime.Story;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Load compiler output with the exact runtime selected by gradle.properties. */
class InkProbe {
    public static void main(String[] args) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Story story = new Story(json);
        System.out.println("Loaded compiled story with the game's Ink runtime.");
    }
}
