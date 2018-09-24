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
package com.bladecoder.engine.ui;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

/**
 * Singleton class for recording verbs calls. This class can record and play a
 * game session.
 * 
 * @author rgarcia
 */
public class Recorder {
	private static final String DEFAULT_RECORD_FILENAME = "record";
	public static final String RECORD_EXT = ".verbs.rec";
	public static final String GAMESTATE_REC_EXT = ".gamestate.rec";
	private static final float WAITING_TIME = .5f;

	private ArrayList<TimeVerb> list = new ArrayList<>();
	private boolean playing = false;
	private boolean recording = false;
	private float time;
	private int pos;
	private String fileName = DEFAULT_RECORD_FILENAME;
	private final World w;

	public Recorder(World w) {
		this.w = w;
	}

	public void update(float delta) {

		if (w.isPaused())
			return;

		if (recording) {
			time += delta;
		} else if (playing) {
			time += delta;

			if (pos >= list.size()) {
				setPlaying(false);
				return;
			}

			TimeVerb v = list.get(pos);

			Scene s = w.getCurrentScene();

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("RECORDER - ");

			// while (playing && v.time < time) {
			if (playing && v.time < time && !w.inCutMode()) {

				if (v.verb == null) {
					if (v.pos == null) { // DIALOG OPTION
						w.selectDialogOption(v.dialogOption);

						stringBuilder.append(" SELECT DIALOG OPTION: ").append(v.dialogOption);
					} else { // GOTO
						s.getPlayer().goTo(v.pos, null, false);

						stringBuilder.append(" GOTO ").append(v.pos.x).append(',').append(v.pos.y);
					}
				} else {

					stringBuilder.append(v.verb);

					if (v.verb.equals("SAVEGAME")) {
						// SPECIAL VERB TO SAVE THE GAME
						stringBuilder.append(v.target);
						try {
							w.getSerializer().saveGameState(v.target);
						} catch (IOException e) {
							EngineLogger.error("Couldn't save game: " + v.target + " : " + e.getMessage());
						}
					} else {

						InteractiveActor a = (InteractiveActor) s.getActor(v.actorId, true);

						if (a != null) {
							stringBuilder.append(' ').append(v.actorId);

							if (v.target != null) {
								stringBuilder.append(" with ").append(v.target);
							}

							a.runVerb(v.verb, v.target);
						} else
							EngineLogger.error("PLAYING ERROR: BaseActor not found: " + v.actorId);
					}
				}

				EngineLogger.debug(stringBuilder.toString());

				time = 0;
				pos++;
				if (pos >= list.size()) {
					setPlaying(false);
				} else {
					v = list.get(pos);
				}
			}
		}
	}

	public void add(String actorId, String verb, String target) {
		if (recording) {

			time += WAITING_TIME;
			TimeVerb v = new TimeVerb();
			v.time = time;
			v.verb = verb;
			v.target = target;
			v.actorId = actorId;

			list.add(v);
			time = 0;
		}
	}

	public void add(int dialogOption) {
		if (recording) {
			TimeVerb v = new TimeVerb();
			time += WAITING_TIME;
			v.time = time;

			if (w.hasDialogOptions()) {
				v.dialogOption = dialogOption;
			}

			list.add(v);
			time = 0;
		}
	}

	public void add(Vector2 pos) {
		if (recording) {
			TimeVerb v = new TimeVerb();
			time += WAITING_TIME;
			v.time = time;

			v.pos = pos;

			list.add(v);
			time = 0;
		}
	}

	public boolean isRecording() {
		return recording;
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
		time = 0;
		pos = 0;

		if (recording) {
			EngineLogger.debug("RECORDING...");
			try {
				w.getSerializer().saveGameState(fileName + GAMESTATE_REC_EXT);
			} catch (IOException e) {
				EngineLogger.error(e.getMessage());
			}
		} else
			save();
	}

	public boolean isPlaying() {
		return playing;
	}

	public void setPlaying(boolean p) {
		if (p)
			EngineLogger.debug("PLAYING...");
		else
			EngineLogger.debug("STOP PLAYING...");

		this.playing = p;
		this.recording = false;
		time = 0;
		pos = 0;
	}

	public void draw(SpriteBatch batch) {
		if (recording && ((int) time) % 2 == 0) {
			// RectangleRenderer.draw(batch, 10,
			// w.getCamera().getViewport().commandHeight - 30,
			// 20, 20, Color.RED);

			// TODO: Provisional. Pass viewport to get height
			RectangleRenderer.draw(batch, 10, 10, 20, 20, Color.RED);
		}
	}

	static class TimeVerb {
		float time;
		String verb;
		String target;
		String actorId;
		int dialogOption;
		Vector2 pos;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFilename(String name) {
		if (name != null && !name.trim().isEmpty()) {
			fileName = name;
		} else {
			fileName = DEFAULT_RECORD_FILENAME;
		}
	}

	@SuppressWarnings("unchecked")
	public void load() {
		String gameStateFileName = fileName + GAMESTATE_REC_EXT;
		String recordFileName = fileName + RECORD_EXT;

		FileHandle verbsFile = EngineAssetManager.getInstance().getUserFile(recordFileName);

		if (!verbsFile.exists())
			verbsFile = EngineAssetManager.getInstance().getAsset("tests/" + recordFileName);

		if (verbsFile.exists()) {
			// LOAD GAME STATE IF EXISTS
			FileHandle gameStateFile = EngineAssetManager.getInstance().getUserFile(gameStateFileName);

			if (!gameStateFile.exists())
				gameStateFile = EngineAssetManager.getInstance().getAsset("tests/" + gameStateFileName);

			if (gameStateFile.exists())
				try {
					w.getSerializer().loadGameState(gameStateFile);
				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
			else
				EngineLogger.debug("LOADING RECORD: no saved file exists");

			// LOAD VERBS
			list = new Json().fromJson(ArrayList.class, TimeVerb.class, verbsFile.reader("UTF-8"));
		} else {
			EngineLogger.error("LOADING RECORD: no record file exists");
		}
	}

	public void save() {

		Json json = new Json();

		// String s = json.prettyPrint(list);
		String s = json.toJson(list, ArrayList.class, TimeVerb.class);
		s = json.prettyPrint(s);

		Writer w = EngineAssetManager.getInstance().getUserFile(fileName + RECORD_EXT).writer(false, "UTF-8");

		try {
			w.write(s);
			w.close();
		} catch (IOException e) {
			EngineLogger.error("ERROR SAVING RECORD", e);
		}
	}
}
