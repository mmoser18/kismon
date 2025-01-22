/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import net.mmo.utils.kism.entities.nodes.connections.JDBCConnection;
import net.mmo.utils.kism.entities.nodes.connections.PingConnection;
import net.mmo.utils.kism.entities.nodes.connections.RESTConnection;
import net.mmo.utils.kism.entities.nodes.connections.SOAPConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection;

/**
 * enumerates all visible nodes
 */
@SuppressWarnings("javadoc")
public enum VisibleNodeType {
	RootNode(RootNode.class),
	IntermediateNode(IntermediateNode.class),
	RESTConnection(RESTConnection.class),
	SOAPConnection(SOAPConnection.class),
	JDBCConnection(JDBCConnection.class),
	PingConnection(PingConnection.class),
	SSHConnection(SSHConnection.class),
	;

	Class<? extends Node> nodeClass;

	VisibleNodeType(Class<? extends Node> nodeClass) {
		this.nodeClass = nodeClass;
	}

	public Class<? extends Node> getNodeClass() {
		return this.nodeClass;
	}
}