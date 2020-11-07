package net.mmo.utils.monitoring_kiss.ui.view.list;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.mmo.utils.monitoring_kiss.backend.entity.Company;
import net.mmo.utils.monitoring_kiss.backend.entity.Contact;
import net.mmo.utils.monitoring_kiss.backend.service.CompanyService;
import net.mmo.utils.monitoring_kiss.backend.service.ContactService;
import net.mmo.utils.monitoring_kiss.ui.CommonConstants;
import net.mmo.utils.monitoring_kiss.ui.MainLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Route(value="", layout = MainLayout.class)
@PageTitle("Contacts | " + CommonConstants.ApplicationFullName)
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
public class ListView extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "list-view"; 
	public static final String GridClassName = "list-view-grid";
	public static final String FilterFieldClassName = "list-view-filter"; 

	private ContactService contactService;
	private CompanyService companyService;

	private TextField filterText = new TextField();
	Grid<Contact> grid = new Grid<>(Contact.class); // package visible for UTs - is there no other way?
	ContactForm form; // package visible for ITs - is there no other way?
	
	// This gets autowired but still puzzled which annotation causes this...
	public ListView(ContactService contactService, 
	                CompanyService companyService) {
		log.debug("creating ListView:");
		this.contactService = contactService;
		this.companyService = companyService;
		
		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window
		
		configureGrid();
		
		form = new ContactForm(this.companyService.findAll());
		form.addListener(ContactForm.SaveEvent.class, this::saveContact);
		form.addListener(ContactForm.DeleteEvent.class, this::deleteContact);
		form.addListener(ContactForm.CloseEvent.class, e -> closeEditor());
		form.setVisible(false); // initially the ContactForm is not shown
		
		log.trace("Creating content:");
		Div content = new Div(grid, form);
		content.addClassName("content");
		content.setSizeFull();
		
		add(getToolbar(), content);
		
		updateList();
		log.debug("ListView complete.");
	}
	
	void addContact() {
		log.info("addContact");
		grid.asSingleSelect().clear();
		editContact(new Contact(), "new contact:");
	}
	
	private void saveContact(ContactForm.SaveEvent event) {
		log.info("saveContact");
		contactService.save(event.getContact());
		updateList();
		closeEditor();
	}

	private void deleteContact(ContactForm.DeleteEvent event) {
		log.info("deleteContact");
		contactService.delete(event.getContact());
		updateList();
		closeEditor();
	}

	private HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:");
		filterText.setClassName(FilterFieldClassName);
		filterText.setPlaceholder("Filter by name...");
		filterText.setClearButtonVisible(true);
		filterText.setValueChangeMode(ValueChangeMode.LAZY);
		filterText.addValueChangeListener(e -> updateList());
		
		Button addContactButton = new Button("Add contact");
		addContactButton.addClickListener(click -> addContact());
		
		HorizontalLayout toolbar = new HorizontalLayout(filterText, addContactButton);
		toolbar.addClassName("toolbar");
		return toolbar;
	}
	
	private void configureGrid() {
		log.trace("Configuring Grid:");
		grid.addClassName(GridClassName);
		grid.setSizeFull();
		grid.setColumns("firstName", "lastName", "email", "status");
		grid.addColumn(contact -> 
			{
				Company company = contact.getCompany();
				return company == null ? "-" : company.getName();
			}).setHeader("Company");
		grid.getColumns().forEach(col -> col.setAutoWidth(true));
		grid.asSingleSelect()
			.addValueChangeListener(event -> editContact(event.getValue(), "Edit or delete contact:")); // getValue() returns the selected Contact or null if none is selected
	}

	public void editContact(Contact contact, String header) {
		log.info("editContact: {}", contact);
		if (contact == null) {
			closeEditor();
		} else {
			form.setHeader(header);
			form.setContact(contact);
			form.setVisible(true);
			addClassName("editing");
		}
	}

	private void closeEditor() {
		log.info("closeEditor");
		form.setContact(null);
		form.setVisible(false);
		removeClassName("editing");
	}
	
	private void updateList() {
		grid.setItems(contactService.findAll(filterText.getValue()));
	}
}
