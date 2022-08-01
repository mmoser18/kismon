/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.entities.nodes.actions;

import java.io.Serializable;
import java.util.Arrays;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ActionCheck extends AbstractEntity implements Cloneable, Serializable
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
	// Threshold fields are define as TextField (instead of IntegerField) to allow for placeholders
	private String[] triggerThresholds =
		AppProperties.getProperties().getProperty("ActionCheck.DefaultThresholdValues", "5,2,1").split(",");  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
	/**
	 * This keeps the previous state for which an action was triggered
	 */
	@JsonIgnore
	transient private State previousActionState;

	/**
	 * The actual action
	 */
	@NotNull
	protected IAction<ActionableNode> action;

	public void checkForAction(ActionableNode node) {
		// the aim of the following is to NOT trigger an action right away but only if it the state has been stable for a given number of checks
		final State newState = node.getState();
		final State oldState = this.previousActionState; // we keep it for logging and in case we trigger the action
		final int threshold = Integer.parseInt(this.triggerThresholds[newState.ordinal()]);
		final int stableSince =  node.getSameStateSince();
		if (threshold > 0 && // if there IS a threshold defined..
		    newState != oldState && // and the states differ...
		    stableSince >= threshold) {  // and we have seen this state now often enough..
			this.previousActionState = newState;
			log.info("current state '{}' stable since {} ticks (previous state: {}) -> triggering action '{}':", //$NON-NLS-1$
			         newState, threshold, oldState, this);
			node.informOnPropertyChange(PROPERTYNAME_ACTION, oldState, newState);
			// the actual action is handled in a different thread:
			ThreadSupport.executeLater(0, () ->
				{
					log.info("executing action '{}':", this); //$NON-NLS-1$
					this.action.doAction(node, oldState, newState);
					log.info("action '{}' done.", this); //$NON-NLS-1$
				});
		} else {
			log.trace("new state '{}' stable since {} / threshold={} / oldState={} -> no action taken.", //$NON-NLS-1$
			          newState, stableSince, threshold, oldState);
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
