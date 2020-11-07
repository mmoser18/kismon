/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 21 Feb 2021
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