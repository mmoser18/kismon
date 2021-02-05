/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.select.Select;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.IntermediateNode;

// @Slf4j
public class IntermediateNodeForm extends NodeForm<IntermediateNode>
{
	private static final long serialVersionUID = -6878512672974473759L;

	static final String OperationClassName = NodeFormField + "operation"; //$NON-NLS-1$

	protected Select<IntermediateNode.ChildrenCondition> condition;


	public IntermediateNodeForm(NodeService nodeService) {
		super(IntermediateNode.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.condition = new Select<>(IntermediateNode.ChildrenCondition.values());
		this.condition.addClassName(OperationClassName);
		this.condition.setLabel(Messages.getString("IntermediateNode.ChildCondition.Label")); //$NON-NLS-1$
		this.condition.setItemLabelGenerator((cond) -> Messages.getString("IntermediateNode.ChildCondition." + cond.name())); //$NON-NLS-1$
		this.binder.forField(this.condition)
			.bind(IntermediateNode::getCondition, IntermediateNode::setCondition);
//		this.condition.addValueChangeListener(event ->
//		{
//			log.info("condition value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
//			if (event.isFromClient()) {
//				this.node.deriveNewState();
//			}
//		});

		this.fields.add(this.condition);
	}
}
