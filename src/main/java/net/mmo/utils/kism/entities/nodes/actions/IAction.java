/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 18 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes.actions;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;

/**
 * defines action to be taken
 * @param <N>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public interface IAction <N extends ActionableNode> extends Cloneable, Serializable
{
	/**
	 * @param node which has changed
	 * @param previousState
	 * @param newState  (if this is null then this is a manually triggered test only)
	 * @throws Exception
	 */
	public void doAction(@NotNull N node, State previousState, @NotNull State newState) throws Exception;
}