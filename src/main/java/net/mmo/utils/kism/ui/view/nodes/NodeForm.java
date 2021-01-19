
package net.mmo.utils.kism.ui.view.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import java.util.function.BiConsumer;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import net.mmo.utils.kism.utils.NodeProperties;

@Slf4j
public abstract class NodeForm <N extends Node> extends VerticalLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// package visible to allow access by test classes
	static final String ViewClassName = "node-form"; //$NON-NLS-1$
	static final String FieldsClassName = "node-form-fields"; //$NON-NLS-1$
	static final String NameClassName        = NodeFormField + "name"; //$NON-NLS-1$
	static final String DescriptionClassName = NodeFormField + "description"; //$NON-NLS-1$
	static final String StateClassName       = NodeFormField + "state"; //$NON-NLS-1$
	static final String ActiveClassName      = NodeFormField + "active"; //$NON-NLS-1$
	static final String PropertiesClassName  = NodeFormField + "properties"; //$NON-NLS-1$

	/** class name to mark combined fields */
	public static final String CombinedClassName    = "node-form-field-combined"; //$NON-NLS-1$

	/** theme use for fields that should have its label closer to the value field than default */
	public static final String LabelPaddingTheme    = "label-padding"; //$NON-NLS-1$
	/** theme use for fields that should be displayed in monospaced font */
	public static final String MonospaceTheme   = "monospace"; //$NON-NLS-1$

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

		this.binder.bindInstanceFields(this); // this binder-setup has to be AFTER creation of the form fields
		                                      // or else the later readBean/writeBean won't work!
		                                      // Took me a day to find out why the binding was a No-op... ||-(

		add(this.header, this.fields, createButtonsLayout());
	}

	public void setHeader(String header) {
		this.header.setText(header);
	}

	public boolean setNode(N node) {
		log.debug("NodeForm.setNode({})", node); //$NON-NLS-1$
		if (this.node != null) {
			if (this.binder.hasChanges()) {
				log.debug("binder has changes!"); //$NON-NLS-1$
				new ConfirmDialog("There are unsaved changes!", //$NON-NLS-1$
				                  "Are you sure you want to leave this node?", //$NON-NLS-1$
				                  "Save node", (confirmEvent) -> { confirmEvent.getSource().close(); validateAndSave(); }, //$NON-NLS-1$
				                  "Abandon changes", (rejectEvent) -> { rejectEvent.getSource().close(); readBean(this.node); }, //$NON-NLS-1$
				                  "Stay on node", (cancelEvent)  -> { /*nothing to do*/ } //$NON-NLS-1$
				                  ).open();
				return false;
			}
			this.node.setChangeListener(null);
			log.info("changeListener removed on node {}", this.node); //$NON-NLS-1$
		}
		this.node = node;
		log.debug("NodeForm.setNode({}) - new node accepted!", node); //$NON-NLS-1$
		// this.binder.setBean(node);
		if (node != null) {
			this.node.setChangeListener(event ->
				{
					@SuppressWarnings("unchecked")
					N sourceNode = (N)event.getSource();
					String propertyName = event.getPropertyName();
					Object oldValue = event.getOldValue();
					Object newValue = event.getNewValue();
					log.info("changeListener called for '{}': '{}' -> '{}' for node '{}'", propertyName, oldValue, newValue, sourceNode); //$NON-NLS-1$

					getUI().ifPresentOrElse((ui) -> ui.access(() -> handleChangeEvent(sourceNode, propertyName, oldValue, newValue)),
					                        () -> log.warn("UI not present for changeListener for node {}", node)); //$NON-NLS-1$
				});
			log.info("changeListener set on node {}", node); //$NON-NLS-1$
			updateResolvableValues();
		}
		// Note: readBean() copies the values from the bean to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
		readBean(this.node); // to bind the values from the node to the UI fields.
		return true;
	}

	/** to be extended by subclasses if they handle further property-updates */
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case Node.PROPERTYNAME_STATE:
			this.state.setValue(newValue.toString());
			break;
		default:
			// ignore
			// NodeView.ProgrammingError(String.format("Unexpected property name '%s' for %s", propertyName, sourceNode), new Exception("dummy to obtain stacktrace")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fireEvent(new UpdateEvent(this, this.node));
	}

	/** derived classes need to override this */
	protected void createFormFields() {
		this.name = new TextField(Messages.getString("NodeForm.Name.Label")); //$NON-NLS-1$
		this.name.addClassName(NameClassName);
		this.name.setThemeName(LabelPaddingTheme);
//		this.binder.forField(this.nameField)
//			.withValidator(name ->
//				{ // Explicit validator instance
//					return nodeService.getRootNodes().stream()
//						.filter(rootNode -> rootNode != this.node) // not the same rootnode?
//						.filter(rootNode -> rootNode.getName().equals(name)) // same name as any other rootnode?
//						.findAny().isEmpty();
//				}, Messages.getString("NodeForm.Validator.RootName.MustBeUnique")) //$NON-NLS-1$
//			.bind(Node::getName, Node::setName);

		this.description = new TextArea(Messages.getString("NodeForm.Description.Label")); //$NON-NLS-1$
		this.description.addClassName(DescriptionClassName);
		this.description.setThemeName(LabelPaddingTheme);

		this.state = new TextField(Messages.getString("NodeForm.State.Label")); //$NON-NLS-1$
		this.state.addClassName(StateClassName);
		this.state.setReadOnly(true); // state is calculated automatically!
		this.binder.forField(this.state).bind(n -> n.getState().toString(), null);

		this.active  = new Checkbox(Messages.getString("NodeForm.Active.Label")); //$NON-NLS-1$
		this.active.addClassName(ActiveClassName);

		HorizontalLayout stateAndActive = new HorizontalLayout(this.active, this.state);
		stateAndActive.addClassName(CombinedClassName);

		this.properties = new TextArea(Messages.getString("NodeForm.Properties.Label")); //$NON-NLS-1$
		this.properties.addClassName(PropertiesClassName);
		this.properties.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.properties)
			.withValidator(str ->
				{ // Explicit validator instance:
					try {
						if (this.node != null) KeyValuesConverter.convertStringToMap(this.node.resolveProperties(str));
						return true; // still here -> the above conversion succeeded
					} catch (Exception ex) { // Exception -> the above conversion failed
						log.error("Exception converting '{}' to properties: {}", str, ex); //$NON-NLS-1$
						return false;
					}
				}, Messages.getString("NodeForm.Validator.Properties.IllegalValue")) //$NON-NLS-1$
			.bind(NodeForm::getPropertiesAsString, NodeForm::setPropertiesFromString);
		// we also may have to update the resulting URL if any of the properties change:
		this.properties.addValueChangeListener(event ->
			{
				log.debug("properties value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					if (this.node != null) setPropertiesFromString(this.node, event.getValue());
				}
				try {
					updateResolvableValues();
				} catch (Exception ex) {
					log.error("Error updating properties", ex); //$NON-NLS-1$
				}
			});

		this.fields = new VerticalLayout(this.name, this.description, stateAndActive, this.properties);
		this.fields.setClassName(FieldsClassName);
		// this.fields.setSizeFull();
		this.fields.setWidthFull();
		this.fields.setMargin(false);
	}

	public N getNode() {
		return this.node;
	}

	// the following were introduced to be able to "extend" the binder with "sub-binders"

	protected void readBean(N n) {
		log.info("readBean node={}", n); //$NON-NLS-1$
		this.binder.readBean(n);
	}

	protected void writeBean(N n) throws ValidationException {
		log.info("writeBean node={}", n); //$NON-NLS-1$
		this.binder.writeBean(n);
	}

	protected boolean binderHasChanges() {
		boolean res = this.binder.hasChanges();
		log.debug("binderHasChanges(): {}", res); //$NON-NLS-1$
		return res;
	}

	// We need to prevent bean validation when there is currently no bean set or else we get
	// another one of these dreaded IllegalStateException. I don't why this is not done by default :-(
	protected boolean binderIsValid() {
		boolean res = (this.binder.getBean() != null ? this.binder.isValid() : true);
		log.debug("binderIsValid(): {} (bean:{})", res, this.binder.getBean()); //$NON-NLS-1$
		return res;
	}

	public static String getPropertiesAsString(Node n) {
		NodeProperties props = n.getProperties();
		return (props!= null ? props.getPropertiesAsString() : null);
	}

	public static void setPropertiesFromString(Node n, String values) {
		log.trace("setPropertiesFromString(\"{}\")", values); //$NON-NLS-1$
		NodeProperties props = n.getProperties();
		if (props == null) {
			props = new NodeProperties(n.getParent() != null ? n.getParent().getProperties() : null);
		}
		props.setPropertiesFromString(values);
	}
	protected void updateResolvableValues() {
		// to be overriden by subclasses
	}

	/**
	 * @param logPrefix
	 * @param n Node
	 * @param original value
	 * @param resolved method/lambda to update node's field with resolved value (can be null)
	 * @param textField the label to update
	 * @param errorTamplate the string to format with a error msg. (if there was an error)
	 * @return
	 * @throws Exception
	 */
	protected String updateResultingField(String logPrefix,
	                                      N n,
	                                      String originalValue,
	                                      BiConsumer<N, String> resolved,
	                                      TextField textField
	                                     ) {
		String resolvedValue;
		try {
			if (this.node != null) {
				resolvedValue = n.resolveProperties(originalValue);
				log.debug("{}: '{}' -> '{}'", logPrefix, originalValue, resolvedValue); //$NON-NLS-1$
//				if (resolved != null) {
					resolved.accept(n, resolvedValue);
//				}
			} else {
				log.debug("{}: node==null", logPrefix); //$NON-NLS-1$
				resolvedValue = String.format(Messages.getString("NodeForm.ResultingField.NodeUndefined"), logPrefix, originalValue); //$NON-NLS-1$
			}
		} catch (Exception ex) {
			String summary = ExceptionUtils.exceptionCauseSummary(ex);
			log.debug("{}: Illegal value: '{}' : {}", logPrefix, originalValue, summary); //$NON-NLS-1$
			resolvedValue = String.format(Messages.getString("NodeForm.ResultingField.IllegalSyntax"), logPrefix, originalValue, summary); //$NON-NLS-1$
		}
		if (!resolvedValue.equals(originalValue)) {
			log.debug("{}: Setting and displaying textField '{}' -> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
			textField.setValue(resolvedValue);
			textField.setVisible(true);
		} else {
			log.debug("{}: Hiding textField '{}' <-> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
			textField.setVisible(false);
		}
		return resolvedValue;
	}

	private HorizontalLayout createButtonsLayout() {
		this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.save.setId("nodeform-save-button"); //$NON-NLS-1$
		this.save.addClickShortcut(Key.ENTER);
		this.save.addClickListener(event -> validateAndSave());
		this.binder.addStatusChangeListener(event ->
			{
				boolean enabled = binderHasChanges() && binderIsValid();
				log.debug("binder status change -> [save] enabled:{}", enabled); //$NON-NLS-1$
				this.save.setEnabled(enabled);
			});
		this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.delete.setId("nodeform-delete-button"); //$NON-NLS-1$
		this.delete.addClickListener(event -> fireEvent(new DeleteEvent(this, this.node)));

		// Updates the fields again with the previously saved values
		this.reset.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		this.reset.setId("nodeform-reset-button"); //$NON-NLS-1$
		this.reset.addClickListener(event -> readBean(this.node));

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
			writeBean(this.node);
			fireEvent(new SaveEvent(this, this.node));
			readBean(this.node);
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
	@SuppressWarnings("rawtypes") // this class must NOT be parameterized or else it an not be used in lambdas
	public static class UpdateEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -5848948485202766204L;

		UpdateEvent(NodeForm source, Node node) {
			super(source, node);
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