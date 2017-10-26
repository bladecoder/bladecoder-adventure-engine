/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.bladecoder.engineeditor.setup;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.common.Versions;

/**
 * Command line tool to generate libgdx projects
 * 
 * @author badlogic
 * @author Tomski
 * @author rgarcia
 */
public class BladeEngineSetup {
	public static boolean isSdkLocationValid(String sdkLocation) {
		return new File(sdkLocation, "tools").exists() && new File(sdkLocation, "platforms").exists();
	}

	public static boolean isEmptyDirectory(String destination) {
		if (new File(destination).exists()) {
			return new File(destination).list().length == 0;
		} else {
			return true;
		}
	}

	public static boolean isSdkUpToDate(String sdkLocation) {
		File buildTools = new File(sdkLocation, "build-tools");
		if (!buildTools.exists()) {
			EditorLogger.error("You have no build tools!\nUpdate your Android SDK with build tools version: "
					+ Versions.getBuildToolsVersion());
			return false;
		}

		File apis = new File(sdkLocation, "platforms");
		if (!apis.exists()) {
			EditorLogger.error("You have no Android APIs!\nUpdate your Android SDK with API level: "
					+ Versions.getAndroidAPILevel());
			return false;
		}
		String newestLocalTool = getLatestTools(buildTools);
		int[] localToolVersion = convertTools(newestLocalTool);
		int[] targetToolVersion = convertTools(Versions.getBuildToolsVersion());
		if (compareVersions(targetToolVersion, localToolVersion)) {
			// ALWAYS USE THE CURRENT BUILD TOOLS
			Versions.setBuildToolsVersion(newestLocalTool);

			EditorLogger.error("Using build tools: " + Versions.getBuildToolsVersion());
		} else {
			if (!hasFileInDirectory(buildTools, Versions.getBuildToolsVersion())) {
				EditorLogger.error(
						"Please update your Android SDK, you need build tools: " + Versions.getBuildToolsVersion());
				return false;
			}
		}

		int newestLocalApi = getLatestApi(apis);
		if (newestLocalApi > Integer.valueOf(Versions.getAndroidAPILevel())) {

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

	private static String getLatestTools(File buildTools) {
		String version = null;
		int[] versionSplit = new int[3];
		int[] testSplit = new int[3];
		for (File toolsVersion : buildTools.listFiles()) {
			if (version == null) {
				version = readBuildToolsVersion(toolsVersion);
				versionSplit = convertTools(version);
				continue;
			}
			testSplit = convertTools(readBuildToolsVersion(toolsVersion));
			if (compareVersions(versionSplit, testSplit)) {
				version = readBuildToolsVersion(toolsVersion);
			}
		}
		if (version != null) {
			return version;
		} else {
			return "0.0.0";
		}
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

	private static String readBuildToolsVersion(File parentFile) {
		File properties = new File(parentFile, "source.properties");
		FileReader reader;
		BufferedReader buffer;
		try {
			reader = new FileReader(properties);
			buffer = new BufferedReader(reader);

			String line = null;

			while ((line = buffer.readLine()) != null) {
				if (line.contains("Pkg.Revision")) {

					String versionString = line.split("\\=")[1];
					int count = versionString.split("\\.").length;
					for (int i = 0; i < 3 - count; i++) {
						versionString += ".0";
					}

					buffer.close();
					reader.close();

					return versionString;
				}
			}
		} catch (IOException e) {
			EditorLogger.printStackTrace(e);
		}
		return "0.0.0";
	}

	private static boolean compareVersions(int[] version, int[] testVersion) {
		if (testVersion[0] > version[0]) {
			return true;
		} else if (testVersion[0] == version[0]) {
			if (testVersion[1] > version[1]) {
				return true;
			} else if (testVersion[1] == version[1]) {
				return testVersion[2] > version[2];
			}
		}
		return false;
	}

	private static int[] convertTools(String toolsVersion) {
		String[] stringSplit = toolsVersion.split("\\.");
		int[] versionSplit = new int[3];
		if (stringSplit.length == 3) {
			try {
				versionSplit[0] = Integer.parseInt(stringSplit[0]);
				versionSplit[1] = Integer.parseInt(stringSplit[1]);
				versionSplit[2] = Integer.parseInt(stringSplit[2]);
				return versionSplit;
			} catch (NumberFormatException nfe) {
				return new int[] { 0, 0, 0 };
			}
		} else {
			return new int[] { 0, 0, 0 };
		}
	}

	public void build(String outputDir, String appName, String packageName, String mainClass, String sdkLocation,
			boolean spinePlugin) throws IOException {
		ProjectSetup project = new ProjectSetup();

		String packageDir = packageName.replace('.', '/');
		String sdkPath = null;

		if (sdkLocation != null && !sdkLocation.isEmpty())
			sdkPath = sdkLocation.replace('\\', '/');

		if (!isSdkLocationValid(sdkLocation)) {
			EditorLogger.error("Android SDK location '" + sdkLocation + "' doesn't contain an SDK");
		} else if (!isSdkUpToDate(sdkLocation)) {
			// SHOW THE ANDROID SDK MANAGER??
		}

		// root dir/gradle files
		project.files.add(new ProjectFile("gitignore", ".gitignore", false));
		project.files.add(new ProjectFile("settings.gradle", false));
		project.files.add(new ProjectFile("gradlew", false));
		project.files.add(new ProjectFile("gradlew.bat", false));
		project.files.add(new ProjectFile("gradle/wrapper/gradle-wrapper.jar", false));
		project.files.add(new ProjectFile("gradle/wrapper/gradle-wrapper.properties", false));
		project.files.add(new ProjectFile("gradle.properties"));
		project.files.add(new ProjectFile("build.gradle", false));

		// core project
		project.files.add(new ProjectFile("core/build.gradle"));
		new File(outputDir + "/core/src/main/java").mkdirs();

		// DESKTOP project
		project.files.add(new ProjectFile("desktop/build.gradle"));
		project.files.add(new ProjectFile("desktop/src/DesktopLauncher",
				"desktop/src/main/java/" + packageDir + "/desktop/DesktopLauncher.java", true));

		project.files.add(new ProjectFile("desktop/src/icons/icon16.png", "desktop/src/main/resources/icons/icon16.png", false));
		project.files.add(new ProjectFile("desktop/src/icons/icon32.png", "desktop/src/main/resources/icons/icon32.png", false));
		project.files.add(new ProjectFile("desktop/src/icons/icon128.png", "desktop/src/main/resources/icons/icon128.png", false));

		// Assets
		String assetPath = "assets";

		// CREATE ASSETS EMPTY FOLDERS
		new File(outputDir + "/" + assetPath + "/3d").mkdirs();
		new File(outputDir + "/" + assetPath + "/atlases/1").mkdirs();
		new File(outputDir + "/" + assetPath + "/images/1").mkdirs();
		new File(outputDir + "/" + assetPath + "/model").mkdirs();
		new File(outputDir + "/" + assetPath + "/music").mkdirs();
		new File(outputDir + "/" + assetPath + "/sounds").mkdirs();
		new File(outputDir + "/" + assetPath + "/spine").mkdirs();
		new File(outputDir + "/" + assetPath + "/voices").mkdirs();
		new File(outputDir + "/" + assetPath + "/particles").mkdirs();
		new File(outputDir + "/" + assetPath + "/tests").mkdirs();
		new File(outputDir + "/" + assetPath + "/ui/1").mkdirs();
		new File(outputDir + "/" + assetPath + "/ui/fonts").mkdirs();

		project.files.add(new ProjectFile("assets/model/00.chapter.json", assetPath + "/model/00.chapter.json", false));
		project.files
				.add(new ProjectFile("assets/model/world.properties", assetPath + "/model/world.properties", false));
		project.files.add(new ProjectFile("assets/model/world", assetPath + "/model/world", false));
		project.files.add(
				new ProjectFile("assets/model/world_es.properties", assetPath + "/model/world_es.properties", false));

		project.files.add(new ProjectFile("assets/ui/credits.txt", assetPath + "/ui/credits.txt", false));
		project.files
				.add(new ProjectFile("assets/ui/fonts/PaytoneOne.ttf", assetPath + "/ui/fonts/PaytoneOne.ttf", false));
		project.files.add(new ProjectFile("assets/ui/fonts/ArchitectsDaughter.ttf",
				assetPath + "/ui/fonts/ArchitectsDaughter.ttf", false));
		project.files.add(
				new ProjectFile("assets/ui/fonts/Roboto-Black.ttf", assetPath + "/ui/fonts/Roboto-Black.ttf", false));
		project.files.add(new ProjectFile("assets/ui/fonts/Roboto-Regular.ttf",
				assetPath + "/ui/fonts/Roboto-Regular.ttf", false));
		project.files.add(
				new ProjectFile("assets/ui/fonts/Roboto-Thin.ttf", assetPath + "/ui/fonts/Roboto-Thin.ttf", false));
		project.files.add(new ProjectFile("assets/ui/fonts/Ubuntu-M.ttf", assetPath + "/ui/fonts/Ubuntu-M.ttf", false));
		project.files.add(new ProjectFile("assets/ui/ui.json", assetPath + "/ui/ui.json", false));

		project.files.add(new ProjectFile("assets/ui/1/blade_logo.png", assetPath + "/ui/1/blade_logo.png", false));
		project.files.add(new ProjectFile("assets/ui/1/helpDesktop.png", assetPath + "/ui/1/helpDesktop.png", false));
		project.files
				.add(new ProjectFile("assets/ui/1/helpDesktop_es.png", assetPath + "/ui/1/helpDesktop_es.png", false));
		project.files.add(new ProjectFile("assets/ui/1/helpPie.png", assetPath + "/ui/1/helpPie.png", false));
		project.files.add(new ProjectFile("assets/ui/1/helpPie_es.png", assetPath + "/ui/1/helpPie_es.png", false));
		project.files.add(new ProjectFile("assets/ui/1/libgdx_logo.png", assetPath + "/ui/1/libgdx_logo.png", false));
		project.files.add(new ProjectFile("assets/ui/1/ui.atlas", assetPath + "/ui/1/ui.atlas", false));
		project.files.add(new ProjectFile("assets/ui/1/ui.png", assetPath + "/ui/1/ui.png", false));

		project.files
				.add(new ProjectFile("assets/BladeEngine.properties", assetPath + "/BladeEngine.properties", false));

		// ANDROID project
		project.files.add(new ProjectFile("android/res/values/strings.xml"));
		project.files.add(new ProjectFile("android/res/values/styles.xml", false));
		project.files.add(new ProjectFile("android/res/drawable-hdpi/ic_launcher.png", false));
		project.files.add(new ProjectFile("android/res/drawable-mdpi/ic_launcher.png", false));
		project.files.add(new ProjectFile("android/res/drawable-xhdpi/ic_launcher.png", false));
		project.files.add(new ProjectFile("android/res/drawable-xxhdpi/ic_launcher.png", false));
		project.files.add(new ProjectFile("android/res/drawable-xxxhdpi/ic_launcher.png", false));
		project.files.add(new ProjectFile("android/src/AndroidLauncher",
				"android/src/main/java/" + packageDir + "/AndroidLauncher.java", true));
		project.files.add(new ProjectFile("android/AndroidManifest.xml"));
		project.files.add(new ProjectFile("android/build.gradle", true));
		project.files.add(new ProjectFile("android/ic_launcher-web.png", false));
		project.files.add(new ProjectFile("android/proguard-rules.pro", false));
		project.files.add(new ProjectFile("android/project.properties", true));

		if (sdkLocation != null)
			project.files.add(new ProjectFile("local.properties", true));

		// IOS ROBOVM
		project.files.add(
				new ProjectFile("ios/src/IOSLauncher", "ios/src/main/java/" + packageDir + "/IOSLauncher.java", true));
		project.files.add(new ProjectFile("ios/data/Default.png", false));
		project.files.add(new ProjectFile("ios/data/Default@2x.png", false));
		project.files.add(new ProjectFile("ios/data/Default@2x~ipad.png", false));
		project.files.add(new ProjectFile("ios/data/Default-568h@2x.png", false));
		project.files.add(new ProjectFile("ios/data/Default~ipad.png", false));
		project.files.add(new ProjectFile("ios/data/Default-375w-667h@2x.png", false));
		project.files.add(new ProjectFile("ios/data/Default-414w-736h@3x.png", false));
		project.files.add(new ProjectFile("ios/data/Default-1024w-1366h@2x~ipad.png", false));

		project.files.add(new ProjectFile("ios/data/Media.xcassets/Contents.json", false));
		project.files
				.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/app-store-icon-1024@1x.png", false));
		project.files.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/Contents.json", false));
		project.files.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-app-icon-76@1x.png", false));
		project.files.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-app-icon-76@2x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-notifications-icon-20@1x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-notifications-icon-20@2x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-pro-app-icon-83.5@2x.png", false));
		project.files
				.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-settings-icon-29@1x.png", false));
		project.files
				.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-settings-icon-29@2x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-spotlight-icon-40@1x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/ipad-spotlight-icon-40@2x.png", false));
		project.files
				.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/iphone-app-icon-60@2x.png", false));
		project.files
				.add(new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/iphone-app-icon-60@3x.png", false));
		project.files.add(new ProjectFile(
				"ios/data/Media.xcassets/AppIcon.appiconset/iphone-notification-icon-20@2x.png", false));
		project.files.add(new ProjectFile(
				"ios/data/Media.xcassets/AppIcon.appiconset/iphone-notification-icon-20@3x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/iphone-spotlight-icon-40@2x.png", false));
		project.files.add(
				new ProjectFile("ios/data/Media.xcassets/AppIcon.appiconset/iphone-spotlight-icon-40@3x.png", false));
		project.files.add(new ProjectFile(
				"ios/data/Media.xcassets/AppIcon.appiconset/iphone-spotlight-settings-icon-29@2x.png", false));
		project.files.add(new ProjectFile(
				"ios/data/Media.xcassets/AppIcon.appiconset/iphone-spotlight-settings-icon-29@3x.png", false));

		project.files.add(new ProjectFile("ios/build.gradle", true));
		project.files.add(new ProjectFile("ios/Info.plist.xml", false));
		project.files.add(new ProjectFile("ios/robovm.properties"));
		project.files.add(new ProjectFile("ios/robovm.xml", true));

		Map<String, String> values = new HashMap<String, String>();
		values.put("%APP_NAME%", appName);
		values.put("%APP_NAME_ESCAPED%", appName.replace("'", "\\'"));
		values.put("%PACKAGE%", packageName);
		values.put("%PACKAGE_DIR%", packageDir);
		values.put("%MAIN_CLASS%", mainClass);
		values.put("%USE_SPINE%", Boolean.toString(spinePlugin));

		if (sdkPath != null)
			values.put("%ANDROID_SDK%", sdkPath);

		values.put("%ASSET_PATH%", assetPath);
		values.put("%BUILD_TOOLS_VERSION%", Versions.getBuildToolsVersion());
		values.put("%API_LEVEL%", Versions.getAndroidAPILevel());

		values.put("%BLADE_ENGINE_VERSION%", Versions.getVersion());
		values.put("%LIBGDX_VERSION%", Versions.getLibgdxVersion());
		values.put("%ROBOVM_VERSION%", Versions.getRoboVMVersion());

		values.put("%ANDROID_GRADLE_PLUGIN_VERSION%", Versions.getAndroidGradlePluginVersion());
		values.put("%ROBOVM_GRADLE_PLUGIN_VERSION%", Versions.getROBOVMGradlePluginVersion());

		values.put("%BLADE_INK_VERSION%", Versions.getBladeInkVersion());

		copyAndReplace(outputDir, project, values);

		// HACK executable flag isn't preserved for whatever reason...
		new File(outputDir, "gradlew").setExecutable(true);

		RunProccess.runGradle(new File(outputDir), "desktop:clean");
	}

	private void copyAndReplace(String outputDir, ProjectSetup project, Map<String, String> values) throws IOException {
		File out = new File(outputDir);
		if (!out.exists() && !out.mkdirs()) {
			throw new RuntimeException("Couldn't create output directory '" + out.getAbsolutePath() + "'");
		}

		for (ProjectFile file : project.files) {
			copyFile(file, out, values);
		}
	}

	private byte[] readResource(String resource, String path) {
		InputStream in = null;
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024 * 10];
			in = BladeEngineSetup.class.getResourceAsStream(path + resource);
			if (in == null)
				throw new RuntimeException("Couldn't read resource '" + resource + "'");
			int read = 0;
			while ((read = in.read(buffer)) > 0) {
				bytes.write(buffer, 0, read);
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't read resource '" + resource + "'", e);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
	}

	private byte[] readResource(File file) throws IOException {
		InputStream in = null;
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024 * 10];
			in = new FileInputStream(file);
			int read = 0;
			while ((read = in.read(buffer)) > 0) {
				bytes.write(buffer, 0, read);
			}
			return bytes.toByteArray();
		} catch (Exception e) {
			throw new IOException("Couldn't read resource '" + file.getAbsoluteFile() + "'", e);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
	}

	private String readResourceAsString(String resource, String path) {
		try {
			return new String(readResource(resource, path), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String readResourceAsString(File file) throws IOException {
		try {
			return new String(readResource(file), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeFile(File outFile, byte[] bytes) {
		OutputStream out = null;

		try {
			out = new BufferedOutputStream(new FileOutputStream(outFile));
			out.write(bytes);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write file '" + outFile.getAbsolutePath() + "'", e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
				}
		}
	}

	private void writeFile(File outFile, String text) {
		try {
			writeFile(outFile, text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyFile(ProjectFile file, File out, Map<String, String> values) throws IOException {
		File outFile = new File(out, file.outputName);
		if (!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) {
			throw new RuntimeException("Couldn't create dir '" + outFile.getAbsolutePath() + "'");
		}

		boolean isTemp = file instanceof TemporaryProjectFile ? true : false;

		if (file.isTemplate) {
			String txt;
			if (isTemp) {
				txt = readResourceAsString(((TemporaryProjectFile) file).file);
			} else {
				txt = readResourceAsString(file.resourceName, file.resourceLoc);
			}
			txt = replace(txt, values);
			writeFile(outFile, txt);
		} else {
			if (isTemp) {
				writeFile(outFile, readResource(((TemporaryProjectFile) file).file));
			} else {
				writeFile(outFile, readResource(file.resourceName, file.resourceLoc));
			}
		}
	}

	private String replace(String txt, Map<String, String> values) {
		for (String key : values.keySet()) {
			String value = values.get(key);
			txt = txt.replace(key, value);
		}
		return txt;
	}
}
