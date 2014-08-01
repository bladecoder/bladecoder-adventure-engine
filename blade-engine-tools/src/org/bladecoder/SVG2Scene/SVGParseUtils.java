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
package org.bladecoder.SVG2Scene;

import java.awt.geom.Point2D;
import java.util.ArrayList;


/**
 * 
 * Simple Path parser 
 * 
 * Only retrieves a list of vertices
 * 
 * @author rgarcia
 */
public class SVGParseUtils {
	public static ArrayList<Point2D.Float> parsePathD(String s) {
		ArrayList<Point2D.Float> vertices = new ArrayList<Point2D.Float>();
		
		String []elist = s.split(" "); // Point2D.Float and command list
		
		for(String e:elist) {
			if(e.length() > 1) { // a Point2D.Float detected, the commands are ignored
				String[] pstr = e.split(",");
				Point2D.Float p = new Point2D.Float();
				
				p.x = Float.parseFloat(pstr[0]);
				p.y = Float.parseFloat(pstr[1]);
				
				vertices.add(p);
			}
		}
		
		return vertices;
	}
	
	/**
	 * Parse transform tag and apply transform to points. Only applies translate transform.
	 * 
	 * @param Point2D.Floats
	 * @param s
	 */
	public static void parsePathTransform(ArrayList<Point2D.Float> points, String s) {
		Point2D.Float t = parseTransform(s);
		if(s==null) return;
		
		for(Point2D.Float p:points) {
			p.x+=t.x;
			p.y+=t.y;
		}
		
	}
	
	/**
	 * Parse transform tag. Only gets translate transform.
	 * 
	 * @param Point2D.Floats
	 * @param s
	 */
	public static Point2D.Float parseTransform(String s) {
		
		if(s==null || !s.startsWith("translate(")) return null;
		
		Point2D.Float t = new Point2D.Float();
		
		String s2 = s.substring(s.indexOf('(') + 1, s.length()-2);
		
		String[] pstr = s2.split(",");
		
		t.x = Float.parseFloat(pstr[0]);
		t.y = Float.parseFloat(pstr[1]);
		
		return t;
	}	
}
