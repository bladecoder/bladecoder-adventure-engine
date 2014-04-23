package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.FileInputPanel;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.xml.sax.SAXException;

@SuppressWarnings("serial")
public class CreateProjectDialog extends EditDialog {

	public static final String INFO = "<html>A project folder with the proper structure <br/>will be created in the selected location.</html>";

	private InputPanel projectName;
	private InputPanel location;

	public CreateProjectDialog(java.awt.Frame parent) {
		super(parent);

		setInfo(INFO);

		projectName = new InputPanel("Project Name", "<html>Select the name of the project</html>");

		location = new FileInputPanel("Location",
				"<html>Select the folder location for the project</html>", true);

		setTitle("CREATE PROJECT");

		centerPanel.add(projectName);
		centerPanel.add(location);

		init(parent);
	}

	@Override
	protected void ok() {
		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the current project.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);
		}

		dispose();

		createProject();

	}

	private void createProject() {
		Ctx.window.getScnCanvas().setMsg("Creating project...");

		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				try {
					Ctx.project.createProject(new File(location.getText() + "/"
							+ projectName.getText()));
				} catch (Exception e) {
					EditorLogger.error(e.getMessage());
					return false;
				}

				return true;
			}

			@Override
			protected void done() {
				boolean res = true;

				try {
					res = get();
				} catch (InterruptedException | ExecutionException e1) {
					res = false;
				}

				if (res) {
					try {
						Ctx.project.loadProject(new File(location.getText() + "/"
								+ projectName.getText()));
					} catch (IOException | ParserConfigurationException | SAXException e) {
						EditorLogger.error(e.getMessage());
						res = false;
					}
				}

				if (!res) {
					Ctx.window.getScnCanvas().setMsgWithTimer(
							"ERROR CREATING PROJECT", 2000);
				} else {
					Ctx.window.getScnCanvas().setMsg(null);
				}
			}
		};

		worker.execute();
	}

	@Override
	protected boolean validateFields() {
		boolean isOk = true;

		if (projectName.getText().trim().isEmpty()) {
			projectName.setError(true);
			isOk = false;
		} else
			projectName.setError(false);

		if (location.getText().trim().isEmpty()) {
			location.setError(true);
			isOk = false;
		} else
			location.setError(false);

		return isOk;
	}
}
