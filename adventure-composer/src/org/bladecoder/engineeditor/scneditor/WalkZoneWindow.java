package org.bladecoder.engineeditor.scneditor;

import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class WalkZoneWindow extends Container {
	private static final String DELETE_WALK_ZONE = "Delete Walk Zone";
	private static final String CREATE_WALK_ZONE = "Create Walk Zone";
	private static final float OBSTACLE_WIDTH = 200;

	TextButton createZoneBtn;
	TextButton createObstacleBtn;
	TextButton deleteObstacleBtn;
	TextButton addObstacleActorBtn;
	TextButton deleteObstacleActorBtn;
	Scene scn;

	private final ScnWidgetInputListener scnIL;

	public WalkZoneWindow(Skin skin, ScnWidgetInputListener sIL) {
		this.scnIL = sIL;

		Table table = new Table(skin);
		createZoneBtn = new TextButton(null, skin);
		createObstacleBtn = new TextButton("Create Obstacle", skin);
		deleteObstacleBtn = new TextButton("Delete Obstacle", skin);
		addObstacleActorBtn = new TextButton("Set Actor as Obstacle", skin);
		deleteObstacleActorBtn = new TextButton("Remove Actor as Obstacle", skin);

		createZoneBtn.setDisabled(true);
		createObstacleBtn.setDisabled(true);
		deleteObstacleBtn.setDisabled(true);

		table.top();
		table.add(new Label("Walk Zone", skin, "big")).center();

		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();
		table.add(createZoneBtn).expandX().fill();
		table.row();
		table.add(createObstacleBtn).expandX().fill();
		table.row();
		table.add(deleteObstacleBtn).expandX().fill();
		setWidget(table);

		createZoneBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				if (scn.getPolygonalNavGraph() == null) {
					float[] verts = new float[8];

					float width = scn.getCamera().getScrollingWidth(); 
					float height = scn.getCamera().getScrollingHeight();
					
					verts[3] = height;
					verts[4] = width;
					verts[5] = height;
					verts[6] = width;

					Polygon poly = new Polygon(verts);
					PolygonalNavGraph pf = new PolygonalNavGraph();
					pf.setWalkZone(poly);
					scn.setPolygonalNavGraph(pf);
					createZoneBtn.setText(DELETE_WALK_ZONE);
					createObstacleBtn.setDisabled(false);
					Ctx.project.getSelectedChapter().createWalkZone(
							Ctx.project.getSelectedScene(), poly);
				} else {
					createZoneBtn.setText(CREATE_WALK_ZONE);
					createObstacleBtn.setDisabled(true);
					scn.setPolygonalNavGraph(null);
					Ctx.project.getSelectedChapter().deleteWalkZone(
							Ctx.project.getSelectedScene());
				}

				event.cancel();
			}

		});

		createObstacleBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float[] verts = new float[8];

				verts[3] = OBSTACLE_WIDTH;
				verts[4] = OBSTACLE_WIDTH;
				verts[5] = OBSTACLE_WIDTH;
				verts[6] = OBSTACLE_WIDTH;

				Polygon poly = new Polygon(verts);
				PolygonalNavGraph pf = scn.getPolygonalNavGraph();
				pf.addObstacle(poly);
				Ctx.project.getSelectedChapter().createObstacle(
						Ctx.project.getSelectedScene(), poly);
				deleteObstacleBtn.setDisabled(false);

				event.cancel();
			}
		});

		deleteObstacleBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				PolygonalNavGraph pf = scn.getPolygonalNavGraph();
				
				if(pf.getObstacles().size() > 0)
					scnIL.setDeleteObstacle(true);
				else 
					Ctx.msg.show(getStage(), "There are no obstacles to delete", 3);

				// if(pf.getObstacles().size() == 0)
				// deleteObstacleBtn.setDisabled(true);

				event.cancel();
			}
		});

		prefSize(200, 200);
		setSize(200, 200);
	}

	public void setScene(Scene scn) {
		this.scn = scn;

		if (scn == null) {
			createZoneBtn.setDisabled(true);
			createObstacleBtn.setDisabled(true);
			deleteObstacleBtn.setDisabled(true);
			return;
		}

		createZoneBtn.setDisabled(false);

		if (scn.getPolygonalNavGraph() == null) {
			createZoneBtn.setText(CREATE_WALK_ZONE);
			createObstacleBtn.setDisabled(true);
		} else {
			createZoneBtn.setText(DELETE_WALK_ZONE);
			createObstacleBtn.setDisabled(false);
		}

		if (scn.getPolygonalNavGraph() != null
				&& scn.getPolygonalNavGraph().getObstacles().size() > 0) {
			deleteObstacleBtn.setDisabled(false);
		} else {
			deleteObstacleBtn.setDisabled(true);
		}
	}

}
