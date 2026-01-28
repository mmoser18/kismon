/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.RootNode;

@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class RootNodeForm extends IntermediateNodeForm
{
	private static final long serialVersionUID = 2620944667353199126L;

	static final String FilePathClassName = NodeFormField + "filePath"; //$NON-NLS-1$

	protected TextField filePath;
	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.filePath = new TextField(Messages.getString("RootNodeForm.FilePath.Label")); //$NON-NLS-1$
		this.filePath.addClassName(NameClassName);
		this.filePath.setThemeName(LabelPaddingTheme);
		this.filePath.setClearButtonVisible(this.clearButtonsVisible);
		this.binder.forField(this.filePath).bind(n -> ((RootNode)n).getFilePath(), null);

		this.fields.addComponentAsFirst(this.filePath);
	}
}
