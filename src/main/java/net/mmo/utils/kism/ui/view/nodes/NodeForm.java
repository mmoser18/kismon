
package net.mmo.utils.kism.ui.view.nodes;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.Node;

@Slf4j
public class NodeForm extends FormLayout
{
	private static final long serialVersionUID = -4928837821374748141L;

	// public to allow access by test classes
	public static final String ViewClassName = "node-form"; //$NON-NLS-1$
	public static final String NameClassName = "node-form-name"; //$NON-NLS-1$
	public static final String DescriptionClassName = "node-form-description"; //$NON-NLS-1$
	public static final String StateClassName = "node-form-state"; //$NON-NLS-1$
	public static final String EnabledClassName = "node-form-enabled"; //$NON-NLS-1$
	public static final String IgnoredClassName = "node-form-ignored"; //$NON-NLS-1$

	Label header = new Label(Messages.getString("NodeForm.Label.Header.Text")); //$NON-NLS-1$
	TextField name = new TextField(Messages.getString("NodeForm.TextField.Name.Label")); //$NON-NLS-1$
	TextArea description = new TextArea(Messages.getString("NodeForm.TextField.Description.Label")); //$NON-NLS-1$
	ComboBox<Node.State> state = new ComboBox<>(Messages.getString("NodeForm.ComboBox.State.Label")); //$NON-NLS-1$
	ComboBox<Boolean> enabled = new ComboBox<>(Messages.getString("NodeForm.ComboBox.Enabled.Label")); //$NON-NLS-1$
	ComboBox<Boolean> ignored = new ComboBox<>(Messages.getString("NodeForm.ComboBox.Ignored.Label")); //$NON-NLS-1$

	Button save   = new Button(Messages.getString("NodeForm.Button.Save.Label")); //$NON-NLS-1$
	Button delete = new Button(Messages.getString("NodeForm.Button.Delete.Label")); //$NON-NLS-1$
	Button close  = new Button(Messages.getString("NodeForm.Button.Cancel.Label")); //$NON-NLS-1$

	private Node node;
	Binder<Node> binder = new BeanValidationBinder<>(Node.class); // a Binder that is aware of bean validation annotations

	public NodeForm() {
		log.debug("Creating ContactForm:"); //$NON-NLS-1$
		addClassName(ViewClassName);
		this.binder.bindInstanceFields(this);

		this.name.addClassName(NameClassName);
		this.description.addClassName(DescriptionClassName);
		this.state.addClassName(StateClassName);
		this.enabled.addClassName(EnabledClassName);
		this.ignored.addClassName(IgnoredClassName);

		this.state.setItems(Node.State.values());
		this.enabled.setItems(Boolean.TRUE, Boolean.FALSE);
		this.ignored.setItems(Boolean.FALSE, Boolean.TRUE);

		add(this.header, this.name, this.description, this.state, this.enabled, this.ignored, createButtonsLayout());
	}

	public void setHeader(String header) {
		this.header.setText(header);
	}

	public void setNode(Node node) {
		this.node = node;
		this.binder.readBean(this.node); // to bind the values from the node to the UI fields.
		// Note: readBean() copies the values from the Contact to an internal model, that way we don’t accidentally overwrite values if we cancel editing.
	}

	private HorizontalLayout createButtonsLayout() {
		this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.save.setId("nodeform-save-button"); //$NON-NLS-1$
		this.save.addClickShortcut(Key.ENTER);
		this.save.addClickListener(event -> validateAndSave());
		this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));

		this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.save.setId("nodeform-delete-button"); //$NON-NLS-1$
		this.delete.addClickListener(event -> fireEvent(new DeleteEvent(this, this.node)));

		this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		this.close.setId("nodeform-close-button"); //$NON-NLS-1$
		this.close.addClickShortcut(Key.ESCAPE);
		this.close.addClickListener(event -> fireEvent(new CloseEvent(this)));

		return new HorizontalLayout(this.save, this.delete, this.close);
	}

	private void validateAndSave() {
		log.info("Saving {}", this.node); //$NON-NLS-1$
		try {
			this.binder.writeBean(this.node);
			fireEvent(new SaveEvent(this, this.node));
		} catch (ValidationException e) {
			e.printStackTrace();
		}
	}

	// Events - shared super class
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

	public static class SaveEvent extends NodeFormEvent
	{

		private static final long serialVersionUID = -8255534604198398835L;

		SaveEvent(NodeForm source, Node node) {
			super(source, node);
		}
	}
	public static class DeleteEvent extends NodeFormEvent
	{
		private static final long serialVersionUID = -5394186746319400182L;

		DeleteEvent(NodeForm source, Node node) {
			super(source, node);
		}
	}
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
		return getEventBus().addListener(eventType, listener);
	}
}
