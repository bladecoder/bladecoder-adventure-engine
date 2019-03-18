/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
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
package com.bladecoder.engineeditor.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Run Java Process in a new VM
 * 
 * @author rgarcia
 */
public class RunProccess {
	private static final String ANT_MAIN_CLASS = "org.apache.tools.ant.launch.Launcher";

	private static String getClasspath(List<String> classpathEntries) {
		StringBuilder builder = new StringBuilder();
		int count = 0;
		final int totalSize = classpathEntries.size();
		for (String classpathEntry : classpathEntries) {
			builder.append(classpathEntry);
			count++;
			if (count < totalSize) {
				builder.append(System.getProperty("path.separator"));
			}
		}
		return builder.toString();
	}

	public static boolean runBladeEngine(File prjFolder, String chapter, String scene, boolean fullscreen)
			throws IOException {
		List<String> args = new ArrayList<>();
		args.add(":desktop:run");
		String appArgs = "-PappArgs=['-d'";

		if (!fullscreen)
			appArgs += ",'-w'";

		if (chapter != null) {
			appArgs += ",'-chapter','" + chapter + "'";
		}

		if (scene != null) {
			appArgs += ",'-t','" + scene + "'";
		}

		appArgs += "]";
		args.add(appArgs);

		return runGradle(prjFolder, args);
	}

	public static boolean runBladeEngineInternal(File prjFolder, String chapter, String scene) throws IOException {
		List<String> args = new ArrayList<>();
		args.add("-w");
		args.add("-adv-dir");
		args.add(prjFolder.getAbsolutePath());

		if (scene != null) {
			args.add("-t");
			args.add(scene);
		}

		if (chapter != null) {
			args.add("-chapter");
			args.add(chapter);
		}

		List<String> cp = new ArrayList<>();
		cp.add(System.getProperty("java.class.path"));

		runJavaProccess("com.bladecoder.engineeditor.utils.DesktopLauncher", cp, args);

		return true;
	}

	public static void runAnt(String buildFile, String target, String distDir, String projectDir, Properties props)
			throws IOException {
		String packageFilesDir = "package-files/";

		if (!new File(packageFilesDir).exists()) {
			EditorLogger.error("package-files folder not found. Searching folder for IDE mode.");

			packageFilesDir = "src/dist/package-files/";
			if (!new File(packageFilesDir).exists()) {
				EditorLogger.error(new File(packageFilesDir).getAbsolutePath() + " folder not found in IDE mode.");
				return;
			}
		}

		List<String> args = new ArrayList<>();
		args.add("-f");
		args.add(packageFilesDir + buildFile);
		args.add("-Dproject=" + projectDir);
		args.add("-Ddist=" + distDir);

		StringBuilder sb = new StringBuilder();

		for (Object key : props.keySet()) {
			sb.setLength(0);
			sb.append("-D").append(key).append("=").append(props.get(key));
			args.add(sb.toString());
		}

		args.add(target);

		List<String> cp = new ArrayList<>();
		// cp.add(System.getProperty("java.class.path") );
		cp.add(packageFilesDir + "ant.jar");
		cp.add(packageFilesDir + "ant-launcher.jar");

		Process p = runJavaProccess(ANT_MAIN_CLASS, cp, args);

		try {
			p.waitFor();
			EditorLogger.debug("ANT EXIT VALUE: " + p.exitValue());

			if (p.exitValue() == 1) {
				throw new IOException("ERROR IN ANT PROCCESS");
			}
		} catch (InterruptedException e) {
			EditorLogger.printStackTrace(e);
		}
	}

	public static Process runJavaProccess(String mainClass, List<String> classpathEntries, List<String> args)
			throws IOException {
		String javaRT = System.getProperty("java.home") + "/bin/java";
		String workingDirectory = ".";

		List<String> argumentsList = new ArrayList<>();
		argumentsList.add(javaRT);

		if (classpathEntries != null && classpathEntries.size() > 0) {
			argumentsList.add("-classpath");
			argumentsList.add(getClasspath(classpathEntries));
		}

		argumentsList.add(mainClass);

		if (args != null)
			argumentsList.addAll(args);

		ProcessBuilder processBuilder = new ProcessBuilder(argumentsList.toArray(new String[argumentsList.size()]));
		// processBuilder.redirectErrorStream(true);
		processBuilder.directory(new File(workingDirectory));
		processBuilder.inheritIO();

		return processBuilder.start();
	}

	public static boolean runGradle(File workingDir, List<String> parameters) {
		String exec = workingDir.getAbsolutePath() + "/"
				+ (System.getProperty("os.name").contains("Windows") ? "gradlew.bat" : "gradlew");

		List<String> argumentsList = new ArrayList<>();
		argumentsList.add(exec);
		argumentsList.addAll(parameters);

		EditorLogger.msgThreaded("Executing 'gradlew " + parameters + "'");

		try {
			final ProcessBuilder pb = new ProcessBuilder(argumentsList).directory(workingDir).redirectErrorStream(true);

			// TODO: READ OUTPUT FROM pb AND print in output stream
			// if (System.console() != null)
			// pb.inheritIO();

			final Process process = pb.start();

			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				EditorLogger.msgThreaded(line);
			}

			process.waitFor();
			return process.exitValue() == 0;
		} catch (Exception e) {
			EditorLogger.msgThreaded("ERROR: " + e.getMessage());
			return false;
		}
	}

	public static boolean runInklecate(File workingDir, List<String> parameters) {
		String exec = workingDir.getAbsolutePath() + "/" + "inklecate.exe";

		List<String> argumentsList = new ArrayList<>();
		argumentsList.add(exec);
		argumentsList.addAll(parameters);

		EditorLogger.msgThreaded("Executing 'inklecate " + parameters + "'");

		try {
			final ProcessBuilder pb = new ProcessBuilder(argumentsList).directory(workingDir).redirectErrorStream(true);

			// TODO: READ OUTPUT FROM pb AND print in output stream
			// if (System.console() != null)
			// pb.inheritIO();

			final Process process = pb.start();

			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				EditorLogger.msgThreaded(line);
			}

			process.waitFor();
			return process.exitValue() == 0;
		} catch (Exception e) {
			EditorLogger.msgThreaded("ERROR: " + e.getMessage());
			return false;
		}
	}

	public static boolean runGradle(File workingDir, String parameters) {

		String[] split = parameters.split(" ");

		return runGradle(workingDir, Arrays.asList(split));
	}
}
