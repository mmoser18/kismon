/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions.impl;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.IAction;

@SuppressWarnings("javadoc")
@ToString(includeFieldNames = true, callSuper = true)
@Slf4j
public class RunScriptAction <N extends ActionableNode> implements IAction<N>
{
	// we need a public constructor so that the ActionFactory can create this using <class>.newInstance();
	public RunScriptAction() {
		// empty
	}

	@Override
	public Component getDetailsForm() {
		return new Label("This action is not yet implemented..."); //$NON-NLS-1$
	}

	@Override
	public void doAction(N node, State oldState, State newState) {
		log.info("run script here..."); //$NON-NLS-1$
//		if (newState == FAILED) {
//			restartSystem();
//		}
	}
}
