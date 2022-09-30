/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions.impl;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.entities.nodes.connections.IPConnection;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.NodeProperties;
import net.mmo.utils.kism.utils.PropertyResolver;
import net.mmo.utils.kism.utils.StringUtils;


/**
 * class description here...
 * @param <N>
 */
@ToString(includeFieldNames = true, callSuper = true)
@Setter
@Getter
@Slf4j
public class SendMailAction <N extends ActionableNode> implements IAction<N>
{
	@NotEmpty
	@Email
	String mailTo;
	@Email
	String mailCc;
	@NotEmpty
	@Email
	String mailFrom;
	@NotEmpty
	@NotBlank
	String mailSubject;
	String mailContent;

	// Assuming you are sending email from localhost:
	static String mailHost;
	static String mailPort;

	static String mailProtocol;
	static String mailUser;
	static String mailPwd;
	static {
		Properties appProperties = AppProperties.getProperties();
		mailHost = appProperties.getProperty("SendMailAction.mailHost"); //$NON-NLS-1$
		mailPort = appProperties.getProperty("SendMailAction.mailPort"); //$NON-NLS-1$
		mailProtocol = appProperties.getProperty("SendMailAction.mailProtocol"); //$NON-NLS-1$
		mailUser = appProperties.getProperty("SendMailAction.mailUser"); //$NON-NLS-1$
		mailPwd  = appProperties.getProperty("SendMailAction.mailPwd"); //$NON-NLS-1$
	}

	@JsonIgnore
	MailProperties mailProperties = new MailProperties();

	@JsonIgnore
	Session mailSession;
	/**
	 * we need a public parameterless constructor so that the ActionFactory can create this using <class>.newInstance();
	 */
	public SendMailAction() {
		Properties appProperties = AppProperties.getProperties();
		this.mailTo   = appProperties.getProperty("SendMailForm.To.Default", //$NON-NLS-1$
		                                          "foobar@example.com"); //$NON-NLS-1$ // $NON-NLS-2$
		this.mailCc   = appProperties.getProperty("SendMailForm.Cc.Default"); //$NON-NLS-1$
		this.mailFrom = appProperties.getProperty("SendMailForm.Sender.Default", //$NON-NLS-1$
		                                          "barfoo@example.com"); //$NON-NLS-1$ // $NON-NLS-2$
		this.mailSubject = appProperties.getProperty("SendMailForm.Subject.Default", //$NON-NLS-1$
		                                             "Notification for Node '${NodeName}': ${NodeStatusTo}"); //$NON-NLS-1$
		this.mailContent = appProperties.getProperty("SendMailForm.Content.Default", //$NON-NLS-1$
		                                             "Notification for Node '${NodeName}': Status-change: ${NodeStatusFrom} => ${NodeStatusTo}"); //$NON-NLS-1$
	}

//	@SuppressWarnings("javadoc")
//	public void setMailTo(String mailTo) {
//		log.info("setMailTo:{}", mailTo); //$NON-NLS-1$
//		this.mailTo = mailTo;
//	}

	@Override
	public void doAction(N node, State oldState, State newState, ActionCheck check) throws Exception {
		this.mailProperties.setData(node, oldState, newState, check);

		try {
			MimeMessage message;
			String subject = this.mailProperties.resolveProperties(this.mailSubject);
			String content = this.mailProperties.resolveProperties(this.mailContent);

			String msgDetails =
				String.format("email to '%s' (subject: '%s') for node '%s' having changed from %s to %s.", //$NON-NLS-1$
				              this.mailTo, subject, node.getName(), oldState, newState);
			log.info("Sending " + msgDetails); //$NON-NLS-1$
			if (this.mailSession == null) {
				Properties properties = System.getProperties();
//				if (!StringUtils.isEmpty(SendMailAction.mailUser)) {
//					properties.setProperty("mail.user", SendMailAction.mailUser); //$NON-NLS-1$
//					if (!StringUtils.isEmpty(SendMailAction.mailPwd)) properties.setProperty("mail.password", SendMailAction.mailPwd); //$NON-NLS-1$
//				}
				properties.setProperty("mail.smtp.host", node.resolveProperties(SendMailAction.mailHost));  //$NON-NLS-1$
				if (!StringUtils.isEmpty(SendMailAction.mailPort)) properties.setProperty("mail.smtp.port", node.resolveProperties(SendMailAction.mailPort));  //$NON-NLS-1$
				if (!StringUtils.isEmpty(SendMailAction.mailProtocol)) properties.setProperty("mail.smtp.protocol", node.resolveProperties(SendMailAction.mailProtocol)); //$NON-NLS-1$
				properties.setProperty("mail.smtp.auth", Boolean.toString(Boolean.TRUE)); //$NON-NLS-1$
				properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE)); //$NON-NLS-1$
				properties.setProperty("mail.smtp.debug", Boolean.toString(Boolean.TRUE)); //$NON-NLS-1$

				Authenticator auth = new SMTPAuthenticator();
				this.mailSession = Session.getDefaultInstance(properties, auth);
			}

