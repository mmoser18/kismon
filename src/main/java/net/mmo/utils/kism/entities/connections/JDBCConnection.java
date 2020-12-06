/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

public class JDBCConnection extends Connection
{
	private static final long serialVersionUID = -2228821478937714703L;

	String sid;
	String user;
	String pwd;

	public JDBCConnection(String name, String description, State state) {
		super(name, description, state);
	}

}
