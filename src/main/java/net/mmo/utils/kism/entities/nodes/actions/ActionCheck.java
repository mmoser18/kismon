/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.actions;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ThreadSupport;

/**
 * defines the actions that can be taken after a change state
 * @param <N>
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
public class ActionCheck extends AbstractEntity
{
	private static final long serialVersionUID = 9046877318500148684L;

	/**
	 * name signaled to node.informOnPropertyChange(...)
	 */
	public final static String PROPERTYNAME_ACTION = "action";  //$NON-NLS-1$
	/**
	 *  values for [FAILED, DEGRADED, OK]
	 *  Threshold fields are define as TextField (instead of IntegerField) to allow placeholders
	 */
	@NotNull
	@NotEmpty
	@NotBlank
	private String[] triggerThresholds =
		AppProperties.getProperties().getProperty("ActionCheck.DefaultThresholdValues", "5,2,1").split(",");  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$

	public String getTriggerThresholdString(State state) {
		return this.triggerThresholds[state.ordinal()];
	}
	public int getTriggerThresholdNumeric(State state) {
		return Integer.parseInt(getTriggerThresholdString(state));
	}

	/**
	 * This keeps the previous state for which an action was triggered
	 */
	@JsonIgnore
	transient private State previousActionState = State.nullTolerantValueOf(AppProperties.getProperties().getProperty("ActionCheck.AssumedInitialState", "OK")); //$NON-NLS-1$ //$NON-NLS-2$

	public void checkForAction(@NotNull ActionableNode node) {
		// the aim of the following is to NOT trigger an action right away but only if it the state has been stable for a given number of checks
		final State newState = node.getState();
		final int stableSince =  node.getSameStateSince();
		final State oldActionState = this.previousActionState; // we keep it for logging and in case we trigger the action
		final int threshold = getTriggerThresholdNumeric(newState);
		if (threshold > 0 && // if there IS a threshold defined ...
			oldActionState != null && // ... the previous state has been initialized
		    newState != oldActionState && // ... and the states differ...
		    stableSince >= threshold) {  // ... and we have seen this state now often enough, then:
			this.previousActionState = newState;
			log.info("Current state '{}' stable since {} ticks (previous state: {}) -> triggering action '{}':", //$NON-NLS-1$
			         newState, threshold, oldActionState, this);
			node.informOnPropertyChange(PROPERTYNAME_ACTION, oldActionState, newState);
			// the actual action is handled in a different thread:
			ThreadSupport.executeLater(0, () ->
			{
				IAction<ActionableNode> action = node.getAction();
				log.info("Executing action '{}':", action); //$NON-NLS-1$
				try {
					action.doAction(node, oldActionState, newState);
					log.info("Action '{}' done.", action); //$NON-NLS-1$
				} catch (Exception ex) {
					log.error("Error executing action " + action + ":", ex); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		} else if (oldActionState == null) {  // if this is still uninitialized:
			setPreviousActionState(newState); // ... we memorize the first seen state as previous state.
			                                  // This prevents the triggering of the actions on first check
		} else {
			log.trace("New state '{}' stable since {} / threshold={} / oldState={} -> no action taken.", //$NON-NLS-1$
			          newState, stableSince, threshold, oldActionState);
		}
	}
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", triggerThresholds:").append(Arrays.toString(getTriggerThresholds())) //$NON-NLS-1$
			.append(", previousActionState:").append(getPreviousActionState()) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}
