/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import net.mmo.utils.kism.entities.LeafNode;

public class Connection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	String protocol;
	String host;
	String port;
	String path;
	String args;

	public Connection(String name, String description, State state) {
		super(name, description, state);
	}

}
