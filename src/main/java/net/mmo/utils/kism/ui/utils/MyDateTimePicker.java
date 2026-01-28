/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.utils;

import java.time.Duration;
import java.util.Locale;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;

/**
 * In Vaadin v18 the original DateTimePicker had a bug. Thus I had then used
 * org.vaadin.miki.superfields.dates.SuperDateTimePicker instead
 * (@see <a href="https://github.com/vaadin-miki/super-fields">https://github.com/vaadin-miki/super-fields</a>).
 * To shield me from possible later modifications or future incompatibilities (which indeed happened with
 * Vaadin v20+) I wrapped this into my own class, so that I can replace/implement it in one central place
 * With Vaadin v22 I changed this back to the original Vaadin DateTimePicker, i.e. the class now is
 * practically empty but I still keep it - just in case...
 */
public class MyDateTimePicker extends DateTimePicker
{
	private static final long serialVersionUID = 7607754515556993999L;

	/**
	 *
	 */
	public MyDateTimePicker() {
		super();
		setStep(Duration.ofMinutes(1));
		setLocale(Locale.getDefault());
	}
}
