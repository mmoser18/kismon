
package net.mmo.utils.kism.ui.view.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import java.net.URI;
import java.util.Objects;
import java.util.function.BiConsumer;

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
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.ui.utils.ConfirmDialog;
import net.mmo.utils.kism.ui.utils.PropertyChangeListenerWithPriorUIReservation;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.KeyValuesConverter;

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
	static final String ApplicableClassName  = NodeFormField + "applicable"; //$NON-NLS-1$
	static final String PropertiesClassName  = NodeFormField + "properties"; //$NON-NLS-1$
	static final String ExecuteClassName     = NodeFormField + "execute"; //$NON-NLS-1$

	/** class name to mark combined fields */
	public static final String CombinedClassName    = "node-form-field-combined"; //$NON-NLS-1$

	/** theme use for fields that should have its label closer to the value field than default */
	public static final String LabelPaddingTheme    = "label-padding"; //$NON-NLS-1$
	/** theme use for fields that should be displayed in monospaced font */
	public static final String MonospaceTheme   = "monospace"; //$NON-NLS-1$

	protected Label header = new Label(Messages.getString("NodeForm.Label.Header.Text")); //$NON-NLS-1$

	protected TextField name;
	protected TextArea description;
	protected TextField stateField;	// stateField is calculated automatically - never entered!
	protected Checkbox applicable;
	protected TextArea properties;

	// this is not yet added to the form to allow subclasses to include them at appropriate places:
	protected Button execute;

	protected VerticalLayout fields;

	private Button save   = new Button(Messages.getString("NodeForm.Button.Save.Label")); //$NON-NLS-1$
	private Button delete = new Button(Messages.getString("NodeForm.Button.Delete.Label")); //$NON-NLS-1$
	private Button reset  = new Button(Messages.getString("NodeForm.Button.Reset.Label")); //$NON-NLS-1$
	private Button close  = new Button(Messages.getString("NodeForm.Button.Cancel.Label")); //$NON-NLS-1$

	protected N node;
	protected Binder<N> binder; // a Binder that is aware of bean validation annotations

	/** Unfortunately the Vaadin binder not only considers changes via the UI but also programmatically
	 * updated fields as changes. Its hasCHanges()-method is thus not properly reflecting whether the
	 * *user* has changed anything. We need to keep track of that ourselves.
	 */
	protected boolean uiChanges = false;

	protected static boolean clearButtonsVisible = Boolean.getBoolean(AppProperties.getProperties().getProperty("NodeForm.ClearButtonsVisible", "true")); //$NON-NLS-1$ //$NON-NLS-2$


	protected PropertyChangeListenerWithPriorUIReservation<N> nodeRefreshListener;

	/** reusable validator for fields that support properties */
	protected Validator<String> propertiesResolvableValidator;
	/** reusable URL validator for URL fields */
	protected Validator<String> urlValidator;


	public NodeForm(Class<N> clazz, NodeService nodeService) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		addClassName(ViewClassName);

		// We create a Validator that can be used everywhere where we need to check whether a field
		// contains unresolvable properties:
		this.propertiesResolvableValidator = new Validator<>()
			{
				private static final long serialVersionUID = -7211887363956128833L;

				@Override
				public ValidationResult apply(String value, ValueContext context) {
					try {
						if (NodeForm.this.node != null) NodeForm.this.node.resolveProperties(value);
						return ValidationResult.ok(); // still here -> the above conversion succeeded
					} catch (Exception ex) { // Exception -> the above conversion failed
						return ValidationResult.error(String.format(Messages.getString("NodeForm.Validator.Properties.IllegalValue"), //$NON-NLS-1$
																	value, ex.getMessage()));
					}
				}
			};

			this.urlValidator = new Validator<>()
			{
				private static final long serialVersionUID = 4829240255774815878L;

				@Override
				public ValidationResult apply(String value, ValueContext context) {
					log.trace("validating url: '{}'", value); //$NON-NLS-1$
					try {
						if (NodeForm.this.node != null) {
							// we check whether we can resolve the url and convert it to a valid URL:
							URI uri = new URI(NodeForm.this.node.resolveProperties(value));
							// no check necessary - any error is thrown!
							log.debug("validation succeeded: '{}' => '{}'", value, uri); //$NON-NLS-1$
						} else {
							log.debug("node == null: validation succeeded"); //$NON-NLS-1$
						}
						return ValidationResult.ok();
					} catch (Exception ex) {
						log.debug("validation failed: '{}' -> {}", value, ex); //$NON-NLS-1$
						return ValidationResult.error(String.format(Messages.getString("NodeForm.URL.ValidatorErrorMsg"), //$NON-NLS-1$
						                                            value, ex));
					}
				}
			};

		// we create our binder:
		this.binder = new BeanValidationBinder<>(clazz);

		createFormFields(nodeService);

		// we bind the binder to our node-beans:
		this.binder.bindInstanceFields(this); // this binder-setup has to be AFTER creation of the form fields
		                                      // or else the later readBean/writeBean won't work!
		                                      // Took me a day to find out why the binding was a No-op... ||-(

		add(this.header, this.fields, createButtonsLayout());

		// we need to keep track of user-changes ourselves - see explanation at "uiChanges"
		this.binder.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				log.debug("valueChangeEvent from Client:"); //$NON-NLS-1$
				setUiChanges(true);
			}
		});
	}

	protected void setUiChanges(boolean hasUIchanges) {
		this.uiChanges = hasUIchanges;
		updateSaveButtonState();
	}

	protected boolean hasUiChanges() {
		if (!this.isVisible()) this.uiChanges = false; // if we are not visible (any more) we can't have changes (work-around for some buggy conditions).
		return this.uiChanges
			   || ((this.node != null) && (!(this.node instanceof RootNode) && (this.node.getParent() == null))); // a non-root node without parent is a free floating node which has changes by definition (since it was just created)
	}

	public void setState(Node.State state) {
		this.stateField.setValue(StateLabelProvider.getStateLabel(state));
	}

	public void setHeader(String header) {
		this.header.setText(String.format(header, Messages.getString("NodeForm.Header.Type." + this.getClass().getSimpleName()))); //$NON-NLS-1$
	}

	public boolean setNode(N newNode) {
		log.debug("NodeForm.setNode({})", newNode); //$NON-NLS-1$
		if (this.node != null) {
			if (hasUiChanges()) {
				log.debug("we have unsaved UI changes!"); //$NON-NLS-1$
				new ConfirmDialog(Messages.getString("NodeForm.UnsavedChanges.Label"), //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Question"), //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Confirm"), (confirmEvent) -> { validateAndSave(); }, //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Reject"), (rejectEvent) -> { readBean(this.node); }, //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Cancel"), (cancelEvent)  -> { /*nothing to do*/ } //$NON-NLS-1$
				                  ).open();
				return false;
			}
			this.node.removeChangeListener(this.nodeRefreshListener);
			log.info("changeListener removed on node {}", this.node); //$NON-NLS-1$
		}
		this.node = newNode;
		this.uiChanges = false; // when we have a new node this is always the case!
		log.debug("NodeForm.setNode({}) - new node accepted!", (newNode != null ? newNode.getName() : "<null>")); //$NON-NLS-1$ //$NON-NLS-2$
		// this.binder.setBean(node);
		if (newNode != null) {
			this.setHeader(Messages.getString(newNode instanceof RootNode || newNode.getParent() != null
			                                  ? "NodeForm.Header.EditOrDeleteNode" //$NON-NLS-1$
			                                  : "NodeForm.Header.NewNode")); //$NON-NLS-1$
			if (this.nodeRefreshListener == null) createNodeRefreshListener(); // make sure we have one...
			newNode.addChangeListener(this.nodeRefreshListener);
			log.info("changeListener set on node '{}'", newNode.getName()); //$NON-NLS-1$
			updateResolvableValues();
		}
		// Note: readBean() copies the values from the bean to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
		readBean(newNode); // to update the form values from the new node to the UI fields.
		return true;
	}

	protected void createNodeRefreshListener() {
		this.nodeRefreshListener = new PropertyChangeListenerWithPriorUIReservation<N>(this)
		{

			@Override
			protected boolean predicate(N sourceNode, String propertyName, Object oldValue, Object newValue) {
				return sourceNode == NodeForm.this.getNode(); // *this* form is only interested in updates to the *current* node!
			}

			@Override
			protected void handlePropertyChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
				NodeForm.this.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
			}
			// for better log-messages:
			@Override
			public String toString() {
				return "ChangeListener_on_" + NodeForm.this.getClass().getSimpleName() + "_for_" + (NodeForm.this.getNode() != null ? NodeForm.this.getNode().getName() : "<no_node>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

		};
	}

	/**
	 * Handles updates to the data that is NOT triggered via the UI but which may require UI updates (like
	 * the automatic node stateField updates).
	 * This method can be extended by subclasses that need to react to further field changes.
	 */
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case Node.PROPERTYNAME_STATE:
			setState((State)newValue);
			break;
		default:
			// ignore
		}
	}

	/** derived classes need to override this */
	protected void createFormFields(NodeService nodeService) {
		this.name = new TextField(Messages.getString("NodeForm.Name.Label")); //$NON-NLS-1$
		this.name.addClassName(NameClassName);
		this.name.setThemeName(LabelPaddingTheme);
		this.name.setClearButtonVisible(clearButtonsVisible);
		this.binder.forField(this.name)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator(value ->
				{ // Explicit validator instance
					return nodeService.getRootNodes().stream()
						.filter(rootNode -> rootNode != this.node) // not the same rootnode?
						.filter(rootNode -> rootNode.getName().equals(value)) // same name as any other rootnode?
						.findAny().isEmpty();
				}, Messages.getString("NodeForm.Validator.RootName.MustBeUnique")) //$NON-NLS-1$
			.bind(Node::getName, Node::setName)
		;

		this.description = new TextArea(Messages.getString("NodeForm.Description.Label")); //$NON-NLS-1$
		this.description.addClassName(DescriptionClassName);
		this.description.setThemeName(LabelPaddingTheme);
		this.description.setClearButtonVisible(clearButtonsVisible);

		this.stateField = new TextField(Messages.getString("NodeForm.State.Label")); //$NON-NLS-1$
		this.stateField.addClassName(StateClassName);
		this.stateField.setReadOnly(true); // stateField is calculated!
		this.binder.forField(this.stateField).bind(n -> StateLabelProvider.getStateLabel(n.getState()), null);

		this.applicable  = new Checkbox(Messages.getString("NodeForm.Applicable.Label")); //$NON-NLS-1$
		this.applicable.addClassName(ApplicableClassName);

		HorizontalLayout stateAndApplicable = new HorizontalLayout(this.applicable, this.stateField);
		stateAndApplicable.addClassName(CombinedClassName);

		this.properties = new TextArea(Messages.getString("NodeForm.Properties.Label")); //$NON-NLS-1$
		this.properties.addClassName(PropertiesClassName);
		this.properties.setThemeName(LabelPaddingTheme);
		this.properties.addThemeName(MonospaceTheme);
		this.properties.setClearButtonVisible(clearButtonsVisible);
		this.binder.forField(this.properties)
			.withValidator((value, context) ->
				{ // Explicit validator instance:
					try {
						if (this.node != null) KeyValuesConverter.convertStringToMap(this.node.resolveProperties(value));
						return ValidationResult.ok(); // still here -> the above conversion succeeded
					} catch (Exception ex) { // Exception -> the above conversion failed
						return ValidationResult.error(String.format(Messages.getString("NodeForm.Validator.Properties.IllegalValue"), //$NON-NLS-1$
						                                            value, ex.getMessage()));
					}
				})
			.bind(Node::getPropertiesAsString, Node::propertiesFromString);
		// we also may have to update the resulting URL if any of the properties change:
		this.properties.addValueChangeListener(event ->
			{
				log.debug("properties value changed from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					if (this.node != null) this.node.propertiesFromString(event.getValue());
				}
				try {
					updateResolvableValues();
				} catch (Exception ex) {
					log.error("Error updating properties", ex); //$NON-NLS-1$
				}
			});

		this.execute = new Button(Messages.getString("NodeForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.execute.setThemeName(LabelPaddingTheme);
		this.execute.addClickListener(event ->
			{
				log.info("execute clicked"); //$NON-NLS-1$
				if (hasUiChanges()) validateAndSave();
				try {
					this.node.executeRequest();
				} catch (Throwable t) {
					NodeView.ExecutionError("manually triggered execution", t); //$NON-NLS-1$
				}
			});

		this.fields = new VerticalLayout(this.name, this.description, stateAndApplicable, this.properties);
		this.fields.setClassName(FieldsClassName);
		this.fields.setSizeFull();
		// this.fields.setWidthFull();
		this.fields.setMargin(false);
	}

	public N getNode() {
		return this.node;
	}

	// the following were introduced to be able to "extend" the binder with "sub-binders"

	protected void readBean(N n) {
		log.info("readBean node={}", n); //$NON-NLS-1$
		this.binder.readBean(n);
		setUiChanges(false);
	}

	protected void writeBean(N n) throws ValidationException {
		log.info("writeBean node={}", n); //$NON-NLS-1$
		this.binder.writeBean(n);
	}


	// We need to prevent bean validation when there is currently no bean set or else we get
	// another one of these dreaded IllegalStateException. I don't why this is not done by default :-(
	protected boolean binderIsValid() {
		boolean res = (this.binder.getBean() != null ? this.binder.isValid() : true);
		log.debug("binderIsValid(): {} (bean:{})", res, this.binder.getBean()); //$NON-NLS-1$
		return res;
	}

	protected void updateSaveButtonState() {
		boolean enable = hasUiChanges() && binderIsValid();
		log.debug("binder status change -> [save] enabled:{}", enable); //$NON-NLS-1$
		this.save.setEnabled(enable);

		this.execute.setText(Messages.getString(this.uiChanges
		                                        ? (this instanceof IntermediateNodeForm
		                                          ? "IntermediateNodeForm.SaveAndExecute.Label" //$NON-NLS-1$
		                                          : "LeafNodeForm.SaveAndExecute.Label") //$NON-NLS-1$
		                                        : (this instanceof IntermediateNodeForm
		                                          ? "IntermediateNodeForm.Execute.Label" //$NON-NLS-1$
		                                          : "LeafNodeForm.Execute.Label"))); //$NON-NLS-1$
		boolean isValid = binderIsValid();
		log.debug("binder status change -> [execute] active:{}", isValid); //$NON-NLS-1$
		this.execute.setEnabled(isValid);
	}

	protected void updateResolvableValues() {
		// to be overriden by subclasses
	}

	/**
	 * Checks whether a value is "resolvable" (i.e. whether contained properties can be resolved) and
	 * optionally assigns the result to an entity field and/or a form-field
	 * @param logPrefix
	 * @param n Node
	 * @param original value
	 * @param resolved method/lambda to update node's field with resolved value (can be null)
	 * @param textField the form-field to update (can be null)
	 * @return the resolved value or an error message
	 * @throws Exception
	 */
	protected static <N extends Node> String updateResultingField(String logPrefix,
	                                                              N n,
	                                                              String originalValue,
	                                                              BiConsumer<N, String> resolved,
	                                                              TextField textField
	                                                             ) {
		String resolvedValue = null;
		String errorMsg = null;
		try {
			if (n != null) {
				resolvedValue = n.resolveProperties(originalValue);
				log.debug("{}: '{}' -> '{}'", logPrefix, originalValue, resolvedValue); //$NON-NLS-1$
				if (resolved != null) {
					resolved.accept(n, resolvedValue);
				}
			} else {
				log.debug("{}: node==null", logPrefix); //$NON-NLS-1$
				resolvedValue = String.format(Messages.getString("NodeForm.ResultingField.NodeUndefined"), logPrefix, originalValue); //$NON-NLS-1$
			}
		} catch (Exception ex) {
			String summary = ExceptionUtils.exceptionCauseSummary(ex);
			log.debug("{}: Illegal value: '{}' : {}", logPrefix, originalValue, summary); //$NON-NLS-1$
			errorMsg = String.format(Messages.getString("NodeForm.ResultingField.IllegalSyntax"), logPrefix, originalValue, summary); //$NON-NLS-1$
			resolvedValue = errorMsg;
		}
		if (textField != null) {
			if (!Objects.equals(resolvedValue, originalValue)) {
				log.debug("{}: Setting and displaying textField '{}' -> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
				textField.setValue(resolvedValue);
				textField.setVisible(true);
			} else {
				log.debug("{}: Hiding textField '{}' <-> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
				textField.setVisible(false);
			}
		}
		return errorMsg;
	}

	private HorizontalLayout createButtonsLayout() {
		// saves all changes values of the current node
		this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.save.setId("nodeform-save-button"); //$NON-NLS-1$
		this.save.addClickShortcut(Key.ENTER);
		this.save.addClickListener(event -> validateAndSave());
		this.save.setEnabled(false);

		// deletes the current node - sent as event to the top NodeView
		this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.delete.setId("nodeform-delete-button"); //$NON-NLS-1$
		this.delete.addClickListener(event -> fireEvent(new DeleteEvent(this, this.node)));

		// Updates the fields again with the previously saved values
		this.reset.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		this.reset.setId("nodeform-reset-button"); //$NON-NLS-1$
		this.reset.addClickListener(event -> readBean(this.node));

		// close the node details form
		this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		this.close.setId("nodeform-close-button"); //$NON-NLS-1$
		this.close.addClickShortcut(Key.ESCAPE);
		this.close.addClickListener(event -> fireEvent(new CloseEvent(this)));

		return new HorizontalLayout(this.save, this.delete, this.reset, this.close);
	}

	protected void validateAndSave() {
		if (this.node != null) {
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

		protected NodeFormEvent(NodeForm<?> source, Node node) {
			super(source, false);
			this.node = node;
		}

		public Node getNode() {
			return this.node;
		}
	}

	// this class must NOT be parameterized or else it can not be used in lambdas!
	public static class SaveEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -8255534604198398835L;

		SaveEvent(NodeForm<?> source, Node node) {
			super(source, node);
		}
	}
	// this class must NOT be parameterized or else it can not be used in lambdas!
	public static class DeleteEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -5394186746319400182L;

		DeleteEvent(NodeForm<?> source, Node node) {
			super(source, node);
		}
	}
	// this class must NOT be parameterized or else it can not be used in lambdas!
	public static class CloseEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -657451450305155687L;

		CloseEvent(NodeForm<?> source) {
			super(source, null);
		}
	}
	// this class must NOT be parameterized or else it can not be used in lambdas!
	public static class UpdateEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -5848948485202766204L;

		UpdateEvent(NodeForm<?> source, Node node) {
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