package com.bladecoder.engineeditor.setup;

/**
 * A file in a {@link ProjectSetup}, the resourceName specifies the location
 * of the template file, the outputName specifies the final name of the
 * file relative to its project, the isTemplate field specifies if 
 * values need to be replaced in this file or not.
 * @author badlogic
 *
 */
public class ProjectFile {
	/** the name of the template resource, relative to resourceLoc **/
	public String resourceName;
	/** the name of the output file, including directories, relative to the project dir **/
	public String outputName;
	/** whether to replace values in this file **/
	public boolean isTemplate;
	/** If the resource is from resource directory, or working dir **/
	public String resourceLoc = "/projectTmpl/";
	
	public ProjectFile(String name) {
		this.resourceName = name;
		this.outputName = name;
		this.isTemplate = true;
	}
	
	public ProjectFile(String name, boolean isTemplate) {
		this.resourceName = name;
		this.outputName = name;
		this.isTemplate = isTemplate;
	}
	
	public ProjectFile(String resourceName, String outputName, boolean isTemplate) {
		this.resourceName = resourceName;
		this.outputName = outputName;
		this.isTemplate = isTemplate;		
	}
}