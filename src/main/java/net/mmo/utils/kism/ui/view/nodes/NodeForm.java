
package net.mmo.utils.kism.ui.view.nodes;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.Node;

@Slf4j
public abstract class NodeForm <N extends Node> extends VerticalLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// public to allow access by test classes
	public static final String ViewClassName = "node-form"; //$NON-NLS-1$
	public static final String FieldsClassName = "node-form-fields"; //$NON-NLS-1$
	public static final String NameClassName        = "node-form-field-name"; //$NON-NLS-1$
	public static final String DescriptionClassName = "node-form-field-description"; //$NON-NLS-1$
	public static final String StateClassName       = "node-form-field-state"; //$NON-NLS-1$
	public static final String ActiveClassName      = "node-form-field-active"; //$NON-NLS-1$
	public static final String PropertiesClassName  = "node-form-field-properties"; //$NON-NLS-1$
	public static final String CombinedClassName    = "node-form-field-combined"; //$NON-NLS-1$

	public static final String LabelPaddingTheme    = "label-padding"; //$NON-NLS-1$

	protected Label header = new Label(Messages.getString("NodeForm.Label.Header.Text")); //$NON-NLS-1$

	protected TextField name;
	protected TextArea description;
	// state is calculated automatically!
	protected TextField state;
	protected Checkbox active;
	protected TextArea properties;

	protected VerticalLayout fields;

	private Button save   = new Button(Messages.getString("NodeForm.Button.Save.Label")); //$NON-NLS-1$
	private Button delete = new Button(Messages.getString("NodeForm.Button.Delete.Label")); //$NON-NLS-1$
	private Button reset  = new Button(Messages.getString("NodeForm.Button.Reset.Label")); //$NON-NLS-1$
	private Button close  = new Button(Messages.getString("NodeForm.Button.Cancel.Label")); //$NON-NLS-1$

	protected N node;
	protected Binder<N> binder; // a Binder that is aware of bean validation annotations

	public NodeForm(Class<N> clazz) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		addClassName(ViewClassName);

		this.binder = new BeanValidationBinder<>(clazz);

		createFormFields();

