/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 15.01.2021
 */

package net.mmo.utils.kism.ui;

public class UIConstants
{
	public final static String EMPTY_STRING = ""; //$NON-NLS-1$
	public final static String UNDEFINED_STRING = Messages.getString("FieldValue.Undefined"); //$NON-NLS-1$

	public final static String NodeFormField = "node-form-field-"; //$NON-NLS-1$

	/** theme use for fields that should have its label closer to the value field than default */
	public final static String LabelPaddingTheme    = "label-padding"; //$NON-NLS-1$
	/** theme use for fields that should be displayed in monospaced font */
	public final static String MonospaceTheme   = "monospace"; //$NON-NLS-1$

	private UIConstants() {
		// never instantiated
	}
}
