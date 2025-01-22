/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui;

import java.time.format.DateTimeFormatter;

import net.mmo.utils.kism.utils.AppProperties;

@SuppressWarnings("javadoc")
public class UIConstants
{
	public final static String EMPTY_STRING = ""; //$NON-NLS-1$
	public final static String UNDEFINED_STRING = Messages.getString("FieldValue.Undefined"); //$NON-NLS-1$

	public final static String NodeFormField = "node-form-field-"; //$NON-NLS-1$
	public final static String HistoryViewField = "history-view-field-"; //$NON-NLS-1$

	/** class name to mark combined fields */
	public static final String CombinedClassName = "fields-combined"; //$NON-NLS-1$
	/** theme use for fields that should have its label closer to the value field than default */
	public final static String LabelPaddingTheme = "label-padding"; //$NON-NLS-1$
	/** theme use for fields that should be displayed in monospaced font */
	public final static String MonospaceTheme = "monospace"; //$NON-NLS-1$

	public final static DateTimeFormatter dateTimeFormatter =
		DateTimeFormatter.ofPattern(AppProperties.getProperties().getProperty("LocalDateTimeStamp.Format", //$NON-NLS-1$
		                                                                      "yyyy-MM-dd HH:mm:ss.SS")); //$NON-NLS-1$

	private UIConstants() {
		// never instantiated
	}
}
