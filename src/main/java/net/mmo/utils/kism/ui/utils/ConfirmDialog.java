/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.utils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

/**
 * provides misc. variants of confirmation dialogs
 */
@Slf4j
public class ConfirmDialog extends Dialog
{
	private static final long serialVersionUID = -3151200601662024616L;


	/**
	 * @param header
	 * @param question
	 * @param confirmLabel
	 * @param confirmListener
	 */
	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener) {
		this(header, question,
		     confirmLabel, confirmListener,
		     null, null,
		     null, null);
	}

	/**
	 * @param header
	 * @param question
	 * @param confirmLabel
	 * @param confirmListener
	 * @param rejectLabel
	 * @param rejectListener
	 */
	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener,
	                     String rejectLabel , ComponentEventListener<ClickEvent<Button>> rejectListener) {
		this(header, question,
		     confirmLabel, confirmListener,
		     rejectLabel, rejectListener,
		     null, null);
	}

	/**
	 * @param header
	 * @param question
	 * @param confirmLabel
	 * @param confirmListener
	 * @param rejectLabel
	 * @param rejectListener
	 * @param cancelLabel
	 * @param cancelListener
	 */
	public ConfirmDialog(String header, String question,
	                     String confirmLabel, ComponentEventListener<ClickEvent<Button>> confirmListener,
	                     String rejectLabel , ComponentEventListener<ClickEvent<Button>> rejectListener,
	                     String cancelLabel , ComponentEventListener<ClickEvent<Button>> cancelListener) {
		setCloseOnEsc(true);
		setCloseOnOutsideClick(false);

		VerticalLayout msgLane = new VerticalLayout();

		if (header != null) msgLane.add(new H3(header));
		for (String str: question.split("\n")) { //$NON-NLS-1$

			msgLane.add(new NativeLabel(str));
		}

		HorizontalLayout buttons = new HorizontalLayout();
		Button confirmButton = new Button(confirmLabel, event ->
			{
				log.info("confirm-button pressed on " + this); //$NON-NLS-1$
				close();
				if (confirmListener != null) confirmListener.onComponentEvent(event);
			});
		buttons.add(confirmButton);

		if (rejectLabel != null) {
			Button rejectButton = new Button(rejectLabel, event ->
				{
					log.info("reject-button pressed on " + this); //$NON-NLS-1$
					close();
					if (rejectListener != null) rejectListener.onComponentEvent(event);
				});
			buttons.add(rejectButton);
		}
		if (cancelLabel != null) {
			Button cancelButton = new Button(cancelLabel, event ->
				{
					log.info("cancel-button pressed on " + this); //$NON-NLS-1$
					close();
					if (cancelListener != null) cancelListener.onComponentEvent(event);
				});
			buttons.add(cancelButton);
		}
		add(msgLane, buttons);

		log.info("ConfirmDialog('{}','{}')", header, question);  //$NON-NLS-1$
	}
}
