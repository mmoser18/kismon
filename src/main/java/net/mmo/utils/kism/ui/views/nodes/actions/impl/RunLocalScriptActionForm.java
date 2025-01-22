/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.NativeLabel;
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
		add(new NativeLabel("This action is not yet implemented...")); //$NON-NLS-1$
	}

	@Override
	public Logger getLogger() {
		return log;
	}
}
