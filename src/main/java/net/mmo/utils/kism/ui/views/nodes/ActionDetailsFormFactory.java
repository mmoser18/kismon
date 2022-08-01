/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.06.2022
 */

package net.mmo.utils.kism.ui.views.nodes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.ActionFactory.ActionType;
/**
 * creates appropriate form for action handler ...
 * @param <N>
 */
public class ActionDetailsFormFactory <N extends ActionableNode>
{
	private Component[] components;
	private Label noArgsLabel;

	@SuppressWarnings("javadoc")
	public ActionDetailsFormFactory() {
		this.components = new Component[ActionType.values().length];
		this.noArgsLabel = new Label("No arguments available/required for this action"); //$NON-NLS-1$
	}
	/**
	 * creates appropriate form for passed-in action handler
	 * @param node
	 * @param actionType
	 * @return the created form
	 */
	public Component createActionDetailsForm(N node, ActionType actionType) {
		if (actionType != null) {
			Component comp = this.components[actionType.ordinal()];
			if (comp == null) {
				switch (actionType) {
				case EmitLogMsg:
					comp = this.noArgsLabel;
					break;
				case SendEmail:
					comp = this.noArgsLabel;
					break;
				case RunScript:
					comp = this.noArgsLabel;
					break;
				default:
					throw new IllegalStateException("Unexpected/unsupported actiontype '" + actionType + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.components[actionType.ordinal()] = comp;
			}
			return comp;
		}
		return null;
	}
}
