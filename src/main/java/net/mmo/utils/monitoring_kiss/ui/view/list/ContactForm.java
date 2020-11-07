
package net.mmo.utils.monitoring_kiss.ui.view.list;

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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import net.mmo.utils.monitoring_kiss.backend.entity.Company;
import net.mmo.utils.monitoring_kiss.backend.entity.Contact;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContactForm extends FormLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// public to allow access by test classes
	public static final String ViewClassName = "contact-form"; 
	public static final String FirstNameClassName = "contact-form-first-name"; 
	public static final String LastNameClassName = "contact-form-last-name"; 
	public static final String EmailClassName = "contact-form-email"; 
	public static final String StatusClassName = "contact-form-status"; 
	
	Label header = new Label("Add or select a contact");
	TextField firstName = new TextField("First name");
	TextField lastName = new TextField("Last name");
	EmailField email = new EmailField("Email");

	ComboBox<Contact.Status> status = new ComboBox<>("Status");
	ComboBox<Company> company = new ComboBox<>("Company");

	Button save   = new Button("Save");
	Button delete = new Button("Delete");
	Button close  = new Button("Cancel");

	private Contact contact;
	Binder<Contact> binder = new BeanValidationBinder<>(Contact.class); // a Binder that is aware of bean validation annotations

	public ContactForm(List<Company> companies) {
		log.debug("Creating ContactForm:");
		addClassName(ViewClassName);
		binder.bindInstanceFields(this);

		firstName.addClassName(FirstNameClassName);
		lastName.addClassName(LastNameClassName);
		email.addClassName(EmailClassName);
		
		company.setItems(companies);
		company.setItemLabelGenerator(Company::getName);
		status.setItems(Contact.Status.values());
		status.addClassName(StatusClassName);

		add(header, firstName, lastName, email, company, status, createButtonsLayout());
	}

	public void setHeader(String header) {
		this.header.setText(header);
	}

	public void setContact(Contact contact) {
		this.contact = contact;
		binder.readBean(this.contact); // to bind the values from the contact to the UI fields. 
		// Note: readBean() copies the values from the Contact to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
	}

	private HorizontalLayout createButtonsLayout() {
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		save.setId("contactform-save-button");
		save.addClickShortcut(Key.ENTER);
		save.addClickListener(event -> validateAndSave());
		binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		save.setId("contactform-delete-button");
		delete.addClickListener(event -> fireEvent(new DeleteEvent(this, contact)));

		close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		close.setId("contactform-close-button");
		close.addClickShortcut(Key.ESCAPE);
		close.addClickListener(event -> fireEvent(new CloseEvent(this)));

		return new HorizontalLayout(save, delete, close);
	}

	private void validateAndSave() {
		log.info("Saving {}", contact);
		try {
			binder.writeBean(contact);
			fireEvent(new SaveEvent(this, contact));
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
			return contact;
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
