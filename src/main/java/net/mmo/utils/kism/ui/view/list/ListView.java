package net.mmo.utils.kism.ui.view.list;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.CompanyService;
import net.mmo.utils.kism.backend.service.ContactService;
import net.mmo.utils.kism.entities.Company;
import net.mmo.utils.kism.entities.Contact;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.CommonConstants.LayoutDirection;
import net.mmo.utils.kism.ui.MainLayout;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Route(value="list", layout = MainLayout.class)
@PageTitle("Contacts | " + CommonConstants.ApplicationFullName)
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
public class ListView extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "list-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "list-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "list-view-content";  //$NON-NLS-1$
	public static final String FilterFieldClassName = "list-view-filter";  //$NON-NLS-1$
	public static final String GridClassName        = "list-view-grid"; //$NON-NLS-1$

	private ContactService contactService;
	private CompanyService companyService;

	private TextField filterText = new TextField();
	Grid<Contact> grid = new Grid<>(Contact.class); // package visible for UTs - is there no other way?
	ContactForm form; // package visible for ITs - is there no other way?
	Div content;
	LayoutDirection contentDirection = LayoutDirection.direction_row;

	// This gets autowired but still puzzled which annotation causes this...
	public ListView(ContactService contactService,
	                CompanyService companyService) {
		log.debug("creating {}:", this.getClassName()); //$NON-NLS-1$
		this.contactService = contactService;
		this.companyService = companyService;

		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

		configureGrid();
		configureContactForm();

		log.trace("Creating content:"); //$NON-NLS-1$
		this.content = new Div(this.grid, this.form);
		this.content.addClassName(ViewContentClassName);
		this.content.setSizeFull();
		CommonConstants.setClassName(this.content, this.contentDirection);

		add(getToolbar(), this.content);

		updateList();
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	void addContact() {
		log.info("addContact"); //$NON-NLS-1$
		this.grid.asSingleSelect().clear();
		editContact(new Contact(), Messages.getString("ListView.Form.NewContact.Title")); //$NON-NLS-1$
	}

	private void saveContact(ContactForm.SaveEvent event) {
		log.info("saveContact"); //$NON-NLS-1$
		this.contactService.save(event.getContact());
		updateList();
		closeEditor();
	}

	private void deleteContact(ContactForm.DeleteEvent event) {
		log.info("deleteContact"); //$NON-NLS-1$
		this.contactService.delete(event.getContact());
		updateList();
		closeEditor();
	}

	private HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
		this.filterText.setClassName(FilterFieldClassName);
		this.filterText.setPlaceholder(Messages.getString("ListView.TextField.FilterText.Label")); //$NON-NLS-1$
		this.filterText.setClearButtonVisible(true);
		this.filterText.setValueChangeMode(ValueChangeMode.LAZY);
		this.filterText.addValueChangeListener(e -> updateList());

		Button addContactButton = new Button(Messages.getString("ListView.Button.Add_Contact.Label")); //$NON-NLS-1$
		addContactButton.addClickListener(click -> addContact());

		Button directionButton = new Button(String.format(Messages.getString("ListView.Button.Direction.Label"), LayoutDirection.invert(this.contentDirection).toString())); //$NON-NLS-1$
		directionButton.addClickListener(click ->
			{
				// We re-label the button with the current direction...
				directionButton.setText(String.format(Messages.getString("ListView.Button.Direction.Label"), this.contentDirection.toString())); //$NON-NLS-1$
				// ... before changing the direction:
				this.contentDirection = LayoutDirection.invert(this.contentDirection);
				CommonConstants.setClassName(this.content, this.contentDirection);
			});

		HorizontalLayout toolbar = new HorizontalLayout(this.filterText, addContactButton, directionButton);

		toolbar.addClassName(ToolbarClassName);
		return toolbar;
	}

	private void configureGrid() {
		log.trace("Configuring Grid:"); //$NON-NLS-1$
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.setColumns(Messages.getString("ListView.Grid.ColumnName.FirstName"), //$NON-NLS-1$
		                     Messages.getString("ListView.Grid.ColumnName.LastName"), //$NON-NLS-1$
		                     Messages.getString("ListView.Grid.ColumnName.Email"), //$NON-NLS-1$
		                     Messages.getString("ListView.Grid.ColumnName.Status")); //$NON-NLS-1$
		this.grid.addColumn(contact ->
			{
				Company company = contact.getCompany();
				return company == null ? "-" : company.getName(); //$NON-NLS-1$
			}).setHeader("Company"); //$NON-NLS-1$
		this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
		this.grid.asSingleSelect()
		         .addValueChangeListener(event -> editContact(event.getValue(), Messages.getString("ListView.Form.EditOrDeleteContact.Title"))); // getValue() returns the selected Contact or null if none is selected //$NON-NLS-1$
	}

	void configureContactForm() {
		this.form = new ContactForm(this.companyService.findAll());
		this.form.addListener(ContactForm.SaveEvent.class, this::saveContact);
		this.form.addListener(ContactForm.DeleteEvent.class, this::deleteContact);
		this.form.addListener(ContactForm.CloseEvent.class, e -> closeEditor());
		this.form.setVisible(false); // initially the ContactForm is not shown
	}

	public void editContact(Contact contact, String header) {
		log.info("editContact: {}", contact); //$NON-NLS-1$
		if (contact == null) {
			closeEditor();
		} else {
			this.form.setHeader(header);
			this.form.setContact(contact);
			this.form.setVisible(true);
			addClassName("editing"); //$NON-NLS-1$
		}
	}

	private void closeEditor() {
		log.info("closeEditor"); //$NON-NLS-1$
		this.form.setContact(null);
		this.form.setVisible(false);
		removeClassName("editing"); //$NON-NLS-1$
	}

	private void updateList() {
		this.grid.setItems(this.contactService.findAll(this.filterText.getValue()));
	}
}
