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
import org.springframework.stereotype.Component;

@Component
public class NodeFactory <N extends Node>
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

	@SuppressWarnings("unchecked")
	public N createNode(NodeType type) {
		switch (type) {
		case IntermediateNode:
			return (N)new IntermediateNode("Node_" + nodeCtr, "Intermediate Node"); //$NON-NLS-1$ //$NON-NLS-2$
		case RESTConnection:
			return (N)new RESTConnection("REST_" + nodeCtr, "REST IPConnection"); //$NON-NLS-1$ //$NON-NLS-2$
		case SOAPConnection:
			return (N)new SOAPConnection("SOAP_" + nodeCtr, "SOAP IPConnection"); //$NON-NLS-1$ //$NON-NLS-2$
		case JDBCConnection:
			return (N)new JDBCConnection("JDBC_" + nodeCtr, "JDBC IPConnection"); //$NON-NLS-1$ //$NON-NLS-2$
		default: throw new IllegalArgumentException("Unexpected node type " + type + " in NodeFactory.createNode()"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}