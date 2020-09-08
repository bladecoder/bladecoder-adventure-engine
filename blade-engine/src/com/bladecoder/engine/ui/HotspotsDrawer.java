package com.bladecoder.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.UIUtils;

public class HotspotsDrawer {

	private final UI ui;
	private final Viewport viewport;

	private final GlyphLayout textLayout = new GlyphLayout();
	private final Vector3 unprojectTmp = new Vector3();

	public HotspotsDrawer(UI ui, Viewport viewport) {
		this.ui = ui;
		this.viewport = viewport;
	}

	public void draw(SpriteBatch batch, boolean showDesc) {
		World w = ui.getWorld();

		for (BaseActor a : w.getCurrentScene().getActors().values()) {
			if (!(a instanceof InteractiveActor) || !a.isVisible() || a == w.getCurrentScene().getPlayer())
				continue;

			InteractiveActor ia = (InteractiveActor) a;

			if (!ia.canInteract())
				continue;

			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING HOTSPOT FOR: " + a.getId());
			}

			Rectangle r = a.getBBox().getBoundingRectangle();

			unprojectTmp.set(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 0);
			w.getSceneCamera().scene2screen(viewport, unprojectTmp);

			if (!showDesc || ia.getDesc() == null) {

				float size = DPIUtils.ICON_SIZE * DPIUtils.getSizeMultiplier();

				if (ia.getVerb(Verb.LEAVE_VERB) != null) {
					TransformDrawable drawable = (TransformDrawable) ui.getSkin().getDrawable(Verb.LEAVE_VERB);

					drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size / 2, size / 2, size,
							size, 1.0f, 1.0f, UIUtils.calcLeaveArrowRotation(viewport, ia));
				} else {
					Drawable drawable = ui.getSkin().getDrawable("hotspot");

					if (drawable != null)
						drawable.draw(batch, unprojectTmp.x - size / 2, unprojectTmp.y - size / 2, size, size);
				}
			} else {
				BitmapFont font = ui.getSkin().getFont("desc");
				String desc = ia.getDesc();
				if (desc.charAt(0) == I18N.PREFIX)
					desc = w.getI18N().getString(desc.substring(1));

				textLayout.setText(font, desc);

				float textX = unprojectTmp.x - textLayout.width / 2;
				float textY = unprojectTmp.y + textLayout.height;

				RectangleRenderer.draw(batch, textX - 8, textY - textLayout.height - 8, textLayout.width + 16,
						textLayout.height + 16, Color.BLACK);
				font.draw(batch, textLayout, textX, textY);
			}
		}
	}
}
