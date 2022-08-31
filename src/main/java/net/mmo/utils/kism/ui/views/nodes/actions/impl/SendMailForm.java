/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31.08.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions.impl;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * the form for the SendMail-parameters
 */
public class SendMailForm extends VerticalLayout
{
	private static final long serialVersionUID = 7901853154069994172L;

	/**
	 * must be public to be accessible by the factory
	 */
	public SendMailForm() {
		add(new Label("This action is not yet implemented...")); //$NON-NLS-1$
	}
}
