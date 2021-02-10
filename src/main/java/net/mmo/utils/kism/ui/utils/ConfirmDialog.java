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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ConfirmDialog extends Dialog
{
	private static final long serialVersionUID = -3151200601662024616L;

	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener) {
		this(header, question,
		     confirmLabel, confirmListener,
		     null, null,
		     null, null);
	}

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

		VerticalLayout msgLane = new VerticalLayout();

		if (header != null) msgLane.add(new H3(header));
		for (String str: question.split("\n")) { //$NON-NLS-1$
			msgLane.add(new Label(str));
		}

		HorizontalLayout buttons = new HorizontalLayout();
		Button confirmButton = new Button(confirmLabel, event -> {
			close();
			if (confirmListener != null) confirmListener.onComponentEvent(event);
		});
		buttons.add(confirmButton);

		if (rejectLabel != null) {
			Button rejectButton = new Button(rejectLabel, event -> {
				close();
				if (rejectListener != null) rejectListener.onComponentEvent(event);
			});
			buttons.add(rejectButton);
		}
		if (cancelLabel != null) {
			Button cancelButton = new Button(cancelLabel, event -> {
				close();
				if (cancelListener != null) cancelListener.onComponentEvent(event);
			});
			buttons.add(cancelButton);
		}
		add(msgLane, buttons);
	}
}