//		this.binder.forField(this.nameField)
//		.withValidator(name ->
//			{ // Explicit validator instance
//				return nodeService.getRootNodes().stream()
//					.filter(rootNode -> rootNode != this.node) // not the same rootnode?
//					.filter(rootNode -> rootNode.getName().equals(name)) // same name as any other rootnode?
//					.findAny().isEmpty();
//			}, Messages.getString("NodeForm.Validator.RootName.MustBeUnique")) //$NON-NLS-1$
//		.bind(Node::getName, Node::setName);

		this.binder.forField(this.state).bind(n -> n.getState().toString(), null);
		this.binder.forField(this.properties).bind(Node::getPropertiesAsString, Node::setPropertiesFromString);

		this.binder.bindInstanceFields(this); // this binder-setup has to be AFTER creation of the form field
		                                      // or else the later readBean/writeBean won't work!
		                                      // Took me a day to find out why the binding was a No-op... ||-(

		add(this.header, this.fields, createButtonsLayout());
	}

	public void setHeader(String header) {
		this.header.setText(header);
	}

	public void setNode(N node) {
		log.debug("NodeForm.setNode({})", node); //$NON-NLS-1$
		this.node = node;
		this.binder.readBean(this.node); // to bind the values from the node to the UI fields.
		// Note: readBean() copies the values from the bean to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
	}

	public N getNode() {
		return this.node;
	}
	/** derived classes need to override this */
	protected void createFormFields() {
		this.name = new TextField(Messages.getString("NodeForm.Name.Label")); //$NON-NLS-1$
		this.name.addClassName(NameClassName);
		this.name.setThemeName(LabelPaddingTheme);

		this.description = new TextArea(Messages.getString("NodeForm.Description.Label")); //$NON-NLS-1$
		this.description.addClassName(DescriptionClassName);
		this.description.setThemeName(LabelPaddingTheme);

		this.state = new TextField(Messages.getString("NodeForm.State.Label")); //$NON-NLS-1$
		this.state.addClassName(StateClassName);
		this.state.setReadOnly(true); // state is calculated automatically!

		this.active  = new Checkbox(Messages.getString("NodeForm.Active.Label")); //$NON-NLS-1$
		this.active.addClassName(ActiveClassName);

		HorizontalLayout stateAndActive = new HorizontalLayout(this.active, this.state);
		stateAndActive.addClassName(CombinedClassName);

		this.properties = new TextArea(Messages.getString("NodeForm.Properties.Label")); //$NON-NLS-1$
		this.properties.addClassName(PropertiesClassName);
		this.properties.setThemeName(LabelPaddingTheme);

		this.fields = new VerticalLayout(this.name, this.description, stateAndActive, this.properties);
		this.fields.setClassName(FieldsClassName);
		// this.fields.setSizeFull();
		this.fields.setWidthFull();
		this.fields.setMargin(false);
	}

	private HorizontalLayout createButtonsLayout() {
		this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.save.setId("nodeform-save-button"); //$NON-NLS-1$
		this.save.addClickShortcut(Key.ENTER);
		this.save.addClickListener(event -> validateAndSave());
		this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));

		this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.delete.setId("nodeform-delete-button"); //$NON-NLS-1$
		this.delete.addClickListener(event -> fireEvent(new DeleteEvent(this, this.node)));

		// Updates the fields again with the previously saved values
		this.reset.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		this.reset.setId("nodeform-reset-button"); //$NON-NLS-1$
		this.reset.addClickListener(event -> this.binder.readBean(this.node));

		this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		this.close.setId("nodeform-close-button"); //$NON-NLS-1$
		this.close.addClickShortcut(Key.ESCAPE);
		this.close.addClickListener(event -> fireEvent(new CloseEvent(this)));

		return new HorizontalLayout(this.save, this.delete, this.reset, this.close);
	}

	protected void validateAndSave() {
		log.info("validateAndSave {}", this.node.getName()); //$NON-NLS-1$
		try {
			validate(); // throws ValidationException if data not valid
			this.binder.writeBean(this.node);
			fireEvent(new SaveEvent(this, this.node));
		} catch (ValidationException e) {
			log.warn("Validation failed: ", e); //$NON-NLS-1$
			new Notification(e.getMessage(), 0).open();
		}
	}

	@SuppressWarnings("unused")
	protected void validate() throws ValidationException {
		// TODO: more validation?
	}

	// Events - shared super class
	@SuppressWarnings("rawtypes") // this class must NOT be parameterized or else it an not be used in lambdas
	public abstract static class NodeFormEvent extends ComponentEvent<NodeForm>
	{
		private static final long serialVersionUID = -7044223229837911727L;

		private Node node;

		protected NodeFormEvent(NodeForm source, Node node) {
			super(source, false);
			this.node = node;
		}

		public Node getNode() {
			return this.node;
		}
	}

	@SuppressWarnings("rawtypes") // this class must NOT be parameterized or else it an not be used in lambdas
	public static class SaveEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -8255534604198398835L;

		SaveEvent(NodeForm source, Node node) {
			super(source, node);
		}
	}
	@SuppressWarnings("rawtypes") // this class must NOT be parameterized or else it an not be used in lambdas
	public static class DeleteEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -5394186746319400182L;

		DeleteEvent(NodeForm source, Node node) {
			super(source, node);
		}
	}
	@SuppressWarnings("rawtypes") // this class must NOT be parameterized or else it an not be used in lambdas
	public static class CloseEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -657451450305155687L;

		CloseEvent(NodeForm source) {
			super(source, null);
		}
	}

	@Override
	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
	                                                              ComponentEventListener<T> listener) {
		return (!getEventBus().hasListener(eventType) // note the NOT!
		        ? getEventBus().addListener(eventType, listener)
		        : null);
	}
}