/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.RootNode;

public class RootNodeForm extends IntermediateNodeForm
{
	private static final long serialVersionUID = 2620944667353199126L;

	static final String FilePathClassName = NodeFormField + "filePath"; //$NON-NLS-1$

	protected TextField filePath;

	public RootNodeForm(NodeService nodeService) {
		super(nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.filePath = new TextField(Messages.getString("RootNodeForm.FilePath.Label")); //$NON-NLS-1$
		this.filePath.addClassName(NameClassName);
		this.filePath.setThemeName(LabelPaddingTheme);
		this.filePath.setClearButtonVisible(clearButtonsVisible);
		this.binder.forField(this.filePath).bind(n -> ((RootNode)n).getFilePath(), null);

		this.fields.addComponentAsFirst(this.filePath);
	}
}
