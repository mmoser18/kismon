/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import java.net.URISyntaxException;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import net.mmo.utils.kism.entities.connections.Connection;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

public class ConnectionForm <N extends Connection> extends NodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	public static final String EnabledClassName  = "node-form-field-enabled"; //$NON-NLS-1$
	public static final String PeriodClassName   = "node-form-field-period"; //$NON-NLS-1$

	public static final String URLClassName      = "node-form-field-url"; //$NON-NLS-1$
	public static final String ResultingURLClassName = "node-form-field-resulting-url"; //$NON-NLS-1$

	Checkbox enabled;
	IntegerField period;
	TextField url;
	Label resultingURL;

	public ConnectionForm(Class<N> clazz) {
		super(clazz);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.enabled = new Checkbox(Messages.getString("ConnectionForm.Enabled.Label")); //$NON-NLS-1$
		this.enabled.addClassName(EnabledClassName);

		this.period = new IntegerField(Messages.getString("ConnectionForm.Period.Label")); //$NON-NLS-1$
		this.period.addClassName(PeriodClassName);

		this.url = new TextField(Messages.getString("ConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.addValueChangeListener(event -> {
			updateResultingURL();
		});

		this.resultingURL = new Label();
		this.resultingURL.setClassName(ResultingURLClassName);
		updateResultingURL();

		this.fields.add(this.enabled, this.url, this.resultingURL);
	}

	private void updateResultingURL() {
		try {
			String urlString;
			if (this.node != null) {
				urlString = this.node.getResultingURL(this.url.getValue());
				this.resultingURL.setText(String.format(Messages.getString("ConnectionForm.ResultingUrl.Value"), urlString)); //$NON-NLS-1$
				this.resultingURL.setVisible(!urlString.equals(this.url.getValue()));
			} else {
				this.resultingURL.setText("<undefined>"); //$NON-NLS-1$
			}
		} catch (URISyntaxException ex) {
			this.resultingURL.setText("Illegal URL: " + ex + " - reason: " + ex.getReason()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
