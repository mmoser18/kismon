/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import net.mmo.utils.kism.entities.connections.JDBCConnection;

public class JDBCConnectionForm extends ConnectionForm<JDBCConnection>
{
	private static final long serialVersionUID = 5726368043436831511L;

	public JDBCConnectionForm() {
		super(JDBCConnection.class);
	}
}
