package com.bladecoder.engineeditor.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

public class FolderClassLoader extends ClassLoader {

	private String dirName = null; // Path to the .class folder
	private Hashtable<String, Class<?>> classes = new Hashtable<String, Class<?>>(); // used
																						// to
																						// cache
																						// already

	// defined classes
	public FolderClassLoader(String dir) throws IOException {
		super(FolderClassLoader.class.getClassLoader()); // calls the parent
															// class
															// loader's
															// constructor

		this.dirName = dir;

		loadClassesInFolder(new File(dirName), null);
	}

	public void reload() throws IOException {
		loadClassesInFolder(new File(dirName), null);
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		return findClass(className);
	}

	@Override
	public Class<?> findClass(String className) {
		Class<?> result = null;

		result = (Class<?>) classes.get(className); // checks in cached classes
		if (result != null) {
			return result;
		}

		try {
			return findSystemClass(className);
		} catch (Exception e) {
			return null;
		}
	}

	private void loadClassesInFolder(File folder, String classPackage) throws IOException, NoClassDefFoundError {
		byte classByte[];

		File[] list = folder.listFiles();

		if (list != null) {

			for (File f : list) {
				if (f.isDirectory()) {

					String pkg = classPackage;

					if (pkg != null) {
						pkg += ".";
						pkg += f.getName();
					} else {
						pkg = f.getName();
					}

					loadClassesInFolder(f, pkg);
				} else if (f.getName().endsWith(".class")) {
					InputStream is = null;

					try {

						is = new FileInputStream(f);
						ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
						int nextValue = is.read();
						while (-1 != nextValue) {
							byteStream.write(nextValue);
							nextValue = is.read();
						}

						classByte = byteStream.toByteArray();
						String className = classPackage + "." + f.getName().substring(0, f.getName().length() - 6);
						EditorLogger.debug(">>>>>>>>>> LOADING CLASS: " + className);

						if (classes.get(className) == null) {

							Class<?> result = defineClass(className, classByte, 0, classByte.length, null);

							classes.put(className, result);
						}
					} catch (NoClassDefFoundError e) {
						EditorLogger.error("ERROR - Could not load class: " + e.getMessage());
					} catch (IOException e) {						
						throw e;
					} finally {
						is.close();
					}
				}
			}
		}
	}

	public Hashtable<String, Class<?>> getClasses() {
		return classes;
	}
}
