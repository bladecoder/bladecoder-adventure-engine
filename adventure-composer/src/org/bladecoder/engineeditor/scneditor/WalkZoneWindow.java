package org.bladecoder.engineeditor.scneditor;

import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.polygonalpathfinder.PolygonalPathFinder;
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
	
	TextButton createZoneBtn;
	TextButton createObstacleBtn;
	Scene scn;
	
	public WalkZoneWindow(Skin skin) {
		Table table = new Table(skin);
		createZoneBtn = new TextButton(null, skin);
		createObstacleBtn = new TextButton("Create Obstacle", skin);
		
		createZoneBtn.setDisabled(true);
		createObstacleBtn.setDisabled(true);
		
		table.top();
		table.add(new Label("Walk Zone", skin, "big")).center();
		
		Drawable drawable = skin.getDrawable("trans");
		setBackground(drawable);
		table.row();
		table.add(createZoneBtn).expandX().fill();
		table.row();
		table.add(createObstacleBtn).expandX().fill();
		setWidget(table);
		
		createZoneBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				
				if(scn.getPolygonalPathFinder() == null) {
					float[] verts = new float[8];
					
					verts[3] = scn.getBBox().height;
					verts[4] = scn.getBBox().width;
					verts[5] = scn.getBBox().height;
					verts[6] = scn.getBBox().width;
					
					Polygon poly = new Polygon(verts); 
					PolygonalPathFinder pf = new PolygonalPathFinder();
					pf.setWalkZone(poly);
					scn.setPolygonalPathFinder(pf);
					createZoneBtn.setText(DELETE_WALK_ZONE);
					Ctx.project.getSelectedChapter().createWalkZone(Ctx.project.getSelectedScene(), poly);
				} else {
					createZoneBtn.setText(CREATE_WALK_ZONE);
					scn.setPolygonalPathFinder(null);
					Ctx.project.getSelectedChapter().deleteWalkZone(Ctx.project.getSelectedScene());
				}
				
				event.cancel();
			}
			
		});
		
		createObstacleBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				
			}			
		});
		
		
		prefSize(200, 200);
		setSize(200, 200);
	}
	
	public void setScene(Scene scn) {
		this.scn = scn;
		
		if(scn == null) {
			createZoneBtn.setDisabled(true);
			createObstacleBtn.setDisabled(true);
			return;
		}
		
		createZoneBtn.setDisabled(false);
		
		if(scn.getPolygonalPathFinder() == null) {
			createZoneBtn.setText(CREATE_WALK_ZONE);
			createObstacleBtn.setDisabled(true);
		} else {
			createZoneBtn.setText(DELETE_WALK_ZONE);
			createObstacleBtn.setDisabled(false);
		}
	}

}
