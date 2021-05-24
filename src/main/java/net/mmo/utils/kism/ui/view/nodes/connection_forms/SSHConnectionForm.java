/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.MonospaceTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ResultChecker;
import net.mmo.utils.kism.entities.nodes.ResultChecker.Condition;
import net.mmo.utils.kism.entities.nodes.connections.IPConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection.Auth_Method;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

@Slf4j
@PreserveOnRefresh
public class SSHConnectionForm extends IPConnectionForm<SSHConnection>
{
	private static final long serialVersionUID = 995865766559973043L;

	// these are package visible so that unit-tests can access them:
	static final String HostPortClassName = NodeFormField + "hostPort"; //$NON-NLS-1$

	static final String UidClassName = NodeFormField + "username"; //$NON-NLS-1$
	static final String AuthMethodClassName = NodeFormField + "authMethod"; //$NON-NLS-1$
//	static final String PwdClassName = NodeFormField + "password"; //$NON-NLS-1$
//	static final String KeyClassName = NodeFormField + "privkey"; //$NON-NLS-1$
	static final String AuthArgClassName = NodeFormField + "authArg"; //$NON-NLS-1$

	static final String CommandClassName  = NodeFormField + "command"; //$NON-NLS-1$
	static final String ResponseClassName = NodeFormField + "response"; //$NON-NLS-1$

	static final String RequestDetailsClassName = NodeFormField + "request-details"; //$NON-NLS-1$

	static final String ResponseDetailsClassName = NodeFormField + "response-details"; //$NON-NLS-1$

	static final String ResultCheckerClassName = NodeFormField + "resultChecker"; //$NON-NLS-1$
	static final String ConditionClassName = NodeFormField + "condition"; //$NON-NLS-1$
	static final String OperandClassName = NodeFormField + "operand"; //$NON-NLS-1$

	protected IntegerField hostPort;

	// authentication stuff:
	protected TextField username;
	protected Select<Auth_Method> authMethod;
	protected PasswordField authArg;

	protected TextField command;
	protected TextArea  response;

	// protected ResultCheckerForm resultChecker;
	protected Select<ResultChecker.Condition> condition;
	protected TextArea operand;

	public SSHConnectionForm(NodeService nodeService) {
		super(SSHConnection.class, nodeService);
	}

	@SuppressWarnings("serial")
	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.hostPort = new IntegerField(Messages.getString("SSHConnectionForm.HostPort.Label")); //$NON-NLS-1$
		this.hostPort.setClassName(HostPortClassName);
		this.hostPort.setThemeName(LabelPaddingTheme);
		this.hostPort.setHasControls(true);
		this.hostPort.setClearButtonVisible(NodeForm.clearButtonsVisible);
//		this.hostPort.addValueChangeListener(event ->
//			{
//				log.debug("hostAddress value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
//				if (event.isFromClient()) {
//					try {
//						updateResultingHostAddress(event.getValue());
//					} catch (Exception ex) {
//						log.error("Error updating URL", ex); //$NON-NLS-1$
//					}
//				}
//			});

		this.authMethod = new Select<>(Auth_Method.values());
		this.authMethod.setClassName(AuthMethodClassName);
		this.authMethod.setLabel(Messages.getString("SSHConnectionForm.AuthMethod.Label")); //$NON-NLS-1$
		this.authMethod.setItemLabelGenerator(item -> {
				return Messages.getString("SSHConnectionForm.AuthMethod.Item." + item.name()); //$NON-NLS-1$
			});
		this.authMethod.addValueChangeListener(event -> {
				if (event.isFromClient()) {
					log.info("command value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
					this.authArg.setLabel(Messages.getString("SSHConnectionForm.AuthArg.Label." + event.getValue().name())); //$NON-NLS-1$
				}
			});

		this.username = new TextField(Messages.getString("SSHConnectionForm.Uid.Label")); //$NON-NLS-1$
		this.username.setClassName(UidClassName);
		this.username.setThemeName(LabelPaddingTheme);
		this.username.setClearButtonVisible(NodeForm.clearButtonsVisible);
		this.binder.forField(this.username).withValidator(this.propertiesResolvableValidator);

		this.authArg = new PasswordField(Messages.getString("SSHConnectionForm.AuthArg.Label." + Auth_Method.PWD.name())); //$NON-NLS-1$
		this.authArg.setClassName(AuthArgClassName);
		this.authArg.setThemeName(LabelPaddingTheme);
		this.authArg.setClearButtonVisible(NodeForm.clearButtonsVisible);
		this.binder.forField(this.authArg).withValidator(this.propertiesResolvableValidator);

		this.command = new TextField(Messages.getString("SSHConnectionForm.Command.Label")); //$NON-NLS-1$
		this.command.setClassName(CommandClassName);
		this.command.setThemeName(LabelPaddingTheme);
		this.command.addThemeName(MonospaceTheme);
		this.command.addValueChangeListener(event ->
			{
				if (event.isFromClient()) {
					log.info("command value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
				}
			});

		this.response = new TextArea(Messages.getString("SSHConnectionForm.Response.Label")); //$NON-NLS-1$
		this.response.setClassName(ResponseClassName);
		this.response.setThemeName(LabelPaddingTheme);
		this.response.addThemeName(MonospaceTheme);

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
		this.operand.setThemeName(LabelPaddingTheme);
		this.operand.setClearButtonVisible(NodeForm.clearButtonsVisible);


		this.binder.forField(this.condition).bind(SSHConnectionForm::getCondition, SSHConnectionForm::setCondition);
		this.binder.forField(this.operand)
			.withValidator(this.propertiesResolvableValidator)
			.bind(SSHConnectionForm::getOperand, SSHConnectionForm::setOperand);

		// combining and grouping bits and pieces:
		HorizontalLayout hostAndPort = new HorizontalLayout(this.hostAddress, this.hostPort);
		hostAndPort.addClassName(CombinedClassName);

		HorizontalLayout credentials = new HorizontalLayout(this.authMethod, this.username, this.authArg);
		credentials.addClassName(CombinedClassName);

		this.connnectionDetailsPanel.add(hostAndPort,
		                                 this.resultingHostAddress,
		                                 credentials,
		                                 this.command
		                                 );

		HorizontalLayout responseDetailsFields = new HorizontalLayout(this.duration, this.timestamp);
		responseDetailsFields.addClassName(CombinedClassName);

		Accordion requestDetails = new Accordion();
		requestDetails.close(); // initially closed
		VerticalLayout requestDetailsPanel = new VerticalLayout();
		requestDetailsPanel.setClassName(RequestDetailsClassName);
		requestDetailsPanel.add(this.response);
		requestDetails.add(Messages.getString("SSHConnectionForm.RequestAndResponseDetails.Label"), requestDetailsPanel); //$NON-NLS-1$

		this.validationDetailsPanel.add(this.condition, this.operand);

		this.fields.add(new Label(Messages.getString("SSHConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseDetailsFields,
		                requestDetails,
		                this.validationDetails
		               );
	}

	@Override
	public boolean setNode(SSHConnection node, boolean abandonChanges) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node, abandonChanges);
		adaptResultCheckerVisibility((node != null ? node.isCheckResults() : false), node);
		return res;
	}

	private void adaptResultCheckerVisibility(boolean check, SSHConnection n) {
		if (n != null) {
			if (n.getResultChecker() == null) {
				log.info("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
				n.setResultChecker(new ResultChecker());
// attempt to bind the ResultChecker in via a child-form:
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
		updateResultingHostAddress(this.node != null ? this.node.getHostAddress() : null);
	}

	@Override
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
