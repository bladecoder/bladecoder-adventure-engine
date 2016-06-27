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
package com.bladecoder.engine.common;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public final class Utils3D {
	
	final static float GRID_MIN = -10f;
	final static float GRID_MAX = 10f;
	final static float GRID_STEP = 1f;
	
	private static Model axesModel;
	private static ModelInstance axesInstance;
	
	private static Model floorModel;
	private static ModelInstance floorInstance;
	
	public static void dispose() {
		if(axesModel !=null ) {
			axesModel.dispose();
		}
		
		axesModel = null;

		
		if(floorModel !=null ) {
			floorModel.dispose();
		}
		
		floorModel = null;		
	}
	

	
	public static void createFloor() {

		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder mpb = modelBuilder.part("parts", GL20.GL_TRIANGLES,
				Usage.Position | Usage.Normal | Usage.ColorUnpacked, new Material(
						ColorAttribute.createDiffuse(Color.WHITE)));
		mpb.setColor(1f, 1f, 1f, 1f);
//		mpb.box(0, -0.1f, 0, 10, .2f, 10);
		mpb.rect(-10, 0, -10, 
				-10, 0, 10,
				10, 0, 10,
				10, 0, -10, 0, 1, 0);
		floorModel = modelBuilder.end();
		floorInstance = new ModelInstance(floorModel);
		
		// TODO Set only when FBO is active
		floorInstance.materials.get(0).set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
	}

	
	private static void createAxes() {
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder builder = modelBuilder.part("grid", GL20.GL_LINES, Usage.Position | Usage.ColorUnpacked, new Material());
		builder.setColor(Color.LIGHT_GRAY);
		for (float t = GRID_MIN; t <= GRID_MAX; t+=GRID_STEP) {
			builder.line(t, 0, GRID_MIN, t, 0, GRID_MAX);
			builder.line(GRID_MIN, 0, t, GRID_MAX, 0, t);
		}
		builder = modelBuilder.part("axes", GL20.GL_LINES, Usage.Position | Usage.ColorUnpacked, new Material());
		builder.setColor(Color.RED);
		builder.line(0, 0, 0, 10, 0, 0);
		builder.setColor(Color.GREEN);
		builder.line(0, 0, 0, 0, 10, 0);
		builder.setColor(Color.BLUE);
		builder.line(0, 0, 0, 0, 0, 10);
		axesModel = modelBuilder.end();
		axesInstance = new ModelInstance(axesModel);
	}	

	public static ModelInstance getFloor() {
		if(floorModel == null) {
			createFloor();
		}
		
		return floorInstance;
	}
	
	public static ModelInstance getAxes() {
		if(axesModel == null) {
			createAxes();
		}
		
		return axesInstance;		
	}
}
