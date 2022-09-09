/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions.impl;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.entities.nodes.actions.IAction;

@SuppressWarnings("javadoc")
@ToString(includeFieldNames = true, callSuper = true)
@Slf4j
public class EmitLogAction  <N extends ActionableNode> implements IAction<N>
{
	// we need a public constructor so that the ActionFactory can create this using <class>.newInstance();
	public EmitLogAction() {
		// empty
	}

	@Override
	public void doAction(N node, State oldState, State newState, ActionCheck check) {
		log.info("State for node '{}' has changed from {} to {}.", //$NON-NLS-1$
		         node.getName(), oldState, newState);
	}
}
