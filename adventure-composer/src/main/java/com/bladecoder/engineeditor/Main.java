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
package com.bladecoder.engineeditor;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bladecoder.engine.model.TrackPropertyChanges;
import com.bladecoder.engineeditor.model.PropertyChange;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;

public class Main extends LwjglApplication {
	public static void main(final String[] args) {
		ByteBuddyAgent.installOnOpenJDK();

		new AgentBuilder.Default()
				.rebase(ElementMatchers.nameStartsWith("com.bladecoder.engine"))
				.transform((builder, typeDescription) -> builder
								.method(ElementMatchers.isPublic().and(ElementMatchers.isAnnotatedWith(TrackPropertyChanges.class)))
								.intercept(
										MethodDelegation
												.to(TrackPropertyChangesInterceptor.class)
								)
								.defineField(TrackPropertyChangesInterceptor.PROPERTY_CHANGE_FIELD, PropertyChange.class)
								.defineField(TrackPropertyChangesInterceptor.MODIFIED_FIELD, boolean.class)
				)
				.installOnByteBuddyAgent();

		// This dummy instantiation will initialize JavaFX for us
		new JFXPanel();

		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = "Adventure Composer";
		Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
		cfg.width = (int) (bounds.getWidth() - bounds.getWidth() * 0.1);
		cfg.height = (int) (bounds.getHeight() - bounds.getHeight() * 0.1);

		cfg.resizable = true;
		// cfg.samples = 2;
		// cfg.useGL30 = true;

		if (Main.class.getResource("/images/ic_app64.png") != null)
			cfg.addIcon("images/ic_app64.png", FileType.Internal);

		if (Main.class.getResource("/images/ic_app32.png") != null)
			cfg.addIcon("images/ic_app32.png", FileType.Internal);

		if (Main.class.getResource("/images/ic_app16.png") != null)
			cfg.addIcon("images/ic_app16.png", FileType.Internal);

		parseArgs(args);

		new Main(new Editor(), cfg);
	}

	private static void parseArgs(String[] args) {
		for (int i = 1; i < args.length; i++) {
			if (args[i - 1].equals("-f")) {
				try {
					File file = new File(args[i]).getCanonicalFile();
					Ctx.project.loadProject(file);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public Main(Editor editor, LwjglApplicationConfiguration cfg) {
		super(editor,cfg);
	}
	
	@Override
	public void exit() {
		((Editor) listener).exit();
	}
	
	public void exitSaved() {
		super.exit();
	}
}
