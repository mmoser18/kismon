/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions.impl;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.utils.StringUtils;

@SuppressWarnings("javadoc")
@ToString(includeFieldNames = true, callSuper = true)
@Slf4j
public class SendMailAction <N extends ActionableNode> implements IAction<N>
{
	// Recipient's email ID needs to be mentioned.
	String mailTo = "mms@elca.ch"; //$NON-NLS-1$
	// Sender's email ID needs to be mentioned
	String mailFrom = "mms@elca.ch"; //$NON-NLS-1$
	// Assuming you are sending email from localhost
	String mailHost;
	String mailUser;
	String mailPwd;

	String mailSubject;
	String mailText;

	@JsonIgnore
	Session mailSession;

	// we need a public constructor so that the ActionFactory can create this using <class>.newInstance();
	public SendMailAction() {
		// Setup mail server
	}

	@Override
	public Component getDetailsForm() {
		return new Label("This action is not yet implemented..."); //$NON-NLS-1$
	}

	@Override
	public void doAction(N node, State oldState, State newState) {
		try {
			String subject = node.resolveProperties(this.mailSubject);
			String content = node.resolveProperties(this.mailText);
			String msgDetails =
				String.format("email to '%s' (subject: '%s') for node '%s' having changed from %s to %s.", //$NON-NLS-1$
				              this.mailTo, subject, node.getName(), oldState, newState);
			log.info("Sending " + msgDetails); //$NON-NLS-1$

			if (this.mailSession == null) {
				Properties properties = System.getProperties();
				if (!StringUtils.isEmpty(this.mailUser) &&
					!StringUtils.isEmpty(this.mailPwd)) {
					properties.setProperty("mail.user", this.mailUser); //$NON-NLS-1$
					properties.setProperty("mail.password", this.mailPwd); //$NON-NLS-1$
				}
				properties.setProperty("mail.smtp.host", node.resolveProperties(this.mailHost)); //$NON-NLS-1$
				this.mailSession = Session.getDefaultInstance(properties);
			}

			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(this.mailSession);

			message.setFrom(new InternetAddress(this.mailFrom));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(this.mailTo));
			message.setSubject(subject);
			message.setText(content);
			try {
				Transport.send(message);
				log.info("Sent " + msgDetails); //$NON-NLS-1$
			} catch (Exception sex) {
				log.error("Exception sending " + msgDetails, sex); //$NON-NLS-1$
			}
		} catch (Exception ex) {
			log.error(String.format("Exception preparing email to '%s' for node '%s' having changed from %s to %s.", //$NON-NLS-1$
			                        this.mailTo, node.getName(), oldState, newState),
			          ex);
		}
	}
}
