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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.LeafNode;
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
//disabled since this doesn't compile with the newest version ("Lombok annotation handler class lombok.eclipse.handlers.HandleToString failed"):
//@ToString(includeFieldNames = true, callSuper = true)
@ToString
@Setter
@Getter
@Slf4j
public class SendMailAction <N extends ActionableNode> implements IAction<N>
{
	private static final long serialVersionUID = 4615050152823304964L;

	@NotEmpty
	String mailTo;
	String mailCc;
	@NotEmpty
	String mailFrom;
	@NotEmpty
	@NotBlank
	String mailSubject;
	String mailContent;

	// Assuming you are sending email from localhost:
	static String mailHost;
	static String mailPort;

	static String mailProt;
	static String mailUser;
	static String mailPwd;

	static String auth;
	static String starttls;
	static String debug;
	static String mailAddrDelims;

	static {
		Properties appProperties = AppProperties.getProperties();
		mailHost		= appProperties.getProperty("SendMailAction.mailHost");							//$NON-NLS-1$
		mailPort		= appProperties.getProperty("SendMailAction.mailPort");							//$NON-NLS-1$
		mailProt		= appProperties.getProperty("SendMailAction.mailProtocol");						//$NON-NLS-1$
		mailUser		= appProperties.getProperty("SendMailAction.mailUser");							//$NON-NLS-1$
		mailPwd			= appProperties.getProperty("SendMailAction.mailPwd");							//$NON-NLS-1$
		auth			= appProperties.getProperty("SendMailAction.auth", Boolean.toString(false));	//$NON-NLS-1$
		starttls		= appProperties.getProperty("SendMailAction.starttls", Boolean.toString(false));	//$NON-NLS-1$
		debug			= appProperties.getProperty("SendMailAction.debug", Boolean.toString(true));	//$NON-NLS-1$
		mailAddrDelims	= appProperties.getProperty("SendMailAction.mailAddrDelims", "[,; ]");		//$NON-NLS-1$ //$NON-NLS-2$
	}

	@JsonIgnore
	MailProperties mailProperties = new MailProperties();

	@JsonIgnore
	transient Session mailSession;
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
	public void doAction(N node, State oldState, State newState) throws Exception {
		String test = newState == null ? "(manually triggered test)" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		this.mailProperties.setData(node, oldState, newState, node.getActionCheck());

		try {
			MimeMessage message;
			String subject = this.mailProperties.resolveProperties(this.mailSubject);
			String content = this.mailProperties.resolveProperties(this.mailContent);
			if (test.length() > 0) {
				content = test + "\n" + content; //$NON-NLS-1$
			}

			String msgDetails =
				String.format("email to '%s' (subject: '%s') for node '%s' having changed from %s to %s. %s", //$NON-NLS-1$
				              this.mailTo, subject, node.getName(), oldState, newState, test);
			log.info("Sending " + msgDetails); //$NON-NLS-1$
			if (this.mailSession == null) {
				setSystemPropertyIfNotEmpty(node, "mail.user", SendMailAction.mailUser, "anonymous"); //$NON-NLS-1$ //$NON-NLS-2$
				setSystemPropertyIfNotEmpty(node, "mail.password", SendMailAction.mailPwd,null); //$NON-NLS-1$
				setSystemPropertyIfNotEmpty(node, "mail.smtp.host", SendMailAction.mailHost,null); //$NON-NLS-1$
				setSystemPropertyIfNotEmpty(node, "mail.smtp.port", SendMailAction.mailPort,null); //$NON-NLS-1$
				setSystemPropertyIfNotEmpty(node, "mail.smtp.protocol", SendMailAction.mailProt,null); //$NON-NLS-1$
				Properties properties = System.getProperties();
				properties.setProperty("mail.smtp.auth", SendMailAction.auth); //$NON-NLS-1$
				properties.setProperty("mail.smtp.starttls.enable", SendMailAction.starttls); //$NON-NLS-1$
				properties.setProperty("mail.smtp.debug", SendMailAction.debug); //$NON-NLS-1$
				Authenticator authenticator = new SMTPAuthenticator();
				this.mailSession = Session.getDefaultInstance(properties, authenticator);
			}

			// Create a default MimeMessage object.
			message = new MimeMessage(this.mailSession);
			// required:
			String value = resolveMailProperties(node, this.mailFrom);
			message.setFrom(new InternetAddress(value));

			value = resolveMailProperties(node,this.mailTo);
			for (String recipient: value.split(mailAddrDelims)) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			}
			value = resolveMailProperties(node,this.mailCc);
			if (!StringUtils.isEmpty(value)) { // Cc is optional:
				for (String recipient: value.split(mailAddrDelims)) {
					message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipient));
				}
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
				              SendMailAction.mailHost, SendMailAction.mailPort, SendMailAction.mailProt, SendMailAction.mailUser);
			throw new Exception(errDetails, ex);
		}
	}

	private void setSystemPropertyIfNotEmpty(N node, String propName, String value, String dflt) {
		if (!StringUtils.isEmpty(value)) {
			System.getProperties().setProperty(propName, value);
		} else if (dflt != null) {
			System.getProperties().setProperty(propName, dflt);
		}
	}

	private String resolveMailProperties(N node, String value) throws Exception {
		String res = this.mailProperties.resolveProperties(value).replaceAll("\\t|\\n|\\r| ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		log.info("node: '{}', key: '{}' --> {}", node, value, res); //$NON-NLS-1$
		return res;
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

		private static final String PlaceHolder_SameStateCounter = "SameStatusCounter"; //$NON-NLS-1$

		private static final String PlaceHolder_ThresholdFailed = "ThresholdForFailed"; //$NON-NLS-1$
		private static final String PlaceHolder_ThresholdDegraded = "ThresholdForDegraded"; //$NON-NLS-1$
		private static final String PlaceHolder_ThresholdOK = "ThresholdForOK"; //$NON-NLS-1$
		private static final String PlaceHolder_ApplicableThreshold = "ApplicableThreshold"; //$NON-NLS-1$
		private static final String PlaceHolder_ApplicableTimespan = "ApplicableTimespan"; //$NON-NLS-1$

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
			this.check = check;
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
			case PlaceHolder_ApplicableThreshold:
				return this.newState != null
				       ? this.check.getTriggerThresholdString(this.newState)
				       : "<test|new-state-not-available>"; //$NON-NLS-1$
			case PlaceHolder_ApplicableTimespan:
				return (this.node instanceof LeafNode)
				        ? (this.newState != null)
				          ? Integer.toString(this.check.getTriggerThresholdNumeric(this.newState)
				                             * ((LeafNode)this.node).getPeriod())
				          : "<'" + PlaceHolder_ApplicableTimespan + "' not computable (period is " + ((LeafNode)this.node).getPeriod() + ")>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				        : "<'" + PlaceHolder_ApplicableTimespan + "' is only available for leafnodes>"; //$NON-NLS-1$ //$NON-NLS-2$
			case PlaceHolder_SameStateCounter:
				return Integer.toString(this.node.getSameStateSince());
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
