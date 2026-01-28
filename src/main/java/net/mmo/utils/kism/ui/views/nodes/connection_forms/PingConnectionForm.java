/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.MonospaceTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.PingConnection;


@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class PingConnectionForm extends IPConnectionForm<PingConnection>
{
	private static final long serialVersionUID = -5157762871511746801L;

	static final String ResponseClassName          = NodeFormField + "response"; //$NON-NLS-1$

	protected TextArea response;

	@Override
	public void init(NodeService nodeService) {
		super.init(PingConnection.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);
		this.response = new TextArea(Messages.getString("PingConnectionForm.Response.Label")); //$NON-NLS-1$
		this.response.setClassName(ResponseClassName);
		this.response.setThemeName(LabelPaddingTheme);
		this.response.addThemeName(MonospaceTheme);
		this.response.setReadOnly(true);
		this.binder.forField(this.response).bind((n) -> n.getResponse(), null);

		this.fields.add(this.response);
	}

}
