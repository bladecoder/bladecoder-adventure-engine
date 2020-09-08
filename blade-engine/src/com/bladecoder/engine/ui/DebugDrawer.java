package com.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class DebugDrawer {

	private final UI ui;
	private final Viewport viewport;
	private final StringBuilder sbTmp = new StringBuilder();
	private final Vector3 unprojectTmp = new Vector3();
	private final GlyphLayout textLayout = new GlyphLayout();

	public DebugDrawer(UI ui, Viewport viewport) {
		this.ui = ui;
		this.viewport = viewport;
	}

	public void draw(SpriteBatch batch) {
		World w = ui.getWorld();

		w.getSceneCamera().getInputUnProject(viewport, unprojectTmp);

		Color color;

		sbTmp.setLength(0);

		if (EngineLogger.lastError != null) {
			// sbTmp.append(EngineLogger.lastError);
			sbTmp.append(EngineLogger.errorBuffer);

			color = Color.RED;
		} else {

			// sbTmp.append(" Density:");
			// sbTmp.append(Gdx.graphics.getDensity());
			// sbTmp.append(" UI Multiplier:");
			// sbTmp.append(DPIUtils.getSizeMultiplier());
			sbTmp.append(" ");

			long millis = w.getTimeOfGame();
			long second = (millis / 1000) % 60;
			long minute = (millis / (1000 * 60)) % 60;
			long hour = (millis / (1000 * 60 * 60));

			String time = String.format("%02d:%02d:%02d", hour, minute, second);

			sbTmp.append(time);

			if (EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
				if (w.inCutMode()) {
					sbTmp.append(" CUT_MODE ");
				} else if (w.hasDialogOptions()) {
					sbTmp.append(" DIALOG_MODE ");
				} else if (w.isPaused()) {
					sbTmp.append(" PAUSED ");
				}

				sbTmp.append(" ( ");
				sbTmp.append((int) unprojectTmp.x);
				sbTmp.append(", ");
				sbTmp.append((int) unprojectTmp.y);
				sbTmp.append(") FPS:");
				sbTmp.append(Gdx.graphics.getFramesPerSecond());

				if (w.getCurrentScene().getState() != null) {
					sbTmp.append(" Scn State: ");
					sbTmp.append(w.getCurrentScene().getState());
				}

				if (w.getCurrentScene().getPlayer() != null) {
					sbTmp.append(" Depth Scl: ");
					sbTmp.append(w.getCurrentScene().getFakeDepthScale(unprojectTmp.y));
				}
			}

			color = Color.WHITE;
		}

		String strDebug = sbTmp.toString();

		textLayout.setText(ui.getSkin().getFont("debug"), strDebug, color, viewport.getScreenWidth(), Align.left, true);
		RectangleRenderer.draw(batch, 0, viewport.getScreenHeight() - textLayout.height - 10, textLayout.width,
				textLayout.height + 10, Color.BLACK);
		ui.getSkin().getFont("debug").draw(batch, textLayout, 0, viewport.getScreenHeight() - 5);

		// Draw actor states when debug
		if (EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {

			for (BaseActor a : w.getCurrentScene().getActors().values()) {

				if (a instanceof AnchorActor)
					continue;

				Rectangle r = a.getBBox().getBoundingRectangle();
				sbTmp.setLength(0);
				sbTmp.append(a.getId());
				if (a instanceof InteractiveActor && ((InteractiveActor) a).getState() != null)
					sbTmp.append(".").append(((InteractiveActor) a).getState());

				unprojectTmp.set(r.getX(), r.getY(), 0);
				w.getSceneCamera().scene2screen(viewport, unprojectTmp);
				ui.getSkin().getFont("debug").draw(batch, sbTmp.toString(), unprojectTmp.x, unprojectTmp.y);
			}

		}
	}
}
