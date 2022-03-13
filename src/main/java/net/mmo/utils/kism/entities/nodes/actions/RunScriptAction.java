/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.Node.State;

@SuppressWarnings("javadoc")
@ToString(includeFieldNames = true, callSuper = true)
@Slf4j
public class RunScriptAction <N extends Node> extends Action<N>
{
	private static final long serialVersionUID = -2777410642398278660L;

	@Override
	public void doAction(Node node, State oldState, State newState) {
		log.info("run script here..."); //$NON-NLS-1$
//		if (newState == FAILED) {
//			restartSystem();
//		}
	}

}
