/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.MonospaceTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.IPConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection.Auth_Method;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.ui.CommonConstants;

@SuppressWarnings("javadoc")
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

	static final String FingerprintClassName  = NodeFormField + "fingerprint"; //$NON-NLS-1$

	static final String CommandClassName  = NodeFormField + "command"; //$NON-NLS-1$
	static final String ResponseClassName = NodeFormField + "response"; //$NON-NLS-1$

	static final String RequestDetailsClassName = NodeFormField + "request-details"; //$NON-NLS-1$
	static final String ResponseDetailsClassName = NodeFormField + "response-details"; //$NON-NLS-1$

	static final String SERVER_FINGERPRINT_PATTERN = "\\d{2}(:\\d2){15}"; //$NON-NLS-1$

	protected IntegerField hostPort;

	// authentication stuff:
	protected TextField username;
	protected Select<Auth_Method> authMethod;
	protected PasswordField authArg;
	protected TextField fingerprint; // optional - if none is given "PromiscuousVerifier" is used.

	protected TextField command;
	protected TextArea  response;

	@Override
	public void init(NodeService nodeService) {
		super.init(SSHConnection.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.hostPort = new IntegerField(Messages.getString("SSHConnectionForm.HostPort.Label")); //$NON-NLS-1$
		this.hostPort.setClassName(HostPortClassName);
		this.hostPort.setThemeName(LabelPaddingTheme);
		this.hostPort.setStepButtonsVisible(true);
		this.hostPort.setClearButtonVisible(this.clearButtonsVisible);
		this.hostPort.setEnabled(this.isAdminUser);
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

		this.authMethod = new Select<>();
		this.authMethod.setClassName(AuthMethodClassName);
		this.authMethod.setLabel(Messages.getString("SSHConnectionForm.AuthMethod.Label")); //$NON-NLS-1$
		this.authMethod.setItems(Auth_Method.values());
		this.authMethod.setItemLabelGenerator(item -> {
				return Messages.getString("SSHConnectionForm.AuthMethod.Item." + item.name()); //$NON-NLS-1$
			});
		this.authMethod.setEnabled(this.isAdminUser);
		this.authMethod.addValueChangeListener(event -> {
				if (event.isFromClient()) {
					this.log.info("command value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
					this.authArg.setLabel(Messages.getString("SSHConnectionForm.AuthArg.Label." + event.getValue().name())); //$NON-NLS-1$
				}
			});

		this.username = new TextField(Messages.getString("SSHConnectionForm.Uid.Label")); //$NON-NLS-1$
		this.username.setClassName(UidClassName);
		this.username.setThemeName(LabelPaddingTheme);
		this.username.setClearButtonVisible(this.clearButtonsVisible);
		this.username.setEnabled(this.isAdminUser);
		this.binder.forField(this.username)
			.withValidator(this.propertiesResolvableValidator)
			.bind(SSHConnection::getUsername, SSHConnection::setUsername);

		this.authArg = new PasswordField(Messages.getString("SSHConnectionForm.AuthArg.Label." + Auth_Method.PWD.name())); //$NON-NLS-1$
		this.authArg.setClassName(AuthArgClassName);
		this.authArg.setThemeName(LabelPaddingTheme);
		this.authArg.setClearButtonVisible(this.clearButtonsVisible);
		this.authArg.setEnabled(this.isAdminUser);
		this.authArg.setRevealButtonVisible(this.isAdminUser);
		if (this.isAdminUser) {
			this.binder.forField(this.authArg)
				.withValidator(this.propertiesResolvableValidator)
				.bind(SSHConnection::getAuthArg, SSHConnection::setAuthArg);
		} else { // we don't want to send the pwd to the client - not even hidden - if (s)he is not an admin user!
			this.binder.forField(this.authArg)
				.bind(CommonConstants::suppressedPassword, null);
		}
		this.fingerprint = new TextField(Messages.getString("SSHConnectionForm.Fingerprint.Label")); //$NON-NLS-1$
		this.fingerprint.setClassName(CommandClassName);
		this.fingerprint.setThemeName(LabelPaddingTheme);
		this.fingerprint.addThemeName(MonospaceTheme);
		this.fingerprint.setWidth("47em"); // 16 double-digit groups plus ':' separators //$NON-NLS-1$
		this.fingerprint.setClearButtonVisible(this.clearButtonsVisible);
		this.fingerprint.setEnabled(this.isAdminUser);
		this.binder.forField(this.fingerprint)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator((value, context) -> {
					String fp;
					try {
						fp = this.node.resolveProperties(value);
					} catch (Exception ex) {
						return ValidationResult.error("Error resolving property: " + ex); //$NON-NLS-1$
					}
					return fp.isBlank() || fp.matches(SERVER_FINGERPRINT_PATTERN)
						? ValidationResult.ok()
						: ValidationResult.error("Not a legal fingerprint string - must match pattern '{" + SERVER_FINGERPRINT_PATTERN + "}'"); //$NON-NLS-1$ //$NON-NLS-2$
				})
			.bind(SSHConnection::getFingerprint, SSHConnection::setFingerprint);

		this.command = new TextField(Messages.getString("SSHConnectionForm.Command.Label")); //$NON-NLS-1$
		this.command.setClassName(CommandClassName);
		this.command.setThemeName(LabelPaddingTheme);
		this.command.addThemeName(MonospaceTheme);
		this.command.setClearButtonVisible(this.clearButtonsVisible);
		this.command.setEnabled(this.isAdminUser);
		this.command.addValueChangeListener(event ->
			{
				if (event.isFromClient()) {
					this.log.info("command value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
				}
			});

		this.response = new TextArea(Messages.getString("SSHConnectionForm.Response.Label")); //$NON-NLS-1$
		this.response.setClassName(ResponseClassName);
		this.response.setThemeName(LabelPaddingTheme);
		this.response.addThemeName(MonospaceTheme);
		this.response.setReadOnly(true);

		// combining and grouping bits and pieces:
		HorizontalLayout hostAndPort = new HorizontalLayout(this.hostAddress, this.hostPort);
		hostAndPort.addClassName(CombinedClassName);

		HorizontalLayout credentials = new HorizontalLayout(this.authMethod, this.username, this.authArg);
		credentials.addClassName(CombinedClassName);

		this.connnectionDetailsPanel.add(hostAndPort,
		                                 this.resultingHostAddress,
		                                 credentials,
		                                 this.fingerprint,
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

		this.fields.add(new NativeLabel(Messages.getString("SSHConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseDetailsFields,
		                requestDetails,
		                this.validationDetails
		               );
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
