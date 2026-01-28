/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("javadoc")
@Slf4j
public class CommonConstants
{
	// These strings are used as annotation arguments and hence unfortunately need to be constants (and can not be made application properties):
	public static final String ApplicationFullName = "KIS-Monitoring"; //$NON-NLS-1$
	public static final String ApplicationShortName = "KISM"; //$NON-NLS-1$

	public static final String LoginURL = "login"; //$NON-NLS-1$
	public static final String LogoutURL = "logout"; //$NON-NLS-1$
	public static final String LoginSuccessURL = "nodes"; //$NON-NLS-1$

	public static final String HelpURL = "./help/help.html"; //$NON-NLS-1$

	public static final String PasswordPlaceHolder = "<password_suppressed>"; // text to send instead of a password for non-admin users //$NON-NLS-1$
	public final static String Role_ADMIN = "ADMIN"; //$NON-NLS-1$
	public final static String Role_READ_ONLY = "READ_ONLY"; //$NON-NLS-1$
	public enum LayoutDirection {
		Horizontal(Messages.getString("CommonConstants.LayoutDirection.Horizontal")), //$NON-NLS-1$
		Vertical  (Messages.getString("CommonConstants.LayoutDirection.Vertical")); //$NON-NLS-1$

		String name;

		LayoutDirection(String name) {
			this.name = name;
		}

		public static LayoutDirection invert(LayoutDirection dir) {
			return (dir == LayoutDirection.Horizontal
			       ? LayoutDirection.Vertical
			       : LayoutDirection.Horizontal);
		}

		public SplitLayout.Orientation getOrientation() {
			switch (this) {
			case Horizontal: return Orientation.HORIZONTAL;
			case Vertical  : return Orientation.VERTICAL;
			default: throw new IllegalArgumentException("Unknown layout direction: " + this); //$NON-NLS-1$
			}
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	private CommonConstants() {}

	/* set a classname to only ONE of a list of enumerated values */
	public static void setClassName(HasStyle component, Enum<?> t) {
		for (Enum<?> x: t.getDeclaringClass().getEnumConstants()) {
			if (x == t) {
				log.trace("Component {}: setting class '{}'", component, x.name()); //$NON-NLS-1$
				component.addClassName(x.name());
			} else {
				log.trace("Component {}: removing class '{}'", component, x.name()); //$NON-NLS-1$
				component.removeClassName(x.name());
			}
		}
	}

	/**	Utility to be used as getters for binders to suppress passwords from being sent to client */
	public static String suppressedPassword(Object _1_) {
		return PasswordPlaceHolder;
	}
}
