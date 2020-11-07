/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.views.nodes;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionForm;

@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class IntermediateNodeForm extends ActionForm<IntermediateNode>
{
	private static final long serialVersionUID = -6878512672974473759L;

	static final String ActivateAllChildrenClassName = NodeFormField + "activateAllChildren"; //$NON-NLS-1$
	static final String DectivateAllChildrenClassName = NodeFormField + "deactivateAllChildren"; //$NON-NLS-1$
	static final String OperationClassName = NodeFormField + "operation"; //$NON-NLS-1$

	private static final String DeactivateAllChildrenClassName = null;

	protected Button activateAllChildren;
	protected Button deactivateAllChildren;

	protected Select<IntermediateNode.ChildrenCondition> condition;

	@Override
	public void init(NodeService nodeService) {
		super.init(IntermediateNode.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.activateAllChildren = new Button(Messages.getString("IntermediateNode.ActivateAllChildren.Label")); //$NON-NLS-1$
		this.activateAllChildren.setClassName(ActivateAllChildrenClassName);
		this.activateAllChildren.setThemeName(UIConstants.LabelPaddingTheme);
		this.activateAllChildren.setEnabled(this.isAdminUser);
		this.activateAllChildren.addClickListener(event ->
			{
				this.log.info("activateAllChildren clicked on '{}'", this.node.getName()); //$NON-NLS-1$
				if (event.isFromClient()) {
					this.node.trickleDown(n -> {
						if (n instanceof LeafNode) {
							((LeafNode)n).setActive(true);
						}
					});
				}
			});

		this.deactivateAllChildren = new Button(Messages.getString("IntermediateNode.DeactivateAllChildren.Label")); //$NON-NLS-1$
		this.deactivateAllChildren.setClassName(DeactivateAllChildrenClassName);
		this.deactivateAllChildren.setThemeName(UIConstants.LabelPaddingTheme);
		this.deactivateAllChildren.setEnabled(this.isAdminUser);
		this.deactivateAllChildren.addClickListener(event ->
			{
				this.log.info("deactivateAllChildren clicked on '{}'", this.node.getName()); //$NON-NLS-1$
				if (event.isFromClient()) {
					this.node.trickleDown(n -> {
						if (n instanceof LeafNode) {
							((LeafNode)n).setActive(false);
						}
					});
				}
			});

		HorizontalLayout childrenOps = new HorizontalLayout(this.executeButton, this.activateAllChildren, this.deactivateAllChildren);
		childrenOps.addClassName(CombinedClassName);

		this.condition = new Select<>();
		this.condition.addClassName(OperationClassName);
		this.condition.setItems(IntermediateNode.ChildrenCondition.values());
		this.condition.setLabel(Messages.getString("IntermediateNode.ChildCondition.Label")); //$NON-NLS-1$
		this.condition.setItemLabelGenerator((cond) -> Messages.getString("IntermediateNode.ChildCondition." + cond.name())); //$NON-NLS-1$
		this.condition.setEnabled(this.isAdminUser);
		this.binder.forField(this.condition)
			.bind(IntermediateNode::getCondition, IntermediateNode::setCondition);
//		this.condition.addValueChangeListener(event ->
//		{
//			log.info("condition value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
//			if (event.isFromClient()) {
//				this.node.deriveNewState();
//			}
//		});

		this.fields.add(childrenOps, this.condition);
	}
}
