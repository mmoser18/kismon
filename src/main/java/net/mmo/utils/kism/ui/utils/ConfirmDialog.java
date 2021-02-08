/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 8 Feb 2021
 */

package net.mmo.utils.kism.ui.utils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;

public class ConfirmDialog extends Dialog
{
	private static final long serialVersionUID = -3151200601662024616L;

	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener,
	                     String rejectLabel , ComponentEventListener<ClickEvent<Button>> rejectListener) {
		this(header, question,
		     confirmLabel, confirmListener,
		     rejectLabel, rejectListener,
		     null, null);
	}

	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener,
	                     String rejectLabel , ComponentEventListener<ClickEvent<Button>> rejectListener,
	                     String cancelLabel , ComponentEventListener<ClickEvent<Button>> cancelListener) {
		setCloseOnEsc(true);
		setCloseOnOutsideClick(false);

		add(new H3(header));
		add(new Label(question));

		Span result = new Span();
		add(result);

		Button confirmButton = new Button(confirmLabel, event -> {
			result.setText("Confirmed!"); //$NON-NLS-1$
			close();
			confirmListener.onComponentEvent(event);
		});
		add(confirmButton);

		if (rejectLabel != null) {
			Button rejectButton = new Button(rejectLabel, event -> {
				result.setText("Rejected!"); //$NON-NLS-1$
				close();
				rejectListener.onComponentEvent(event);
			});
			add(rejectButton);
		}
		if (cancelLabel != null) {
			Button cancelButton = new Button(cancelLabel, event -> {
				result.setText("Cancelled..."); //$NON-NLS-1$
				close();
				cancelListener.onComponentEvent(event);
			});
			add(cancelButton);
		}
	}

}
