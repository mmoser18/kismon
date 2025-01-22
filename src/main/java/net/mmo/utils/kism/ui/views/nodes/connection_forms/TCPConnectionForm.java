/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;

@SuppressWarnings("javadoc")
public abstract class TCPConnectionForm <N extends TCPConnection> extends CheckableNodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	// these are package visible so that unit-tests can access them:
	static final String URLClassName           = NodeFormField + "url"; //$NON-NLS-1$
	static final String ResultingURLClassName  = NodeFormField + "resulting-url"; //$NON-NLS-1$

	static final String RequestDetailsClassName = NodeFormField + "request-details"; //$NON-NLS-1$

	protected TextField url;
	protected TextField resultingURL;

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.url = new TextField(Messages.getString("TCPConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.setThemeName(LabelPaddingTheme);
		this.url.setClearButtonVisible(this.clearButtonsVisible);
		this.url.setEnabled(this.isAdminUser);
		this.url.addValueChangeListener(event ->
			{
				this.log.debug("url value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingUrl(event.getValue());
					} catch (Exception ex) {
						this.log.error("Error updating URL", ex); //$NON-NLS-1$
					}
				}
			});
		// Had to disable the URL validation here (and moved it to HTTPConnectionForm and JDBCConnectionForm)
		// since JDBC-URLs look completely differently and didn't pass Vaadin's conventional URI check!
		// this.binder.forField(this.url)
		//	.withValidator(this.urlValidator)
		//	.bind(TCPConnection::getUrl, TCPConnection::setUrl);

		this.resultingURL = new TextField(Messages.getString("TCPConnectionForm.ResultingUrl.Value")); //$NON-NLS-1$
		this.resultingURL.setClassName(ResultingURLClassName);
		this.resultingURL.setThemeName(LabelPaddingTheme);
		this.resultingURL.setReadOnly(true);
		// same here:
		// this.binder.forField(this.resultingURL).bind(TCPConnection::getResultingUrl, null);

		this.connnectionDetailsPanel.add(this.url, this.resultingURL);
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl(this.node != null ? this.node.getUrl() : null);
	}

	protected void updateResultingUrl(String newValue) {
		updateResultingField("updateResultingURL", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     TCPConnection::setResultingUrl,
		                     this.resultingURL
		                    );
	}

	public static Double getDurationInMillis(TCPConnection n) {
		return convertDurationToMillis(n.getDuration());
	}
}
