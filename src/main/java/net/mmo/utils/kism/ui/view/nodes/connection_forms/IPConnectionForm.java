/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.ResultChecker;
import net.mmo.utils.kism.entities.ResultChecker.Condition;
import net.mmo.utils.kism.entities.connections.IPConnection;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.view.nodes.LeafNodeForm;

@Slf4j
public abstract class IPConnectionForm <N extends IPConnection> extends LeafNodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	// these are package visible so that unit-tests can access them:
	static final String URLClassName           = NodeFormField + "url"; //$NON-NLS-1$
	static final String ResultingURLClassName  = NodeFormField + "resulting-url"; //$NON-NLS-1$

	static final String RequestDetailsClassName = NodeFormField + "request-details"; //$NON-NLS-1$

	static final String ResultCheckerClassName = NodeFormField + "resultChecker"; //$NON-NLS-1$
	static final String ConditionClassName = NodeFormField + "condition"; //$NON-NLS-1$
	static final String OperandClassName = NodeFormField + "operand"; //$NON-NLS-1$

	protected TextField url;
	protected TextField resultingURL;

	// protected ResultCheckerForm resultChecker;
	protected Select<ResultChecker.Condition> condition;
	protected TextArea operand;


	public IPConnectionForm(Class<N> clazz, NodeService nodeService) {
		super(clazz, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.url = new TextField(Messages.getString("IPConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.setThemeName(LabelPaddingTheme);
		this.url.setClearButtonVisible(clearButtonsVisible);
		this.url.addValueChangeListener(event ->
			{
				log.info("url value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingUrl(event.getValue());
					} catch (Exception ex) {
						log.error("Error updating URL", ex); //$NON-NLS-1$
					}
				}
			});
		// had to disable the URL validation here (and moved it to HTTPConnectionForm and JDBCConnectionForm)
		// since JDBC-URLs look completely differently and didn't pass the conventional URI check!
		// this.binder.forField(this.url)
		//	.withValidator(this.urlValidator)
		//	.bind(IPConnection::getUrl, IPConnection::setUrl);

		this.resultingURL = new TextField(Messages.getString("IPConnectionForm.ResultingUrl.Value")); //$NON-NLS-1$
		this.resultingURL.setClassName(ResultingURLClassName);
		this.resultingURL.setThemeName(LabelPaddingTheme);
		// same here:
		// this.binder.forField(this.resultingURL).bind(IPConnection::getResultingUrl, null);

		this.checkResults.addValueChangeListener(event ->
		{
			log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptResultCheckerVisibility(event.getValue(), this.node);
			}
		});

//		this.resultChecker = new ResultCheckerForm();
//		this.resultChecker.setClassName(ResultCheckerClassName);
		this.condition = new Select<Condition>(Condition.values());
		this.condition.setLabel(Messages.getString("ResultChecker.Condition.Label")); //$NON-NLS-1$
		this.condition.setItemLabelGenerator(cond -> Messages.getString("ResultChecker.Condition.Value." + cond.name())); //$NON-NLS-1$
		this.condition.addClassName(ConditionClassName);

		this.operand = new TextArea(Messages.getString("ResultChecker.Operand.Label")); //$NON-NLS-1$
		this.operand.addClassName(OperandClassName);
		this.operand.setThemeName(UIConstants.LabelPaddingTheme);
		this.operand.setClearButtonVisible(clearButtonsVisible);

		this.binder.forField(this.condition).bind(IPConnectionForm::getCondition, IPConnectionForm::setCondition);
		this.binder.forField(this.operand)
			.withValidator(this.propertiesResolvableValidator)
			.bind(IPConnectionForm::getOperand, IPConnectionForm::setOperand);

		this.connnectionDetailsPanel.add(this.url, this.resultingURL);
		this.validationDetailsPanel.add(this.condition, this.operand);
	}

	@Override
	public boolean setNode(N node) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node);
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

	static <N extends IPConnection> void setOperand(N n, String operand) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setOperand(operand);
		}
	}


	private void adaptResultCheckerVisibility(boolean check, N n) {
		if (n != null) {
			if (n.getResultChecker() == null) {
				log.info("creating new ResultChecker:"); //$NON-NLS-1$
				n.setResultChecker(new ResultChecker());
//				// if checkResults is active we need to add the validity of the resultChecker's binder to this one:
//				this.binder.withValidator(nn -> (checkResults ? this.resultChecker.getBinder().isValid() : true),
//				                          "ResultCheck not valid"); //$NON-NLS-1$
			}
//			this.resultChecker.setResultChecker(n.getResultChecker());
//			this.resultChecker.setVisible(check);
			this.condition.setVisible(check);
			this.operand.setVisible(check);
		} else { // we leave the resultChecker attached to the node - in case the user clicks the check-button again.
			log.info("removing ResultChecker from view:"); //$NON-NLS-1$
//			this.resultChecker.setResultChecker(null);
//			this.resultChecker.setVisible(false);
			this.condition.setVisible(false);
			this.operand.setVisible(false);
		}
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl(this.node != null ? this.node.getUrl() : null);
	}

	protected void updateResultingUrl(String newValue) {
		updateResultingField("updateResultingURL", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     IPConnection::setResultingUrl,
		                     this.resultingURL
		                    );
	}

	public static Double getDurationInMillis(IPConnection n) {
		return convertDurationToMillis(n.getDuration());
	}
}
