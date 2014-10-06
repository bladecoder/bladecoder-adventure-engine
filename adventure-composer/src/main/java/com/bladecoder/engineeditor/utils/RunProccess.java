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
package com.bladecoder.engineeditor.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	
	public static boolean runBladeEngine(File prjFolder, String chapter, String scene) throws IOException {
		String args = ":desktop:run -PappArgs=['-w'";
		
		if(chapter != null) {
			args += ",'-chapter','" + chapter + "'";
		}
		
		if(scene != null) {
			args += ",'-t','" + scene + "'";
		}
		
		args += "]";
		
		return runGradle(prjFolder, args);
	}
	
	public static void runAnt(String buildFile, String target, String distDir, String projectDir, Properties props) throws IOException {
		String packageFilesDir = "package-files/";
		
		if(!new File(packageFilesDir).exists()) {
			EditorLogger.error("package-files folder not found. Searching folder for IDE mode.");
			
			packageFilesDir = "src/dist/package-files/";
			if(!new File(packageFilesDir).exists()) {
				EditorLogger.error(new File(packageFilesDir).getAbsolutePath() + " folder not found in IDE mode.");
				return;
			}
		}
		
		
		List<String> args = new ArrayList<String>();
		args.add("-f");
		args.add(packageFilesDir + buildFile);
		args.add("-Dproject=" + projectDir);
		args.add("-Ddist=" + distDir);
		
		StringBuilder sb = new StringBuilder();
		
		for(Object key:props.keySet()) {	
			sb.setLength(0);
			sb.append("-D").append(key).append("=").append(props.get(key));
			args.add(sb.toString());
		}
		
		args.add(target);
		
		List<String> cp = new ArrayList<String>();
//		cp.add(System.getProperty("java.class.path") );
		cp.add(packageFilesDir + "ant.jar");
		cp.add(packageFilesDir + "ant-launcher.jar");
		
		Process p = runJavaProccess(ANT_MAIN_CLASS, cp, args);
		
		try {
			p.waitFor();
			EditorLogger.debug("ANT EXIT VALUE: " + p.exitValue());
			
			if(p.exitValue() == 1) {
				throw new IOException("ERROR IN ANT PROCCESS");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static Process runJavaProccess(String mainClass, List<String> classpathEntries, List<String> args) throws IOException {
		String javaRT = System.getProperty("java.home") + "/bin/java";
		String workingDirectory = ".";
		
		
		List<String> argumentsList = new ArrayList<String>();
		argumentsList.add(javaRT);
		
		if(classpathEntries!=null && classpathEntries.size() > 0) {
			argumentsList.add("-classpath");
			argumentsList.add(getClasspath(classpathEntries));
		}
		
		argumentsList.add(mainClass);
		
		if(args != null)
			argumentsList.addAll(args);

		ProcessBuilder processBuilder = new ProcessBuilder(
				argumentsList.toArray(new String[argumentsList.size()]));
//		processBuilder.redirectErrorStream(true);
		processBuilder.directory(new File(workingDirectory));
		processBuilder.inheritIO();
		
		return processBuilder.start();
	}
	
	public static boolean runGradle(File workingDir, String parameters) {
		String exec = workingDir.getAbsolutePath() + "/" + (System.getProperty("os.name").contains("Windows") ?  "gradlew.bat": "gradlew");
		String command = exec + " " + parameters;
		
		EditorLogger.debug("Executing '" + command + "'");
		
		try {
			final ProcessBuilder pb = new ProcessBuilder(command.split(" ")).directory(workingDir);
			
			// TODO: READ OUTPUT FROM pb AND print in output stream			
			if(System.console() != null)
				pb.inheritIO();
			
			final Process process = pb.start();
			process.waitFor();			
			return process.exitValue() == 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}	
}
