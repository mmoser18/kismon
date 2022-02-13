/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13 Feb 2022
 */

package net.mmo.utils.kism.ui.utils;

import java.time.Duration;
import java.util.Locale;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;

/**
 * In Vaadin v18 the original DateTimePicker had a bug. Thus I had then used
 * org.vaadin.miki.superfields.dates.SuperDateTimePicker instead
 * (@see <a href="https://github.com/vaadin-miki/super-fields">https://github.com/vaadin-miki/super-fields</a>).
 * But to shield me from possible later modifications or incompatibilities (which indeed happened with
 * Vaadin v20+) I wrapped it into my own class, so that I could replace/implement it in one central place
 * if need be. With Vaadin v22 I changed this back to the original Vaadin DateTimePicker.
 */
public class MyDateTimePicker extends DateTimePicker
{
	private static final long serialVersionUID = 7607754515556993999L;

	public MyDateTimePicker() {
		super();
		setStep(Duration.ofMinutes(1));
		setLocale(Locale.getDefault());
	}
}
