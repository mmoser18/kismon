/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 5 Feb 2021
 */

package net.mmo.utils.kism.ui.views.nodes;

import java.util.HashMap;

import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.utils.StringUtils;

@SuppressWarnings("javadoc")
public class NodeTypeLabelProvider
{
	static HashMap<String, String> labels = new HashMap<>();

	public static String getNodeTypeLabel(Node node) {
		return getNodeTypeLabel(node.getClass().getSimpleName());
	}
	public static String getNodeTypeLabel(VisibleNodeType type) {
		return getNodeTypeLabel(type.getNodeClass().getSimpleName());
	}
	public static String getNodeTypeLabel(String classSimpleName) {
		String label = labels.get(classSimpleName);
		if (label == null) {
			label = Messages.getString("Node.Type.Label." + classSimpleName); //$NON-NLS-1$
			labels.put(classSimpleName, label);
		}
		return label;
	}

	// this is the inverse of the above to determine the node type from the UI-Label
	public static VisibleNodeType getNodeType(String label) {
		if (!StringUtils.isEmpty(label)) {
			for (VisibleNodeType value: VisibleNodeType.values()) {
				if (getNodeTypeLabel(value).equals(label)) return value;
			}
		}
		return null;
	}
}