			// Create a default MimeMessage object.
			message = new MimeMessage(this.mailSession);
			// required":
			message.setFrom(new InternetAddress(this.mailFrom));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(this.mailTo));
			if (!StringUtils.isEmpty(this.mailCc)) { // Cc is optional:
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(this.mailCc));
			}
			if (!StringUtils.isEmpty(subject)) message.setSubject(subject);
			if (!StringUtils.isEmpty(content)) message.setText(content);

			Transport.send(message);
			log.info("Sent '{}'", msgDetails); //$NON-NLS-1$

		} catch (Exception ex) {
			String errDetails =
				String.format("Exception sending email to '%s' for node '%s' having changed from %s to %s " //$NON-NLS-1$
				              + "(mail-host:%s, port:%s, protocol:%s, user:'%s').", //$NON-NLS-1$
				              this.mailTo, node.getName(), oldState, newState,
				              SendMailAction.mailHost, SendMailAction.mailPort, SendMailAction.mailProtocol, SendMailAction.mailUser);
			throw new Exception(errDetails, ex);
		}
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator
	{
		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(SendMailAction.mailUser, SendMailAction.mailPwd);
		}
	}

	private class MailProperties extends NodeProperties
	{
		private static final long serialVersionUID = 3737364778153496635L;

		private final static String PlaceHolder_NodeName        = "NodeName"; //$NON-NLS-1$
		private final static String PlaceHolder_NodeStatusFrom  = "NodeStatusFrom"; //$NON-NLS-1$
		private final static String PlaceHolder_NodeStatusTo    = "NodeStatusTo"; //$NON-NLS-1$
		private final static String PlaceHolder_NodeDescription = "NodeDescription"; //$NON-NLS-1$

		private static final String PlaceHolder_ThresholdFailed = "ThresholdFailed"; //$NON-NLS-1$

		private static final String PlaceHolder_ThresholdDegraded = "ThresholdDegraded"; //$NON-NLS-1$

		private static final String PlaceHolder_ThresholdOK = "ThresholdOK"; //$NON-NLS-1$
		private static final String PlaceHolder_TargetAddress = "TargetAddress"; //$NON-NLS-1$

		private N node;
		private State oldState;
		private State newState;
		private ActionCheck check;

		void setData(N node, State oldState, State newState, ActionCheck check) {
			this.setParentProperties(node.getProperties());
			this.node = node;
			this.oldState = oldState;
			this.newState = newState;
		}


		String resolveProperties(String str) throws Exception {
			return PropertyResolver.resolveProperties(str, this);
		}

		@Override
		public String getProperty(String key) {
			switch (key) {
			case PlaceHolder_NodeName:
				return this.node.getName();
			case PlaceHolder_NodeStatusFrom:
				return this.oldState != null ? this.oldState.toString() : "---";  //$NON-NLS-1$
			case PlaceHolder_NodeStatusTo:
				return this.newState != null ? this.newState.toString() : "---";  //$NON-NLS-1$
			case PlaceHolder_ThresholdFailed:
				return this.check.getTriggerThresholds()[State.FAILED.ordinal()];
			case PlaceHolder_ThresholdDegraded:
				return this.check.getTriggerThresholds()[State.DEGRADED.ordinal()];
			case PlaceHolder_ThresholdOK:
				return this.check.getTriggerThresholds()[State.OK.ordinal()];
			case PlaceHolder_NodeDescription:
				return this.node.getDescription();
			case PlaceHolder_TargetAddress:
				if (this.node instanceof TCPConnection) {
					return ((TCPConnection)this.node).getResultingUrl();
				}
				if (this.node instanceof IPConnection) {
					return ((IPConnection)this.node).getResultingHostAddress();
				}
				if (this.node instanceof IPConnection) {
					return ((IPConnection)this.node).getResultingHostAddress();
				}

				//$FALL-THROUGH$
			default:
				return super.getProperty(key);
			}
		}
	}
}
