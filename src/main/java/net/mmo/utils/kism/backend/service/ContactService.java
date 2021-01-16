
package net.mmo.utils.kism.backend.service;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import net.mmo.utils.kism.backend.repositories.CompanyRepository;
import net.mmo.utils.kism.backend.repositories.ContactRepository;
import net.mmo.utils.kism.entities.Company;
import net.mmo.utils.kism.entities.Contact;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Contacts
 * Also initializes these using some random data.
 */
@Service
public class ContactService
{
	private static final Logger LOGGER = Logger.getLogger(ContactService.class.getName());
	private ContactRepository contactRepository;
	private CompanyRepository companyRepository;

	public ContactService(ContactRepository contactRepository, CompanyRepository companyRepository) {
		this.contactRepository = contactRepository;
		this.companyRepository = companyRepository;
	}

	public List<Contact> findAll() {
		return this.contactRepository.findAll();
	}

	public List<Contact> findAll(String stringFilter) {
		if (stringFilter == null || stringFilter.isEmpty()) {
			return this.contactRepository.findAll();
		} else {
			return this.contactRepository.search(stringFilter);
		}
	}

	public long count() {
		return this.contactRepository.count();
	}

	public void delete(Contact contact) {
		this.contactRepository.delete(contact);
	}

	public void save(Contact contact) {
		if (contact == null) {
			LOGGER.severe("Contact is null. Are you sure you have connected your form to the application?"); //$NON-NLS-1$
			return;
		}
		this.contactRepository.save(contact);
	}

	@PostConstruct
	public void populateTestData() {
		if (this.companyRepository.count() == 0) {
			this.companyRepository.saveAll(Stream.of("Path-Way Electronics", "E-Tech Management", "Path-E-Tech Management") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                          .map(Company::new)
			                          .collect(Collectors.toList()));
		}
		if (this.contactRepository.count() == 0) {
			Random r = new Random(0);
			List<Company> companies = this.companyRepository.findAll();
			this.contactRepository.saveAll(Stream.of("Gabrielle Patel", "Brian Robinson", "Eduardo Haugen", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                                         "Koen Johansen", "Alejandro Macdonald", "Angel Karlsson", "Yahir Gustavsson", "Haiden Svensson", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			                                         "Emily Stewart", "Corinne Davis", "Ryann Davis", "Yurem Jackson", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			                                         "Kelly Gustavsson", //$NON-NLS-1$
			                                         "Eileen Walker", "Katelyn Martin", "Israel Carlsson", "Quinn Hansson", "Makena Smith", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			                                         "Danielle Watson", "Leland Harris", "Gunner Karlsen", "Jamar Olsson", "Lara Martin", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			                                         "Ann Andersson", "Remington Andersson", "Rene Carlsson", "Elvis Olsen", "Solomon Olsen", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			                                         "Jaydan Jackson", "Bernard Nilsen") //$NON-NLS-1$ //$NON-NLS-2$
			                               .map(name ->
			                               {
			                            	   String[] split = name.split(" "); //$NON-NLS-1$
			                            	   Contact contact = new Contact();
			                            	   contact.setFirstName(split[0]);
			                            	   contact.setLastName(split[1]);
			                            	   contact.setCompany(companies.get(r.nextInt(companies.size())));
			                            	   contact.setStatus(Contact.Status.values()[r.nextInt(Contact.Status.values().length)]);
			                            	   String email = (contact.getFirstName() + "." + contact.getLastName() //$NON-NLS-1$
			                            	                  + "@" + contact.getCompany().getName().replaceAll("[\\s-]", "") + ".com").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			                            	   contact.setEmail(email);
			                            	   return contact;
			                               })
			                               .collect(Collectors.toList()));
		}
	}
}
