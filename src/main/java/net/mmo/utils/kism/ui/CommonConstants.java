/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 16.10.2020
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.component.HasStyle;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonConstants
{
	public static final String ApplicationFullName = "KIS-Monitoring"; //$NON-NLS-1$
	public static final String ApplicationShortName = "KISM"; //$NON-NLS-1$

	public static final String LoginURLSuffix = "login"; //$NON-NLS-1$
	public static final String LogoutURLSuffix = "logout"; //$NON-NLS-1$

	public enum LayoutDirection {
		DirectionRow(Messages.getString("CommonConstants.LayoutDirection.Row")), //$NON-NLS-1$
		DirectionCol(Messages.getString("CommonConstants.LayoutDirection.Col")); //$NON-NLS-1$

		String name;

		LayoutDirection(String name) {
			this.name = name;
		}

		public static LayoutDirection invert(LayoutDirection dir) {
			return (dir == LayoutDirection.DirectionRow
			       ? LayoutDirection.DirectionCol
			       : LayoutDirection.DirectionRow);
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	/* set a classname to only ONE of a list of enumerated values */
	public static void setClassName(HasStyle component, Enum<?> t) {
		for (Enum<?> x: t.getDeclaringClass().getEnumConstants()) {
			if (x == t) {
				log.info("Component {}: setting class '{}'", component, x.name()); //$NON-NLS-1$
				component.addClassName(x.name());
			} else {
				log.info("Component {}: removing class '{}'", component, x.name()); //$NON-NLS-1$
				component.removeClassName(x.name());
			}
		}
	}

	private CommonConstants() {}
}
