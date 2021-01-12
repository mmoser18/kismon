/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 11.01.2021
 */

package net.mmo.utils.kism.utils;

public class ExceptionUtils
{
	private ExceptionUtils() {
		// never used!
	}
	public static String exceptionCauseSummary(Throwable t) {
		StringBuffer buf = new StringBuffer();
		for (Throwable t2 = t; t2 != null; t2 = t2.getCause()) {
			if (buf.length() > 0) {
				buf.append("\ncaused by: "); //$NON-NLS-1$
			}
			buf.append(t2.getClass()).append(": ").append(t2.getMessage()); //$NON-NLS-1$
		}
		return buf.toString();
	}
}
