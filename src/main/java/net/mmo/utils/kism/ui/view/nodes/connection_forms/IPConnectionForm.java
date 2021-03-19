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

		this.hostAddress = new TextField(Messages.getString("TCPConnectionForm.HostAddress.Label")); //$NON-NLS-1$
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
		// had to disable the URL validation here (and moved it to HTTPConnectionForm and JDBCConnectionForm)
		// since JDBC-URLs look completely differently and didn't pass the conventional URI check!
		// this.binder.forField(this.url)
		//	.withValidator(this.urlValidator)
		//	.bind(TCPConnection::getUrl, TCPConnection::setUrl);

		this.resultingHostAddress = new TextField(Messages.getString("TCPConnectionForm.ResultingHostAddress.Value")); //$NON-NLS-1$
		this.resultingHostAddress.setClassName(ResultingHostAddressClassName);
		this.resultingHostAddress.setThemeName(LabelPaddingTheme);
		// same here:
		// this.binder.forField(this.resultingURL).bind(TCPConnection::getResultingUrl, null);

		this.checkResults.addValueChangeListener(event ->
		{
			log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptResultCheckerVisibility(event.getValue(), this.node);
			}
		});

//		this.resultChecker = new ResultCheckerForm();
//		this.resultChecker.setClassName(ResultCheckerClassName);
//		this.condition = new Select<Condition>(Condition.values());
//		this.condition.setLabel(Messages.getString("ResultChecker.Condition.Label")); //$NON-NLS-1$
//		this.condition.setItemLabelGenerator(cond -> Messages.getString("ResultChecker.Condition.Value." + cond.name())); //$NON-NLS-1$
//		this.condition.addClassName(ConditionClassName);
//
//		this.operand = new TextArea(Messages.getString("ResultChecker.Operand.Label")); //$NON-NLS-1$
//		this.operand.addClassName(OperandClassName);
//		this.operand.setThemeName(LabelPaddingTheme);
//		this.operand.setClearButtonVisible(NodeForm.clearButtonsVisible);
//
//		this.binder.forField(this.condition).bind(IPConnectionForm::getCondition, IPConnectionForm::setCondition);
//		this.binder.forField(this.operand)
//			.withValidator(this.propertiesResolvableValidator)
//			.bind(IPConnectionForm::getOperand, IPConnectionForm::setOperand);

		this.connnectionDetailsPanel.add(this.hostAddress, this.resultingHostAddress);
//		this.validationDetailsPanel.add(this.condition, this.operand);
	}

	@Override
	public boolean setNode(N node, boolean abandonChanges) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node, abandonChanges);
		adaptResultCheckerVisibility((node != null ? node.isCheckResults() : false), node);
		return res;
	}

//	// when the main-form's bean is read we also need to read the ResultChecker's data (if set):
//	@Override
//	protected void readBean(N n) {
//		log.info("readBean node={}", n); //$NON-NLS-1$
//		super.readBean(n);
//		if (n != null) {
//			ResultChecker rc = n.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				rcBinder.readBean(rc);
//			}
//		}
//	}
//
//	// when the main-form's bean is to be written we also first need to write the ResultChecker's data (if set):
//	@Override
//	protected void writeBean(N n) throws ValidationException {
//		log.info("writeBean node={}", n); //$NON-NLS-1$
//		if (n != null) {
//			ResultChecker rc = n.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				rcBinder.writeBean(rc);
//			}
//		}
//		super.writeBean(n);
//	}
//
//	@Override
//	protected boolean binderHasChanges() {
//		if (this.node != null) {
//			ResultChecker rc = this.node.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				if (rcBinder.hasChanges()) {
//					log.debug("binderHasChanges{}: true"); //$NON-NLS-1$
//					return true;
//				}
//			}
//		}
//		return super.binderHasChanges();
//	}
//
//	// We need to prevent bean validation when there is currently no bean set or else we get
//	// another one of these dreaded IllegalStateException. I don't why this is not done by default :-(
//	@Override
//	protected boolean binderIsValid() {
//		if (this.node != null) {
//			ResultChecker rc = this.node.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				if (rcBinder.getBean() != null && !rcBinder.isValid()) {
//					log.debug("binderIsValid{}: false"); //$NON-NLS-1$
//					return false;
//				}
//			}
//		}
//		return super.binderIsValid();
//	}

	static <N extends TCPConnection> ResultChecker.Condition getCondition(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getCondition()
				: null);
	}

	static <N extends TCPConnection> void setCondition(N n, ResultChecker.Condition condition) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setCondition(condition);
		}
	}

	static <N extends TCPConnection> String getOperand(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getOperand()
				: null);
	}

	static <N extends TCPConnection> void setOperand(N n, String operand) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setOperand(operand);
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
