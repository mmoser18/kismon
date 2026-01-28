/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

@SuppressWarnings("javadoc")
@Component
public class NodeFactory <N extends Node>
{
	private static int nodeCtr = 0;

	public N createNode(VisibleNodeType type)
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

	public static List<VisibleNodeType> getLeafNodeTypes() {
		ArrayList<VisibleNodeType> res = new ArrayList<>();
		Arrays.asList(VisibleNodeType.values()).forEach(value -> { if (LeafNode.class.isAssignableFrom(value.getNodeClass())) res.add(value); });
		return res;
	}
}