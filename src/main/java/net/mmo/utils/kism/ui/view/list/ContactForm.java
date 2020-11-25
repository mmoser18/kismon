
package net.mmo.utils.kism.ui.view.list;

import java.util.List;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.Company;
import net.mmo.utils.kism.entities.Contact;

@Slf4j
public class ContactForm extends FormLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// public to allow access by test classes
	public static final String ViewClassName = "contact-form"; //$NON-NLS-1$
	public static final String FieldsClassName = "contact-form-fields"; //$NON-NLS-1$
	public static final String FirstNameClassName = "contact-form-first-name"; //$NON-NLS-1$
	public static final String LastNameClassName  = "contact-form-last-name"; //$NON-NLS-1$
	public static final String EmailClassName     = "contact-form-email"; //$NON-NLS-1$
	public static final String StatusClassName    = "contact-form-status"; //$NON-NLS-1$

	Label header = new Label(Messages.getString("ContactForm.Label.Header.Text")); //$NON-NLS-1$

	TextField firstName = new TextField(Messages.getString("ContactForm.TextField.FirstName.Label")); //$NON-NLS-1$
	TextField lastName = new TextField(Messages.getString("ContactForm.TextField.LastName.Label")); //$NON-NLS-1$
	EmailField email = new EmailField(Messages.getString("ContactForm.EmailField.Email.Label")); //$NON-NLS-1$
	ComboBox<Contact.Status> status = new ComboBox<>(Messages.getString("ContactForm.ComboBox.Status.Label")); //$NON-NLS-1$
	ComboBox<Company> company = new ComboBox<>(Messages.getString("ContactForm.ComboBox.Company.Label")); //$NON-NLS-1$

	VerticalLayout fields;

	Button save   = new Button(Messages.getString("ContactForm.Button.Save.Label")); //$NON-NLS-1$
	Button delete = new Button(Messages.getString("ContactForm.Button.Delete.Label")); //$NON-NLS-1$
	Button close  = new Button(Messages.getString("ContactForm.Button.Cancel.Label")); //$NON-NLS-1$

	private Contact contact;
	Binder<Contact> binder = new BeanValidationBinder<>(Contact.class); // a Binder that is aware of bean validation annotations

	public ContactForm(List<Company> companies) {
		log.debug("Creating ContactForm:"); //$NON-NLS-1$
		addClassName(ViewClassName);
		this.binder.bindInstanceFields(this);


		this.firstName.addClassName(FirstNameClassName);
		this.lastName.addClassName(LastNameClassName);
		this.email.addClassName(EmailClassName);

		this.company.setItems(companies);
		this.company.setItemLabelGenerator(Company::getName);
		this.status.setItems(Contact.Status.values());
		this.status.addClassName(StatusClassName);

		this.fields = new VerticalLayout(this.firstName, this.lastName, this.email, this.company, this.status);
		this.fields.setClassName(FieldsClassName);
		this.fields.setSizeFull();

		add(this.header, this.fields, createButtonsLayout());
	}

	public void setHeader(String header) {
		this.header.setText(header);
	}

	public void setContact(Contact contact) {
		this.contact = contact;
		this.binder.readBean(this.contact); // to bind the values from the contact to the UI fields.
		// Note: readBean() copies the values from the Contact to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
	}

	private HorizontalLayout createButtonsLayout() {
		this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.save.setId("contactform-save-button"); //$NON-NLS-1$
		this.save.addClickShortcut(Key.ENTER);
		this.save.addClickListener(event -> validateAndSave());
		this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));

		this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.save.setId("contactform-delete-button"); //$NON-NLS-1$
		this.delete.addClickListener(event -> fireEvent(new DeleteEvent(this, this.contact)));

		this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		this.close.setId("contactform-close-button"); //$NON-NLS-1$
		this.close.addClickShortcut(Key.ESCAPE);
		this.close.addClickListener(event -> fireEvent(new CloseEvent(this)));

		return new HorizontalLayout(this.save, this.delete, this.close);
	}

	private void validateAndSave() {
		log.info("Saving {}", this.contact); //$NON-NLS-1$
		try {
			this.binder.writeBean(this.contact);
			fireEvent(new SaveEvent(this, this.contact));
		} catch (ValidationException e) {
			e.printStackTrace();
		}
	}

	// Events - shared super class
	public abstract static class ContactFormEvent extends ComponentEvent<ContactForm>
	{
		private static final long serialVersionUID = 6565504237382800366L;
		private Contact contact;

		protected ContactFormEvent(ContactForm source, Contact contact) {
			super(source, false);
			this.contact = contact;
		}

		public Contact getContact() {
			return this.contact;
		}
	}

	public static class SaveEvent extends ContactFormEvent
	{
		private static final long serialVersionUID = 7901067473147616654L;

		SaveEvent(ContactForm source, Contact contact) {
			super(source, contact);
		}
	}
	public static class DeleteEvent extends ContactFormEvent
	{
		private static final long serialVersionUID = 6365902203761799749L;

		DeleteEvent(ContactForm source, Contact contact) {
			super(source, contact);
		}
	}
	public static class CloseEvent extends ContactFormEvent
	{
		private static final long serialVersionUID = -657451450305155687L;

		CloseEvent(ContactForm source) {
			super(source, null);
		}
	}

	@Override
	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
	                                                              ComponentEventListener<T> listener) {
		return getEventBus().addListener(eventType, listener);
	}
}
