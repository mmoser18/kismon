/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 11.01.2021
 */

package net.mmo.utils.kism.utils;

import java.util.Arrays;

@SuppressWarnings("javadoc")
public class ExceptionUtils
{
	private ExceptionUtils() {
		// never instantiated!
	}
	public static String exceptionCauseSummary(Throwable start) {
		StringBuffer buf = new StringBuffer();
		for (Throwable t = start; t != null; t = t.getCause()) {
			if (buf.length() > 0) {
				buf.append("\ncaused by: "); //$NON-NLS-1$
			}
			buf.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()); //$NON-NLS-1$
		}
		return buf.toString();
	}
	public static String exceptionRootCauseMsg(Throwable start) {
		String msg = "<no root cause provided>"; //$NON-NLS-1$
		for (Throwable t = start; t != null; t = t.getCause()) {
			String tMsg = t.getMessage();
			if (tMsg != null && !tMsg.isBlank()) {
				msg = t.getMessage();
			} else {
				Throwable[] suppressed = t.getSuppressed(); // always non-null
				if (suppressed.length > 0) {
					msg = Arrays.asList(t.getSuppressed()).toString();
				}
			}
		}
		return msg;
	}
}
