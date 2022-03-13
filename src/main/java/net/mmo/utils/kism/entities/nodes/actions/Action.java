/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.entities.nodes.actions;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.utils.ThreadSupport;

/**
 * defines the actions that can be taken after a change state
 * @param <N>
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@ToString
@Slf4j
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public abstract class Action <N extends Node> extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 9046877318500148684L;
	public final static String PROPERTYNAME_ACTION = "action";  //$NON-NLS-1$
	protected int triggerThresholds[] = new int[] {3, 5, 2};  // values for [FAILED, DEGRADED, OK]
	/**
	 * This keeps the previous state for which an action was triggered
	 */
	protected State previousActionState;

	public void checkForAction(N node) {
		// the aim of the following is to NOT trigger an action right away but only if it the state has been stable for a given number of checks
		final State newState = node.getState();
		final State oldState = this.previousActionState; // we keep it for logging and in case we trigger the action
		final int threshold = this.triggerThresholds[newState.ordinal()];
		final int stableSince =  node.getSameStateSince();
		if (threshold > 0 && // if there is a threshold defined...
		    stableSince == threshold && // and we have seen this state now often enough..
		    newState != oldState) { // and the states differ...
			this.previousActionState = newState;
			log.info("current state '{}' stable since {} ticks (previous state: {}) -> triggering action '{}':", //$NON-NLS-1$
			         newState, threshold, oldState, this);
			node.informOnPropertyChange(PROPERTYNAME_ACTION, oldState, newState);
			// the actual action is handled in a different thread:
			ThreadSupport.executeLater(0, () ->
				{
					log.info("executing action '{}':", this); //$NON-NLS-1$
					doAction(node, oldState, newState);
					log.info("action '{}' done.", this); //$NON-NLS-1$
				});
		} else {
			log.trace("new state '{}' stable since {} / threshold={} / oldState={} -> no action taken.", //$NON-NLS-1$
			          newState, stableSince, threshold, oldState);
		}
	}

	public abstract void doAction(N node, State previousState, State newState);
}
