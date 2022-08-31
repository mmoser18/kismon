/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31.08.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * the form for the RunScript-parameters
 */
public class RunScriptActionForm extends VerticalLayout
{
	private static final long serialVersionUID = 919750045304284045L;

	/**
	 * must be public to be accessible by the factory
	 */
	public RunScriptActionForm() {
		add(new Label("This action is not yet implemented...")); //$NON-NLS-1$
	}

}
