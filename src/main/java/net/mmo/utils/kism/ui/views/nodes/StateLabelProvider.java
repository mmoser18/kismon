/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 5 Feb 2021
 */

package net.mmo.utils.kism.ui.views.nodes;

import java.util.HashMap;

import net.mmo.utils.kism.entities.nodes.Node;

@SuppressWarnings("javadoc")
public class StateLabelProvider
{
	static HashMap<Node.State, String> labels = new HashMap<>();

	public static String getStateLabel(Node.State state) {
		if (state == null) {
			return Messages.getString("Node.State.Label.NULL"); //$NON-NLS-1$
		}
		String label = labels.get(state);
		if (label == null) {
			label = Messages.getString("Node.State.Label." + state.name()); //$NON-NLS-1$
			labels.put(state, label);
		}
		return label;
	}
}
