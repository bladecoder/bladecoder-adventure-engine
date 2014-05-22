package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditList;
import org.bladecoder.engineeditor.utils.DesktopUtils;

import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ResolutionList extends EditList<Resolution> {

	public ResolutionList(Skin skin) {
		super(skin);
		toolbar.hideCopyPaste();		

		list.setCellRenderer(listCellRenderer);
		
		list.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
			}
		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						toolbar.disableCreate(Ctx.project.getProjectDir() == null);
						addResolutions();
					}
				});
	}

	private void addResolutions() {
		if (Ctx.project.getProjectDir() != null) {

			list.getItems().clear();

			ArrayList<Resolution> tmp = new ArrayList<Resolution>();

			for (Resolution scn : Ctx.project.getResolutions()) {
				tmp.add(scn);
			}

			Collections.sort(tmp, new Comparator<Resolution>() {
				@Override
				public int compare(Resolution o1, Resolution o2) {
					return o2.portraitWidth - o1.portraitWidth;
				}
			});

			for (Resolution s : tmp)
				list.getItems().add(s);

			if (list.getItems().size > 0) {
				list.setSelectedIndex(0);
			}
		}

		toolbar.disableCreate(Ctx.project.getProjectDir() == null);
	}

	@Override
	public void create() {
		CreateResolutionDialog dialog = new CreateResolutionDialog(skin);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addResolutions();
			}});
	}

	@Override
	public void edit() {

	}

	@Override
	public void delete() {
		int index = list.getSelectedIndex();
		Resolution r = list.getItems().get(index);

		removeDir(Ctx.project.getProjectDir() + "/" + Project.BACKGROUNDS_PATH
				+ "/" + r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.OVERLAYS_PATH
				+ "/" + r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.UI_PATH + "/"
				+ r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.ATLASES_PATH
				+ "/" + r.suffix);

		addResolutions();
	}

	private void removeDir(String dir) {
		try {
			DesktopUtils.removeDir(dir);
		} catch (IOException e) {
			String msg = "Something went wrong while deleting the resolution.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(),msg, 2000);
			e.printStackTrace();
		}
	}
	
	@Override
	protected void copy() {
	
	}

	@Override
	protected void paste() {
	
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private static final CellRenderer<Resolution> listCellRenderer = new CellRenderer<Resolution>() {
		@Override
		protected String getCellTitle(Resolution r) {
			return r.suffix;
		}
	};
}
