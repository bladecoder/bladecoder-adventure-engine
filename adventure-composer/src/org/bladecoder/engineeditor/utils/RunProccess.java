package org.bladecoder.engineeditor.utils;

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

	private static final String LAUNCHER_MAIN_CLASS = "org.bladecoder.engine.desktop.Main";
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
	
	public static void runBladeEngine(String prjFolder, String scene) throws IOException {
		List<String> args = new ArrayList<String>();
		args.add("-w");
		args.add("-adv-dir");
		args.add(prjFolder);
		
		if(scene != null) {
			args.add("-t");
			args.add(scene);
		} else {
			args.add("-r");			
		}
		
		List<String> cp = new ArrayList<String>();
		cp.add(System.getProperty("java.class.path") );
		cp.add("./package-files/engine/engine-desktop.jar");
		
		start(LAUNCHER_MAIN_CLASS, cp, args);
	}
	
	public static void runAnt(String buildFile, String target, String distDir, String projectDir, Properties props) throws IOException {
		
		List<String> args = new ArrayList<String>();
		args.add("-f");
		args.add("package-files/" + buildFile);
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
		cp.add(System.getProperty("java.class.path") );
		cp.add("package-files/ant.jar");
		cp.add("package-files/ant-launcher.jar");
		
		Process p = start(ANT_MAIN_CLASS, cp, args);
		
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

	public static Process start(String mainClass, List<String> classpathEntries, List<String> args) throws IOException {
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
}
