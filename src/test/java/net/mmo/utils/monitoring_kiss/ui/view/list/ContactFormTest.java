/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 16.10.2020
 */

package net.mmo.utils.monitoring_kiss.ui.view.list;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.mmo.utils.monitoring_kiss.entities.Company;
import net.mmo.utils.monitoring_kiss.entities.Contact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ContactFormTest
{
	private List<Company> companies;
	private Contact contact1;
	private Company company1;
	private Company company2;

	private final static String Company1Name = "Vaadin Ltd";
	private final static String Company2Name = "IT Mill";

	private final static String Contact1FirstName = "Marc";
	private final static String Contact1LastName  = "Usher";
	private final static String Contact1Email  = "marc@usher.com";
	private final static Contact.Status Contact1Status  = Contact.Status.NotContacted;

	private final static String Contact2FirstName = "John";
	private final static String Contact2LastName  = "Doe";
	private final static String Contact2Email  = "john@doe.com";
	private final static Contact.Status Contact2Status  = Contact.Status.Customer;

	@BeforeEach
	void setupData() {
		this.companies = new ArrayList<>();
		this.company1 = new Company(Company1Name);
		this.companies.add(this.company1);

		this.company2 = new Company(Company2Name);
		this.companies.add(this.company2);

		this.contact1 = new Contact();
		this.contact1.setFirstName(Contact1FirstName);
		this.contact1.setLastName(Contact1LastName);
		this.contact1.setEmail(Contact1Email);
		this.contact1.setStatus(Contact1Status);
		this.contact1.setCompany(this.company2);
	}

	@Test
	void checkFormFieldsArePopulated() {
		ContactForm form = new ContactForm(this.companies);
		form.setContact(this.contact1);
		Assertions.assertAll("Form-fields are not properly populated after form.setContact()",
		                     () -> Assertions.assertEquals(Contact1FirstName, form.firstName.getValue()),
		                     () -> Assertions.assertEquals(Contact1LastName, form.lastName.getValue()),
		                     () -> Assertions.assertEquals(Contact1Email, form.email.getValue()),
		                     () -> Assertions.assertEquals(Contact1Status, form.status.getValue()),
		                     () -> Assertions.assertEquals(this.company2, form.company.getValue()));
	}

	@Test
	void checkSaveEventHasCorrectValues() {
		ContactForm form = new ContactForm(this.companies);
		Contact contact = new Contact();
		form.setContact(contact); // form has empty values

		// populate the form:
		form.firstName.setValue(Contact2FirstName);
		form.lastName.setValue(Contact2LastName);
		form.company.setValue(this.company1);
		form.email.setValue(Contact2Email);
		form.status.setValue(Contact2Status);

		AtomicReference<Contact> savedContactRef = new AtomicReference<>(null);
		//define and attach the save-listener to the form:
		form.addListener(ContactForm.SaveEvent.class, e -> {
			// when save was clicked: memorize the contact
			savedContactRef.set(e.getContact());
		});
		// save the form:
		form.save.click();

		// check that we got what we expect to get:
		Contact savedContact = savedContactRef.get();
		Assertions.assertAll("Date received via SaveEvent are not the same as entered into Form-fields",
		                     () -> Assertions.assertEquals(Contact2FirstName, savedContact.getFirstName()),
		                     () -> Assertions.assertEquals(Contact2LastName, savedContact.getLastName()),
		                     () -> Assertions.assertEquals(Contact2Email, savedContact.getEmail()),
		                     () -> Assertions.assertEquals(Contact2Status, savedContact.getStatus()),
		                     () -> Assertions.assertEquals(this.company1, savedContact.getCompany()));
	}
}
