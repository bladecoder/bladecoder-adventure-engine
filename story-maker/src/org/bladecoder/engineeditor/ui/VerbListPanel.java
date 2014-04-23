package org.bladecoder.engineeditor.ui;

import java.net.URL;
import java.text.MessageFormat;

import javax.swing.ImageIcon;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class VerbListPanel extends ElementListPanel {

	private ActionListPanel actionList;

	public VerbListPanel() {
		super(true);
		actionList = new ActionListPanel();

		actionList.setAlignmentX(LEFT_ALIGNMENT);

		add(actionList);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				addActions();

				// editToolbar.enableEdit(pos!=-1);
			}
		});

		list.setCellRenderer(listCellRenderer);
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		return new CreateEditVerbDialog(Ctx.window, doc, parent, e);
	}

	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		super.addElements(doc, parent, tag);
		addActions();
	}

	private void addActions() {
		int pos = list.getSelectedIndex();

		Element v = null;

		if (pos != -1) {
			v = list.getModel().getElementAt(pos);
		}

		actionList.addElements(doc, v, null);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(true) {

		@Override
		public String getName(Element e) {
			return e.getAttribute("id");
		}

		@Override
		public String getInfo(Element e) {
			String state = e.getAttribute("state");
			String target = e.getAttribute("target");

			StringBuilder sb = new StringBuilder();

			if (!state.isEmpty())
				sb.append("when ").append(state);
			if (!target.isEmpty())
				sb.append(" with target '").append(target).append("'");

			return sb.toString();
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			String iconName = MessageFormat.format("/res/images/ic_{0}.png", e.getAttribute("id"));

			URL u = getClass().getResource(iconName);

			if (u == null)
				u = getClass().getResource("/res/images/ic_custom.png");

			return new ImageIcon(u);
		}
	};
}
