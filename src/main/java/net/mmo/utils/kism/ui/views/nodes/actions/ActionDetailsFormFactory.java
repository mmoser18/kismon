/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.06.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import com.vaadin.flow.component.Component;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionFactory.ActionType;
/**
 * creates appropriate form for action handler ...
 * @param <N>
 */
@Slf4j
public class ActionDetailsFormFactory <N extends ActionableNode>
{
	private Component[] components = new Component[ActionType.values().length];

	@SuppressWarnings("javadoc")
	public ActionDetailsFormFactory() {
//		this.components = new Component[ActionType.values().length];
	}
	/**
	 * creates appropriate form for passed-in action handler
	 * @param node
	 * @param actionType
	 * @param dataHasChanged
	 * @return the created form
	 */
	@SuppressWarnings("unchecked")
	public Component createActionDetailsForm(N node, ActionType actionType, Runnable dataHasChanged) {
		log.info("createActionDetailsForm(node:{}, actionType:{})", node, actionType); //$NON-NLS-1$
		if (actionType != null) {
			try {
				Component comp = this.components[actionType.ordinal()];
				if (comp == null) {
					comp = actionType.getActionFormClass().getDeclaredConstructor().newInstance();
					this.components[actionType.ordinal()] = comp;
				}
				((ActionDetailsForm<N>)comp).setNode(node, dataHasChanged);
				return comp;
			} catch (Exception ex) {
				log.error("Error creating new instance of '" + actionType.getActionFormClass().getName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}
}
