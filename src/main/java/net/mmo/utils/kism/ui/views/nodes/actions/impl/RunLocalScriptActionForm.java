/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31.08.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionDetailsForm;
import org.slf4j.Logger;

/**
 * the form for the RunScript-parameters
 */
@Slf4j
public class RunLocalScriptActionForm extends VerticalLayout implements ActionDetailsForm
{
	private static final long serialVersionUID = 919750045304284045L;

	/**
	 * must be public to be accessible by the factory
	 */
	public RunLocalScriptActionForm() {
		add(new Label("This action is not yet implemented...")); //$NON-NLS-1$
	}

	@Override
	public Logger getLogger() {
		return log;
	}
}
