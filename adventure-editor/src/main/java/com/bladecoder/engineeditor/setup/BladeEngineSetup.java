package com.bladecoder.engineeditor.setup;

import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Versions;

import java.io.*;

/**
 * Command line tool to generate libgdx projects
 *
 * @author rgarcia
 */
public class BladeEngineSetup {
    public static boolean isSdkLocationValid(String sdkLocation) {
        return new File(sdkLocation, "platforms").exists();
    }

    public static boolean isEmptyDirectory(String destination) {
        if (new File(destination).exists()) {
            return new File(destination).list().length == 0;
        } else {
            return true;
        }
    }

    public static boolean isSdkUpToDate(String sdkLocation) {

        File apis = new File(sdkLocation, "platforms");
        if (!apis.exists()) {
            EditorLogger.error("You have no Android APIs!\nUpdate your Android SDK with API level: "
                    + Versions.getAndroidAPILevel());
            return false;
        }

        int newestLocalApi = getLatestApi(apis);
        if (newestLocalApi > Integer.parseInt(Versions.getAndroidAPILevel())) {

            // ALWAYS USE THE CURRENT API
            Versions.setAndroidAPILevel(Integer.toString(newestLocalApi));

            EditorLogger.error("Using API level: " + Versions.getAndroidAPILevel());
        } else {
            if (!hasFileInDirectory(apis, "android-" + Versions.getAndroidAPILevel())) {
                EditorLogger.error(
                        "Please update your Android SDK, you need the Android API: " + Versions.getAndroidAPILevel());
                return false;
            }
        }
        return true;
    }

    private static boolean hasFileInDirectory(File file, String fileName) {
        for (String name : file.list()) {
            if (name.equals(fileName))
                return true;
        }
        return false;
    }

    private static int getLatestApi(File apis) {
        int apiLevel = 0;
        for (File api : apis.listFiles()) {
            int level = readAPIVersion(api);
            if (level > apiLevel)
                apiLevel = level;
        }
        return apiLevel;
    }

    private static int readAPIVersion(File parentFile) {
        File properties = new File(parentFile, "source.properties");
        FileReader reader;
        BufferedReader buffer;
        try {
            reader = new FileReader(properties);
            buffer = new BufferedReader(reader);

            String line = null;

            while ((line = buffer.readLine()) != null) {
                if (line.contains("AndroidVersion.ApiLevel")) {

                    String versionString = line.split("\\=")[1];
                    int apiLevel = Integer.parseInt(versionString);

                    buffer.close();
                    reader.close();

                    return apiLevel;
                }
            }
        } catch (IOException | NumberFormatException e) {
            EditorLogger.printStackTrace(e);
        }

        return 0;
    }

    public void build(String outputDir, String appName, String packageName, String mainClass, String sdkLocation,
                      boolean spinePlugin) throws IOException {
        com.bladecoder.setup.SetupSpec spec = new com.bladecoder.setup.SetupSpec();
        String id = appName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if(id.isEmpty() || !Character.isLetter(id.charAt(0))) id="adventure-"+id;
        spec.id=id;spec.title=appName;spec.packageName=packageName;spec.spine=spinePlugin;
        spec.platforms=new java.util.ArrayList<String>(java.util.Arrays.asList("desktop","android","ios"));
        new com.bladecoder.setup.AdventureSetup().generate(spec,new File(outputDir).toPath(),false,
                sdkLocation==null || sdkLocation.isEmpty()?null:sdkLocation);
    }
}
