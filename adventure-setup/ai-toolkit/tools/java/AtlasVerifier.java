import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.StringWriter;

/** Parse with the pinned LibGDX reader without creating a graphics context. */
class AtlasVerifier {
    public static void main(String[] args) throws Exception {
        FileHandle file = new FileHandle(args[0]);
        TextureAtlasData data = new TextureAtlasData(file, file.parent(), false);
        StringWriter text = new StringWriter();
        JsonWriter json = new JsonWriter(text);json.setOutputType(JsonWriter.OutputType.json);
        json.object().array("pages");
        for (TextureAtlasData.Page page : data.getPages()) {
            if (!page.textureFile.exists()) throw new IllegalArgumentException("Missing atlas page");
            json.value(page.textureFile.name());
        }
        json.pop().array("regions");
        for (TextureAtlasData.Region r : data.getRegions()) {
            if (r.width <= 0 || r.height <= 0 || r.left < 0 || r.top < 0 || r.left+(r.rotate?r.height:r.width) > r.page.width || r.top+(r.rotate?r.width:r.height) > r.page.height)
                throw new IllegalArgumentException("Invalid region bounds: " + r.name);
            json.object().set("name",r.name).set("index",r.index)
                .set("width",r.width).set("height",r.height).set("rotate",r.rotate)
                .set("originalWidth",r.originalWidth).set("originalHeight",r.originalHeight)
                .set("offsetX",r.offsetX).set("offsetY",r.offsetY);
            for (String name : new String[]{"split","pad"}) {
                json.array(name);int[] values=r.findValue(name);
                if(values!=null)for(int value:values)json.value(value);
                json.pop();
            }
            json.pop();
        }
        json.pop().pop();json.close();
        System.out.println("ATLAS_DATA="+text);
    }
}
