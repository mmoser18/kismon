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

/**
 * class description here...
 * @param <N>
 */
@ToString(includeFieldNames = true, callSuper = true)
@Slf4j
public class RunRemoteScriptAction <N extends ActionableNode> implements IAction<N>
{
	/**
	 * we need a public parameterless constructor so that the ActionFactory can create this using <class>.newInstance();
	 */
	public RunRemoteScriptAction() {
		// empty
	}

	@Override
	public void doAction(N node, State oldState, State newState, ActionCheck check) {
		log.info("run remote script here..."); //$NON-NLS-1$
//		if (newState == FAILED) {
//			restartSystem();
//		}
	}
}
