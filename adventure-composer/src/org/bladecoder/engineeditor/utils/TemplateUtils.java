package org.bladecoder.engineeditor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TemplateUtils {
	/**
	 * Copy a ORG file in the destination file replacing all the vars.
	 * 
	 * Vars are indicated in the ORG file like this: ${var1}
	 * 
	 * @param tmpl Template (Org file)
	 * @param dest Destination file. It can be the ORG file for overwriting.
	 * @param vars Variables to replace in the template
	 * @throws IOException 
	 */
	public static void copyTemplate(File tmpl, File dest, HashMap<String,String> vars) throws IOException {
		  // we need to store all the lines
	    List<String> lines = new ArrayList<String>();

	    // first, read the file and store the changes
	    BufferedReader in = new BufferedReader(new FileReader(tmpl));
	    String line = in.readLine();
	    while (line != null) {
	       
	    	for(String var:vars.keySet()) {
	    		String orgStr = "\\$\\{" + var +"\\}";
	    		String destStr = vars.get(var);
	    	
	    		line.replaceAll(orgStr, destStr);
	    	}
	    	
	    	lines.add(line);
	        line = in.readLine();
	    }
	    in.close();

	    // now, write the file again with the changes
	    PrintWriter out = new PrintWriter(dest);
	    for (String l : lines)
	        out.println(l);
	    out.close();
	}
}
