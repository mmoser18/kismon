/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 10.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.connections.JDBCConnection;
import net.mmo.utils.kism.entities.nodes.connections.RESTConnection;
import net.mmo.utils.kism.entities.nodes.connections.SOAPConnection;
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
		N newNode;
		switch (type) {
		case IntermediateNode:
			newNode = (N)new IntermediateNode(String.format(Messages.getString("IntermediateNode.Name"), NodeFactory.nodeCtr), //$NON-NLS-1$
			                                  String.format(Messages.getString("IntermediateNode.Description"), NodeFactory.nodeCtr)); //$NON-NLS-1$
			break;
		case RESTConnection:
			newNode = (N)new RESTConnection(String.format(Messages.getString("RESTConnection.Name"), NodeFactory.nodeCtr), //$NON-NLS-1$
			                                String.format(Messages.getString("RESTConnection.Description"), NodeFactory.nodeCtr)); //$NON-NLS-1$
			break;
		case SOAPConnection:
			newNode = (N)new SOAPConnection(String.format(Messages.getString("SOAPConnection.Name"), NodeFactory.nodeCtr), //$NON-NLS-1$
			                                String.format(Messages.getString("SOAPConnection.Description"), NodeFactory.nodeCtr)); //$NON-NLS-1$
			break;
		case JDBCConnection:
			newNode = (N)new JDBCConnection(String.format(Messages.getString("JDBCConnection.Name"), NodeFactory.nodeCtr), //$NON-NLS-1$
			                                String.format(Messages.getString("JDBCConnection.Description"), NodeFactory.nodeCtr)); //$NON-NLS-1$
			break;
		default: throw new IllegalArgumentException("Unexpected node type " + type + " in NodeFactory.createNode()"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		nodeCtr++;
		return newNode;
	}
}