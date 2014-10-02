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
package com.bladecoder.engine.shading;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class CelShader extends BaseShader  {

	protected final int u_projTrans = register(new Uniform("u_projTrans"));
	protected final int u_worldTrans = register(new Uniform("u_worldTrans"));
	protected final int u_test = register(new Uniform("u_test"));

	protected final ShaderProgram program;
	
	public static class TestAttribute extends Attribute {
		public final static String Alias = "Test";
		public final static long ID = register(Alias);

		public float value;

		protected TestAttribute(final float value) {
			super(ID);
			this.value = value;
		}

		@Override
		public Attribute copy() {
			return new TestAttribute(value);
		}

		@Override
		protected boolean equals(Attribute other) {
			return ((TestAttribute) other).value == value;
		}
	}	

	public CelShader() {
		super();
		program = new ShaderProgram(Gdx.files.classpath("org/bladecoder/engine/shading/cel.vertex.glsl"), 
				Gdx.files.classpath("org/bladecoder/engine/shading/cel.fragment.glsl"));
		if (!program.isCompiled())
			throw new GdxRuntimeException("Couldn't compile shader "
					+ program.getLog());
	}

	@Override
	public void init() {
		super.init(program, null);
	}

	@Override
	public int compareTo(Shader other) {
		return 0;
	}

	@Override
	public boolean canRender(Renderable instance) {
		return true;
	}

	@Override
	public void begin(Camera camera, RenderContext context) {
		program.begin();
		set(u_projTrans, camera.combined);
	}

	@Override
	public void render(Renderable renderable) {
		set(u_worldTrans, renderable.worldTransform);
		TestAttribute attr = (TestAttribute) renderable.material
				.get(TestAttribute.ID);
		set(u_test, attr == null ? 1f : attr.value);
		renderable.mesh.render(program, renderable.primitiveType,
				renderable.meshPartOffset, renderable.meshPartSize);
	}

	@Override
	public void end() {
		program.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		program.dispose();
	}
}
