/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.Arrays;

@SuppressWarnings("javadoc")
public class ExceptionUtils
{
	private ExceptionUtils() {
		// never instantiated!
	}
	public static String exceptionCauseSummary(final Throwable start) {
		final StringBuffer buf = new StringBuffer();
		for (Throwable t = start; t != null; t = t.getCause()) {
			if (buf.length() > 0) {
				buf.append("\ncaused by: "); //$NON-NLS-1$
			}
			buf.append(t.getClass().getName()).append(": ").append(t.getMessage()); //$NON-NLS-1$
		}
		return buf.toString();
	}
	public static String exceptionRootCauseMsg(final Throwable start) {
		String msg = "<no root cause provided>"; //$NON-NLS-1$
		for (Throwable t = start; t != null; t = t.getCause()) {
			final String tMsg = t.getMessage();
			if (tMsg != null && !tMsg.isBlank()) {
				msg = t.getMessage(); // memorize the latest message
			} else {
				final Throwable[] suppressed = t.getSuppressed(); // always non-null
				if (suppressed.length > 0) {
					msg = Arrays.asList(t.getSuppressed()).toString();
				}
			}
		}
		return msg;
	}
}
