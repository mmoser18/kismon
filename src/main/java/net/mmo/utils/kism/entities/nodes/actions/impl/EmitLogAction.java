/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions.impl;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.IAction;


/**
 * class description here...
 * @param <N>
 */
//disabled since this doesn't compile with the newest version ("Lombok annotation handler class lombok.eclipse.handlers.HandleToString failed"):
//@ToString(includeFieldNames = true, callSuper = true)
@ToString
@Slf4j
public class EmitLogAction <N extends ActionableNode> implements IAction<N>
{
	private static final long serialVersionUID = -8407051637305711684L;

	/**
	 * we need a public parameterless constructor so that the ActionFactory can create this using <class>.newInstance();
	 */
	public EmitLogAction() {
		// empty
	}

	@Override
	public void doAction(N node, State oldState, State newState) {
		String test = newState == null ? " (manually triggered test)" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		log.info("State for node '{}' has changed from {} to {}.{}", //$NON-NLS-1$
		         node.getName(), oldState, newState, test);
	}
}
