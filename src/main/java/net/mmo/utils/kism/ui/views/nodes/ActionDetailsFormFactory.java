/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.06.2022
 */

package net.mmo.utils.kism.ui.views.nodes;

import com.vaadin.flow.component.Component;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.ActionFactory.ActionType;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
/**
 * creates appropriate form for action handler ...
 * @param <N>
 */
@Slf4j
public class ActionDetailsFormFactory <N extends ActionableNode>
{
	private Component[] components;
	@SuppressWarnings("javadoc")
	public ActionDetailsFormFactory() {
		this.components = new Component[ActionType.values().length];
	}
	/**
	 * creates appropriate form for passed-in action handler
	 * @param node
	 * @param actionType
	 * @return the created form
	 */
	public Component createActionDetailsForm(N node, ActionType actionType) {
		log.info("createActionDetailsForm(node:{}, actionType:{})", node, actionType); //$NON-NLS-1$
		if (actionType != null) {
			Component comp = this.components[actionType.ordinal()];
			if (comp == null) {
				try {
					IAction<ActionableNode> action = actionType.getActionClass().getDeclaredConstructor().newInstance();
					comp = action.getDetailsForm();
				} catch (Exception ex) {
					log.error("Unexpected/unsupported actiontype '" + actionType + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.components[actionType.ordinal()] = comp;
			}
			return comp;
		}
		return null;
	}
}
