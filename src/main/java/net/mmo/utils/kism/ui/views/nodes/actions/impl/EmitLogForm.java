/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31.08.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionDetailsForm;

/**
 * the form for the EmitgLog-parameters
 */
public class EmitLogForm extends VerticalLayout implements ActionDetailsForm
{
	private static final long serialVersionUID = 3854742967701361528L;

	/**
	 * must be public to be accessible by the factory
	 */
	public EmitLogForm() {
		add(new Label("No arguments available/required for this action.")); //$NON-NLS-1$
	}
}
