/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ResultChecker;
import net.mmo.utils.kism.entities.nodes.connections.IPConnection;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.ui.view.nodes.LeafNodeForm;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

@Slf4j
@PreserveOnRefresh
public abstract class IPConnectionForm <N extends IPConnection> extends LeafNodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	// these are package visible so that unit-tests can access them:
	static final String HostAddressClassName           = NodeFormField + "hostAddress"; //$NON-NLS-1$
	static final String ResultingHostAddressClassName  = NodeFormField + "resulting-hostAddress"; //$NON-NLS-1$

	protected TextField hostAddress;
	protected TextField resultingHostAddress;


	public IPConnectionForm(Class<N> clazz, NodeService nodeService) {
		super(clazz, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.hostAddress = new TextField(Messages.getString("IPConnectionForm.HostAddress.Label")); //$NON-NLS-1$
		this.hostAddress.setClassName(HostAddressClassName);
		this.hostAddress.setThemeName(LabelPaddingTheme);
		this.hostAddress.setClearButtonVisible(NodeForm.clearButtonsVisible);
		this.hostAddress.addValueChangeListener(event ->
			{
				log.debug("hostAddress value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingHostAddress(event.getValue());
					} catch (Exception ex) {
						log.error("Error updating URL", ex); //$NON-NLS-1$
					}
				}
			});

		this.resultingHostAddress = new TextField(Messages.getString("IPConnectionForm.ResultingHostAddress.Label")); //$NON-NLS-1$
		this.resultingHostAddress.setClassName(ResultingHostAddressClassName);
		this.resultingHostAddress.setThemeName(LabelPaddingTheme);

		this.checkResults.addValueChangeListener(event ->
		{
			log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptResultCheckerVisibility(event.getValue(), this.node);
			}
		});

		this.connnectionDetailsPanel.add(this.hostAddress, this.resultingHostAddress);
	}

	@Override
	public boolean setNode(N node, boolean abandonChanges) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node, abandonChanges);
		adaptResultCheckerVisibility((node != null ? node.isCheckResults() : false), node);
		return res;
	}

	static <N extends IPConnection> ResultChecker.Condition getCondition(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getCondition()
				: null);
	}

	static <N extends IPConnection> void setCondition(N n, ResultChecker.Condition condition) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setCondition(condition);
		}
	}

	static <N extends IPConnection> String getOperand(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getOperand()
				: null);
	}

	static <N extends IPConnection> void setOperand(N n, String operands) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setOperand(operands);
		}
	}


	private void adaptResultCheckerVisibility(boolean check, N n) {
//		if (n != null) {
//			if (n.getResultChecker() == null) {
//				log.info("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
//				n.setResultChecker(new ResultChecker());
//// attempt to bind the ResultChecker in via a child-form:
////				// if checkResults is active we need to add the validity of the resultChecker's binder to this one:
////				this.binder.withValidator(nn -> (checkResults ? this.resultChecker.getBinder().isValid() : true),
////				                          "ResultCheck not valid"); //$NON-NLS-1$
//			}
////			this.resultChecker.setResultChecker(n.getResultChecker());
////			this.resultChecker.setVisible(check);
//			this.condition.setVisible(check);
//			this.operand.setVisible(check);
//		} else { // we leave the resultChecker attached to the node - in case the user clicks the check-button again.
//			log.info("removing ResultChecker from view:"); //$NON-NLS-1$
////			this.resultChecker.setResultChecker(null);
////			this.resultChecker.setVisible(false);
//			this.condition.setVisible(false);
//			this.operand.setVisible(false);
//		}
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingHostAddress(this.node != null ? this.node.getHostAddress() : null);
	}

	protected void updateResultingHostAddress(String newValue) {
		updateResultingField("updateResultingHostAddress", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     IPConnection::setResultingHostAddress,
		                     this.resultingHostAddress
		                    );
	}

	public static Double getDurationInMillis(TCPConnection n) {
		return convertDurationToMillis(n.getDuration());
	}
}
