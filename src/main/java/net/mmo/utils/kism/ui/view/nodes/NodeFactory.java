/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 10.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.connections.JDBCConnection;
import net.mmo.utils.kism.entities.connections.RESTConnection;
import net.mmo.utils.kism.entities.connections.SOAPConnection;

public class NodeFactory
{
	public enum NodeType {
		RootNode,
		IntermediateNode,
		RESTConnection,
		SOAPConnection,
		JDBCConnection
		;

		public String getName() {
			return Messages.getString("NodeType." + this.getName()); //$NON-NLS-1$
		}
	}

	private static int nodeCtr = 0;

	public static Node createNode(NodeType type) {
		switch (type) {
		case IntermediateNode:
			return new IntermediateNode("Node" + nodeCtr, "Intermediate Node"); //$NON-NLS-1$ //$NON-NLS-2$
		case RESTConnection:
			return new RESTConnection("REST" + nodeCtr, "REST Connection"); //$NON-NLS-1$ //$NON-NLS-2$
		case SOAPConnection:
			return new SOAPConnection("SOAP" + nodeCtr, "SOAP Connection"); //$NON-NLS-1$ //$NON-NLS-2$
		case JDBCConnection:
			return new JDBCConnection("JDBC" + nodeCtr, "JDBC Connection"); //$NON-NLS-1$ //$NON-NLS-2$
		default: throw new IllegalArgumentException("Unexpected node type " + type + " in NodeFactory.createNode()"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}