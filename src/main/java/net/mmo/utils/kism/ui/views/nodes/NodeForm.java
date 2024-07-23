
package net.mmo.utils.kism.ui.views.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import java.net.URI;
import java.util.Hashtable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.shared.Registration;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.security.SecurityUtils;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.utils.ConfirmDialog;
import net.mmo.utils.kism.ui.utils.PropertyChangeListenerWithPriorUIReservation;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("javadoc")
public abstract class NodeForm <N extends Node> extends VerticalLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// package visible to allow access by test classes
	static final String ViewClassName = "node-form"; //$NON-NLS-1$
	static final String FieldsClassName = "node-form-fields"; //$NON-NLS-1$
	protected static final String NameClassName = NodeFormField + "name"; //$NON-NLS-1$
	static final String DescriptionClassName = NodeFormField + "description"; //$NON-NLS-1$
	static final String StateClassName       = NodeFormField + "state"; //$NON-NLS-1$
	static final String ApplicableClassName  = NodeFormField + "applicable"; //$NON-NLS-1$
	static final String PropertiesClassName  = NodeFormField + "properties"; //$NON-NLS-1$
	static final String ExecuteClassName     = NodeFormField + "executeButton"; //$NON-NLS-1$

	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	protected NativeLabel header = new NativeLabel(Messages.getString("NodeForm.Label.Header.Text")); //$NON-NLS-1$

	protected TextField name;
	protected TextArea description;
	protected TextField stateField;	// stateField is calculated automatically - never entered!
	protected Checkbox applicable;
	protected TextArea properties;

	// this is not yet added to the form to allow subclasses to include them at appropriate places:
	protected Button executeButton;
	protected VerticalLayout fields;

	private Button saveButton;
	private Button deleteButton;
	private Button revertButton;
	private Button closeButton;

	protected N node;
	protected Binder<N> binder; // a Binder that is aware of bean validation annotations

	/** Unfortunately the Vaadin binder not only considers changes via the UI but also programmatically
	 * updated fields as changes. Its hasCHanges()-method is thus not properly reflecting whether the
	 * *user* has changed anything. We need to keep track of that ourselves:
	 */
	protected boolean uiChanges = false;
	protected boolean isAdminUser;
	protected boolean clearButtonsVisible;

	protected PropertyChangeListenerWithPriorUIReservation<N> nodeRefreshListener;

	/** reusable validator for fields that support properties */
	protected Validator<String> propertiesResolvableValidator;
	/** reusable validator for fields that must not be empty */
	public Validator<String> nonEmptyStringValidator;
	/** reusable validator for fields that must not be empty */
	public Validator<String> nonEmptyNumberValidator;
	/** reusable validator for fields that must be 0 or positive */
	public Validator<String> positiveNumberValidator;

	/** reusable URL validator for URL fields */
	public Validator<String> urlValidator;


	protected NodeForm() {
		this.log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	public void init(NodeService nodeService) {
		throw new IllegalStateException("This method must never be called! Subclasses need to override it and call init(Class<N> nodeClass, NodeService nodeService) instead!"); //$NON-NLS-1$
	}
	public void init(Class<N> clazz, NodeService nodeService) {
		addClassName(ViewClassName);

		initFlags();

		createValidators();

		// we create our binder:
		this.binder = new BeanValidationBinder<>(clazz);

		// this creates the majority of the fields:
		this.log.debug("calling createFormFields(...)"); //$NON-NLS-1$
		createFormFields(nodeService);

		// here we append stuff at the very end of the form:
		this.log.debug("calling appendFurtherFields(...)"); //$NON-NLS-1$
		appendFurtherFields(nodeService);

		// we bind the binder to our node-beans:
		this.log.debug("calling bindInstanceFields(...)"); //$NON-NLS-1$
		this.binder.bindInstanceFields(this); // this binder-setup has to be AFTER creation of the form fields
		                                      // or else the later readBean/writeBean won't work!
		                                      // Took me a day to find out why the binding was a No-Op... ||-(

		add(this.header, this.fields, createButtonsLayout());

		// we need to keep track of user-changes ourselves - see explanation at "uiChanges"
		this.binder.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				this.log.debug("Nodeform-binder: valueChangeEvent from Client:"); //$NON-NLS-1$
				setUiChanges(true);
			}
		});
	}

	public void setUiChanges(boolean hasUIchanges) {
		this.uiChanges = hasUIchanges;
		updateSaveButtonState();
	}

	protected boolean hasUiChanges() {
		if (!this.isVisible()) this.uiChanges = false; // if we are not visible (any more) we can't have changes (work-around for some buggy conditions).
		return this.uiChanges
			|| ((this.node != null) && (!(this.node instanceof RootNode) && (this.node.getParent() == null))); // a non-root node without parent is a free floating node which has changes by definition (since it was just created)
	}

	public void initFlags() {
		this.isAdminUser = SecurityUtils.isAdminUser();
		this.clearButtonsVisible =
			Boolean.parseBoolean(AppProperties.getProperties().getProperty("NodeForm.ClearButtonsVisible", //$NON-NLS-1$
			                     "true")) //$NON-NLS-1$
			&& SecurityUtils.isAdminUser();
	}

	public void setState(Node.State state) {
		this.stateField.setValue(StateLabelProvider.getStateLabel(state));
	}

	public void setHeader(String header) {
		this.header.setText(String.format(header, Messages.getString("NodeForm.Header.Type." + this.getClass().getSimpleName()))); //$NON-NLS-1$
	}


	/**
	 * Sets form to new node.
	 *
	 * @param newNode If newNode == null this signals that we want to leave the
	 *            current node. If there are pending/unsaved changes the user
	 *            will be asked, unless abandonChanges is true.
	 * @param abandonChanges
	 * @return whether we actually switched to the new form. False if there were
	 *         changes and the user responded with "cancel/stayOnNode"
	 */
	public boolean setNode(N newNode, boolean abandonChanges) {
		this.log.info("setNode({}) - UI has changes: {}", newNode, hasUiChanges()); //$NON-NLS-1$
		if (this.node != null) {
			if (hasUiChanges() && !abandonChanges) {
				this.log.info("we have unsaved UI changes!"); //$NON-NLS-1$
				new ConfirmDialog(Messages.getString("NodeForm.UnsavedChanges.Label"), //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Question"), //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Confirm"), (confirmEvent) -> { validateAndSave(); this.uiChanges = false; }, //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Reject"), (rejectEvent) -> { readBean(this.node); this.uiChanges = false; }, //$NON-NLS-1$
				                  Messages.getString("NodeForm.UnsavedChanges.Cancel"), (cancelEvent)  -> { /*nothing to do*/ } //$NON-NLS-1$
				                  ).open();
				return false;
			}
			this.node.removeChangeListener(this.nodeRefreshListener);
			this.log.debug("changeListener removed from node {}",  //$NON-NLS-1$
			               (this.log.isTraceEnabled() ? this.node.toString() : this.node.getName()));
		}
		this.node = newNode;
		this.log.debug("setNode({}) - new node accepted!", (newNode != null ? newNode.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
		if (newNode != null) {
			this.setHeader(Messages.getString(newNode instanceof RootNode || newNode.getParent() != null
			                                  ? "NodeForm.Header.EditOrDeleteNode" //$NON-NLS-1$
			                                  : "NodeForm.Header.NewNode")); //$NON-NLS-1$
			if (this.nodeRefreshListener == null) createNodeRefreshListener(); // make sure we have one...
			this.node.addChangeListener(this.nodeRefreshListener);
			this.log.debug("changeListener set on node '{}'", //$NON-NLS-1$
			               (this.log.isTraceEnabled() ? this.node.toString() : this.node.getName()));
			updateResolvableValues();
		}
		// Note: readBean() copies the values from the bean to an internal model.
		// That way we don’t accidentally overwrite values if we "Revert" (cancel editing).
		readBean(this.node); // to update the form values from the new node to the UI fields.
		this.uiChanges = false; // when we have a new node this is initially always the case!
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
				try {
					NodeForm.this.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
				} catch (Exception ex) {
					NodeForm.this.log.error("exception handling change-event for '" + sourceNode.getName() //$NON-NLS-1$
					        + "' [property: " + propertyName + "] "  //$NON-NLS-1$ //$NON-NLS-2$
					        + oldValue + " --> " + newValue, ex); //$NON-NLS-1$
				}
			}
			// for better log-messages:
			@Override
			public String toString() {
				return "ChangeListener on form " + NodeForm.this.getClass().getSimpleName() + " for node '"  //$NON-NLS-1$ //$NON-NLS-2$
					+ (NodeForm.this.getNode() != null ? NodeForm.this.getNode().getName() : "<no_node>") + "'."; //$NON-NLS-1$ //$NON-NLS-2$
			}

		};
	}

	/**
	 * Handles updates to the data that is NOT triggered via the UI but which may require UI updates (like
	 * the automatic node stateField updates).
	 * This method can be extended by subclasses that need to react to further field changes.
	 */
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		if (sourceNode == this.node) {
			switch (propertyName) {
			case Node.PROPERTYNAME_STATE:
				setState((State)newValue);
				break;
			default:
				// ignore
			}
		}
	}

	/** derived classes need to override this */
	protected void createFormFields(NodeService nodeService) {
		this.name = new TextField(Messages.getString("NodeForm.Name.Label")); //$NON-NLS-1$
		this.name.addClassName(NameClassName);
		this.name.setTooltipText(Messages.getString("NodeForm.Name.Tooltip")); //$NON-NLS-1$
		this.name.setThemeName(UIConstants.LabelPaddingTheme);
		this.name.setClearButtonVisible(this.clearButtonsVisible);
		this.name.setEnabled(this.isAdminUser);
		this.binder.forField(this.name)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator((value, context) ->
				{ // Explicit validator instance
					try {
						this.getNode().onRootNodeDo((rootnode) -> rootnode.trickleDown((n) -> {
							if ((n != this.node) && Objects.equals(n.getName(), value)) {
								throw new RuntimeException("dummy"); //$NON-NLS-1$ // we need to abort the trickleDown-method
							}
						}));
					} catch (Throwable ex) {
						return ValidationResult.error(Messages.getString("NodeForm.Validator.NodeName.MustBeUnique")); //$NON-NLS-1$
					}
					return nodeService.getRootNodes().stream()
						.filter(rootNode -> rootNode != this.node) // not the same rootnode?
						.filter(rootNode -> rootNode.getName().equals(value)) // same name as any other rootnode?
						.findAny().isEmpty() ? ValidationResult.ok() : ValidationResult.error(Messages.getString("NodeForm.Validator.RootName.MustBeUnique")); //$NON-NLS-1$
				})
			.bind(Node::getName, Node::setName)
		;

		this.description = new TextArea(Messages.getString("NodeForm.Description.Label")); //$NON-NLS-1$
		this.description.addClassName(DescriptionClassName);
		this.description.setTooltipText(Messages.getString("NodeForm.Description.Tooltip")); //$NON-NLS-1$
		this.description.setThemeName(UIConstants.LabelPaddingTheme);
		this.description.setClearButtonVisible(this.clearButtonsVisible);
		this.description.setEnabled(this.isAdminUser);

		this.stateField = new TextField(Messages.getString("NodeForm.State.Label")); //$NON-NLS-1$
		this.stateField.addClassName(StateClassName);
		this.stateField.setTooltipText(Messages.getString("NodeForm.State.Tooltip")); //$NON-NLS-1$
		this.stateField.setReadOnly(true); // stateField is calculated!
		this.binder.forField(this.stateField).bind(n -> StateLabelProvider.getStateLabel(n.getState()), null);

		this.applicable  = new Checkbox(Messages.getString("NodeForm.Applicable.Label")); //$NON-NLS-1$
		this.applicable.addClassName(ApplicableClassName);
		this.applicable.setTooltipText(Messages.getString("NodeForm.Applicable.Tooltip")); //$NON-NLS-1$

		HorizontalLayout stateAndApplicable = new HorizontalLayout(this.applicable, this.stateField);
		stateAndApplicable.addClassName(UIConstants.CombinedClassName);

		this.properties = new TextArea(Messages.getString("NodeForm.Properties.Label")); //$NON-NLS-1$
		this.properties.addClassName(PropertiesClassName);
		this.properties.setTooltipText(Messages.getString("NodeForm.Properties.Tooltip")); //$NON-NLS-1$
		this.properties.setThemeName(UIConstants.LabelPaddingTheme);
		this.properties.addThemeName(UIConstants.MonospaceTheme);
		this.properties.setClearButtonVisible(this.clearButtonsVisible);
		this.properties.setEnabled(this.isAdminUser);

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
				this.log.debug("properties value changed from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					if (this.node != null) this.node.propertiesFromString(event.getValue());
				}
				try {
					updateResolvableValues();
				} catch (Exception ex) {
					this.log.error("Error updating properties", ex); //$NON-NLS-1$
				}
			});

		this.executeButton = new Button(Messages.getString("NodeForm.Execute.Label")); //$NON-NLS-1$
		this.executeButton.setClassName(ExecuteClassName);
		this.executeButton.setThemeName(UIConstants.LabelPaddingTheme);
		this.executeButton.addClickListener(event ->
			{
				this.log.info("executeButton clicked"); //$NON-NLS-1$
				if (SecurityUtils.isAdminUser() && hasUiChanges()) {
					validateAndSave();
				}
				try {
					this.node.executeRequest();
				} catch (Throwable t) {
					NodeView.ExecutionError("manually triggered execution", t); //$NON-NLS-1$
				}
			});

		this.fields = new VerticalLayout(this.name, this.description, stateAndApplicable, this.properties);
		this.fields.setClassName(FieldsClassName);
	}

	/**
	 * This is meant for fields/forms that are to be added to the bottom of the list.
	 * To be extended by subclasses...
	 */
	protected void appendFurtherFields(NodeService nodeService) {
		this.fields.setSizeFull();
		// this.fields.setWidthFull();
		this.fields.setMargin(false);
	}

	/**
	 * to be extended by subclasses
	 */
	protected void createValidators() {
		// We create a Validator that can be used everywhere where we need to check whether a field
		// contains unresolvable properties:
		this.propertiesResolvableValidator = new Validator<>()
		{
			private static final long serialVersionUID = -7211887363956128833L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				try {
					if (NodeForm.this.node != null) {
						NodeForm.this.node.resolveProperties(value);
					}
					return ValidationResult.ok(); // if still here -> the above conversion succeeded
				} catch (Exception ex) { // Exception -> the above conversion failed
					return ValidationResult.error(String.format(Messages.getString("NodeForm.Validator.Properties.IllegalValue"), //$NON-NLS-1$
																value, ex.getMessage()));
				}
			}
		};

		this.nonEmptyStringValidator = new Validator<>()
		{
			private static final long serialVersionUID = -7211887363956128833L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				String resolvedValue;
				try {
					resolvedValue = NodeForm.this.node != null
					                ? NodeForm.this.node.resolveProperties(value)
					                : value;
				} catch (Exception ex) {
					return ValidationResult.error("Error resolving property: " + ex); //$NON-NLS-1$
				}
				return resolvedValue.isBlank()
					? ValidationResult.error("String operand must not be empty!") //$NON-NLS-1$
					: ValidationResult.ok();
			}
		};

		this.nonEmptyNumberValidator = new Validator<>()
		{
			private static final long serialVersionUID = -7211887363956128833L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				String resolvedValue;
				try {
					resolvedValue = NodeForm.this.node != null
					                ? NodeForm.this.node.resolveProperties(value)
					                : value;
				} catch (Exception ex) {
					return ValidationResult.error("Error resolving property: " + ex); //$NON-NLS-1$
				}
				try {
					Integer.parseInt(resolvedValue);
					return ValidationResult.ok();
				} catch (Exception ex) {
					return ValidationResult.error("Cannot convert '" + resolvedValue + "' to a numeric value!"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		};

		this.positiveNumberValidator = new Validator<>()
		{
			private static final long serialVersionUID = -476108409332944491L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				String resolvedValue;
				try {
					resolvedValue = NodeForm.this.node != null
					                ? NodeForm.this.node.resolveProperties(value)
					                : value;
				} catch (Exception ex) {
					return ValidationResult.error("Error resolving property: " + ex); //$NON-NLS-1$
				}
				try {
					if (Integer.parseInt(resolvedValue) >= 0) return ValidationResult.ok();
					return ValidationResult.error("'" + resolvedValue + "' must be non-negative!"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception ex) {
					return ValidationResult.error("Cannot convert '" + resolvedValue + "' to a numeric value!"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		};

		this.urlValidator = new Validator<>()
		{
			private static final long serialVersionUID = 4829240255774815878L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				NodeForm.this.log.trace("validating url: '{}'", value); //$NON-NLS-1$
				try {
					// we check whether we can resolve the url and convert it to a valid URL:
					URI uri = new URI(NodeForm.this.node != null ? NodeForm.this.node.resolveProperties(value) : value);
					// no check necessary - any error is thrown!
					NodeForm.this.log.debug("validation succeeded: '{}' => '{}'", value, uri); //$NON-NLS-1$
					return ValidationResult.ok();
				} catch (Exception ex) {
					NodeForm.this.log.debug("validation failed: '{}' -> {}", value, ex); //$NON-NLS-1$
					return ValidationResult.error(String.format(Messages.getString("NodeForm.URL.ValidatorErrorMsg"), //$NON-NLS-1$
					                                            value, ex));
				}
			}
		};
	}

	public N getNode() {
		return this.node;
	}

	// the following were introduced to be able to "extend" the binder with "sub-binders"

	protected void readBean(N n) {
		this.log.debug("readBean node={}", n); //$NON-NLS-1$
		if (n != null) {
			this.binder.readBean(n);
		}
		setUiChanges(false);
	}

	protected void writeBean(N n) throws ValidationException {
		this.log.debug("writeBean node={}", n); //$NON-NLS-1$
		if (n != null) {
			this.binder.writeBean(n);
		}
	}


	// We need to prevent bean validation when there is currently no bean set or else we get
	// another one of these dreaded IllegalStateException. I don't why this is not done by default :-(
	protected boolean binderIsValid() {
		boolean res = (this.binder.getBean() != null ? this.binder.isValid() : true);
		this.log.debug("binderIsValid(): {})", res); //$NON-NLS-1$
		return res;
	}

	protected void updateSaveButtonState() {
		boolean binderIsValid = binderIsValid();
		boolean enableSave = hasUiChanges() && binderIsValid;
		this.log.debug("binder status change -> [saveButton] enabled:{}", enableSave); //$NON-NLS-1$
		this.saveButton.setEnabled(enableSave);
		this.executeButton.setText(Messages.getString(hasUiChanges() && SecurityUtils.isAdminUser()
		                                              ? (this instanceof IntermediateNodeForm
		                                                 ? "IntermediateNodeForm.SaveAndExecute.Label" //$NON-NLS-1$
		                                                 : "LeafNodeForm.SaveAndExecute.Label") //$NON-NLS-1$
		                                              : (this instanceof IntermediateNodeForm
		                                                 ? "IntermediateNodeForm.Execute.Label" //$NON-NLS-1$
		                                                 : "LeafNodeForm.Execute.Label"))); //$NON-NLS-1$
		boolean enableExecute = binderIsValid;
		this.log.debug("binder status change -> [executeButton] enabled:{}", enableExecute); //$NON-NLS-1$
		this.executeButton.setEnabled(enableExecute);
	}

	protected void updateResolvableValues() {
		// to be overridden by subclasses
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
	protected String updateResultingField(String logPrefix,
	                                      N n,
	                                      String originalValue,
	                                      BiConsumer<N, String> resolved,
	                                      TextField textField) {
		String resolvedValue = null;
		String errorMsg = null;
		try {
			if (n != null) {
				resolvedValue = n.resolveProperties(originalValue);
				this.log.debug("{}: '{}' -> '{}'", logPrefix, originalValue, resolvedValue); //$NON-NLS-1$
				if (resolved != null) {
					resolved.accept(n, resolvedValue);
				}
			} else {
				this.log.debug("{}: node==null", logPrefix); //$NON-NLS-1$
				resolvedValue = String.format(Messages.getString("NodeForm.ResultingField.NodeUndefined"), logPrefix, originalValue); //$NON-NLS-1$
			}
		} catch (Exception ex) {
			String summary = ExceptionUtils.exceptionCauseSummary(ex);
			this.log.debug("{}: Illegal value: '{}' : {}", logPrefix, originalValue, summary); //$NON-NLS-1$
			errorMsg = String.format(Messages.getString("NodeForm.ResultingField.IllegalSyntax"), logPrefix, originalValue, summary); //$NON-NLS-1$
			resolvedValue = errorMsg;
		}
		if (textField != null) {
			if (!Objects.equals(resolvedValue, originalValue)) {
				this.log.debug("{}: Setting and displaying textField '{}' -> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
				textField.setValue(resolvedValue);
				textField.setVisible(true);
			} else {
				this.log.debug("{}: Hiding textField '{}' <-> '{}'", logPrefix, textField.getValue(), resolvedValue); //$NON-NLS-1$
				textField.setVisible(false);
			}
		}
		return errorMsg;
	}

	private HorizontalLayout createButtonsLayout() {
		// saves all changes values of the current node
		this.saveButton = new Button(Messages.getString("NodeForm.Button.Save.Label")); //$NON-NLS-1$
		this.saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.saveButton.setId("nodeform-saveButton-button"); //$NON-NLS-1$
		this.saveButton.addClickShortcut(Key.ENTER);
		this.saveButton.addClickListener(event -> {
			this.log.info("saveButton clicked."); //$NON-NLS-1$
			validateAndSave();
		});
		this.saveButton.setEnabled(false);

		// deletes the current node - sent as event to the top NodeView
		this.deleteButton = new Button(Messages.getString("NodeForm.Button.Delete.Label")); //$NON-NLS-1$
		this.deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.deleteButton.setId("nodeform-deleteButton-button"); //$NON-NLS-1$
		this.deleteButton.addClickListener(event -> {
			this.log.info("deleteButton clicked."); //$NON-NLS-1$
			fireEvent(new DeleteEvent(this, this.node));
		});
		// Restores the fields again with the previously saved values
		this.revertButton = new Button(Messages.getString("NodeForm.Button.Revert.Label")); //$NON-NLS-1$
		this.revertButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		this.revertButton.setId("nodeform-revertButton-button"); //$NON-NLS-1$
		this.revertButton.addClickListener(event -> {
			this.log.info("revertButton clicked."); //$NON-NLS-1$
			readBean(this.node);
		});

		// closeButton the node details form
		this.closeButton = new Button(Messages.getString("NodeForm.Button.Cancel.Label")); //$NON-NLS-1$
		this.closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		this.closeButton.setId("nodeform-closeButton-button"); //$NON-NLS-1$
		this.closeButton.addClickShortcut(Key.ESCAPE);
		this.closeButton.addClickListener(event -> {
			this.log.info("closeButton clicked."); //$NON-NLS-1$
			fireEvent(new CloseEvent(this));
		});

		return new HorizontalLayout(this.saveButton, this.deleteButton, this.revertButton, this.closeButton);
	}

	protected void validateAndSave() {
		if (this.node != null) {
			this.log.info("validateAndSave '{}'", this.node.getName()); //$NON-NLS-1$
			if (SecurityUtils.isAdminUser()) {
				try {
					validate();
					writeBean(this.node);
					fireEvent(new SaveEvent(this, this.node));
					setUiChanges(false);
				} catch (ValidationException ex) { // throws ValidationException if data not valid
					this.log.info("ValidationException:", ex); //$NON-NLS-1$
					java.util.List<ValidationResult> beanValidationErrors = ex.getBeanValidationErrors();
					String errorMsg = "Validation failed: " + ex.getMessage() + ": "; //$NON-NLS-1$ //$NON-NLS-2$
					if (beanValidationErrors.isEmpty()) {
						java.util.List<BindingValidationStatus<?>> fieldValidationErrors = ex.getFieldValidationErrors();
						errorMsg +=
						fieldValidationErrors.stream()
							.map((BindingValidationStatus<?> beanValidationStatus) -> "status:'" + beanValidationStatus.getStatus() //$NON-NLS-1$
						                                                              + "': " + (beanValidationStatus.getResult().isPresent() //$NON-NLS-1$
						                                                                        ? beanValidationStatus.getResult().get().getErrorMessage()
						                                                                        : "unresolved:" + fieldValidationErrors) //$NON-NLS-1$
						                                                              )
						    .collect(Collectors.toList());
					} else {
						errorMsg += beanValidationErrors.stream().map((validationResult) -> validationResult.getErrorMessage()).collect(Collectors.toList());
					}
					this.log.warn(errorMsg);
					NodeService.createNotification(errorMsg);
				}

			} else {
				readBean(this.node); // reset whatever changes the user may have entered
			}
		} else {
			setUiChanges(false);
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


	private Hashtable<Class<?>, Registration> registrations = new Hashtable<>();

	@Override
	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
	                                                              ComponentEventListener<T> listener) {
		removeListener(eventType); // remove potential listener for same event type
		Registration reg = getEventBus().addListener(eventType, listener);
		this.registrations.put(eventType, reg);
		return reg;
	}
	public <T extends ComponentEvent<?>> void removeListener(Class<T> eventType) {
		Registration reg = this.registrations.get(eventType);
		if (reg != null) reg.remove();
	}

	@SuppressWarnings("unchecked")
	public <T extends ComponentEvent<?>> void clearListeners() {
		this.registrations.keySet().forEach((key) -> removeListener((Class<T>)key));
	}
}