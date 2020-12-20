/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import net.mmo.utils.kism.entities.connections.RESTConnection;

public class RESTConnectionForm extends ConnectionForm<RESTConnection>
{
	private static final long serialVersionUID = 7200842663851555372L;

	public RESTConnectionForm() {
		super(RESTConnection.class);
	}

}
