/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 18 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.vaadin.flow.component.Component;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;

/**
 * defines action to be taken
 * @param <N> node that has changed
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public interface IAction<N extends ActionableNode>
{
	/**
	 * @return form for arguments
	 */
	@JsonIgnore
	Component getDetailsForm();

	/**
	 * @param node which has changed
	 * @param previousState
	 * @param newState
	 */
	void doAction(N node, State previousState, State newState);
}