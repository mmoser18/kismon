/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31.08.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.SendMailAction;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionDetailsForm;
import net.mmo.utils.kism.utils.StringUtils;
import org.slf4j.Logger;
/**
 * the form for the SendMail-parameters
 */
@Slf4j
@SuppressWarnings("javadoc")
public class SendMailForm <N extends ActionableNode> extends VerticalLayout implements ActionDetailsForm
{
	private static final long serialVersionUID = 7901853154069994172L;

	public static final String FormClassName = "send-mail-form";  //$NON-NLS-1$
	public static final String MailToClassName = FormClassName + "-to";  //$NON-NLS-1$
	public static final String MailCcClassName = FormClassName + "-cc";  //$NON-NLS-1$
	public static final String MailFromClassName = FormClassName + "-from";  //$NON-NLS-1$
	public static final String MailSubjectClassName = FormClassName + "-subject";  //$NON-NLS-1$
	public static final String MailContentClassName = FormClassName + "-content";  //$NON-NLS-1$


	protected TextField mailTo;
	protected TextField mailCc;
	protected TextField mailFrom;
	protected TextField mailSubject;
	protected TextArea  mailContent;

	Binder<IAction<ActionableNode>> binder;

	/**
	 * must be public to be accessible by the factory
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public SendMailForm() {
		addClassName(FormClassName);

		String ttl = Messages.getString("SendMailForm.Title"); //$NON-NLS-1$
		if (!StringUtils.isEmpty(ttl)) add(new NativeLabel(ttl)); // we allow this to be empty
		this.mailTo   = new TextField(Messages.getString("SendMailForm.Recipient.Label")); //$NON-NLS-1$
		this.mailTo.addClassName(MailToClassName);
		this.mailTo.setThemeName(UIConstants.LabelPaddingTheme);

		this.mailCc   = new TextField(Messages.getString("SendMailForm.Cc.Label")); //$NON-NLS-1$
		this.mailCc.addClassName(MailCcClassName);
		this.mailCc.setThemeName(UIConstants.LabelPaddingTheme);

		this.mailFrom = new TextField(Messages.getString("SendMailForm.Sender.Label")); //$NON-NLS-1$
		this.mailFrom.addClassName(MailFromClassName);
		this.mailFrom.setThemeName(UIConstants.LabelPaddingTheme);

		this.mailSubject = new TextField(Messages.getString("SendMailForm.Subject.Label")); //$NON-NLS-1$
		this.mailSubject.addClassName(MailSubjectClassName);
		this.mailSubject.setThemeName(UIConstants.LabelPaddingTheme);

		this.mailContent = new TextArea(Messages.getString("SendMailForm.Content.Label")); //$NON-NLS-1$
		this.mailContent.addClassName(MailContentClassName);
		this.mailContent.setThemeName(UIConstants.LabelPaddingTheme);

		add(this.mailTo, this.mailCc, this.mailFrom, this.mailSubject, this.mailContent);

		this.binder = new BeanValidationBinder(SendMailAction.class);
		this.binder.bindInstanceFields(this);
	}

	@Override
	public Binder<IAction<ActionableNode>> getBinder() {
		return this.binder;
	}

	@Override
	public Logger getLogger() {
		return log;
	}
}
