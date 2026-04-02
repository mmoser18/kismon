/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 1 Apr 2026
 */

package net.mmo.utils.kism.ui.utils;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.Messages;

/**
 * Copied from here: {@link "https://vaadin.com/docs/latest/styling/advanced/dynamic-stylesheets"}
 */
@Slf4j
public class ThemeChanger extends Select<String>
{
	private final static String[] Styles = { Lumo.STYLESHEET, Aura.STYLESHEET };
	private final static String DefaultStyle = Styles[0];

	private static final long serialVersionUID = 2381999734913846425L;

	@SuppressWarnings("javadoc")
	public ThemeChanger() {
		addThemeVariants(SelectVariant.SMALL);
		setLabel(Messages.getString("ThemeChanger.Label")); //$NON-NLS-1$
		setItems(Styles);
		setValue(DefaultStyle);
		setEmptySelectionAllowed(true);
		setEmptySelectionCaption("basic (no theme)"); //$NON-NLS-1$
		setErrorMessage("Illegal value"); //$NON-NLS-1$

		addValueChangeListener(event -> {
			log.info("Theme '{}' selected.", event.getValue()); //$NON-NLS-1$
			switchTheme(event.getSource().getUI().orElse(UI.getCurrent()),
			            event.getValue());
		});
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		UI ui = attachEvent.getUI();
		Registration registration = ComponentUtil.getData(ui, Registration.class);
		log.info("registration: {}", registration); //$NON-NLS-1$
		// set default-style if no registration found:
		if (registration == null) {
			switchTheme(ui, DefaultStyle);
		}
	}

	/**
	 * Switch the current theme style sheet for a new one.
	 *
	 * @param ui the ui in use, not {@code null}
	 * @param styleSheet new style to replace current with
	 */
	protected void switchTheme(UI ui, String styleSheet) {
		// Get previous style registration
		Registration registration = ComponentUtil.getData(ui, Registration.class);
		// If previous available remove style sheet
		if (registration != null) {
			registration.remove();
		}
		if (styleSheet != null) {
			// Add the new style sheet
			log.info("addStyleSheet: {}", styleSheet); //$NON-NLS-1$
			registration = ui.getPage().addStyleSheet(styleSheet);
			// Store registration to remove style on later change
			ComponentUtil.setData(ui, Registration.class, registration);
		}
	}
}