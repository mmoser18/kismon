/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 10.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.NodeType;
import org.springframework.stereotype.Component;

@Component
public class NodeFactory <N extends Node>
{
	private static int nodeCtr = 0;

	public N createNode(NodeType type)
		throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<? extends Node> nodeClass = type.getNodeClass();
		@SuppressWarnings("unchecked")
		N newNode = (N)nodeClass.getDeclaredConstructor().newInstance();
		String simpleName = nodeClass.getSimpleName();
		newNode.setName(String.format(Messages.getString(simpleName + ".Name"), NodeFactory.nodeCtr)); //$NON-NLS-1$
		newNode.setDescription(String.format(Messages.getString(simpleName + ".Description"), NodeFactory.nodeCtr)); //$NON-NLS-1$
		nodeCtr++;
		return newNode;
	}

	public static List<NodeType> getLeafNodeTypes() {
		ArrayList<NodeType> res = new ArrayList<>();
		Arrays.asList(NodeType.values()).forEach(value -> { if (LeafNode.class.isAssignableFrom(value.getNodeClass())) res.add(value); });
		return res;
	}
}