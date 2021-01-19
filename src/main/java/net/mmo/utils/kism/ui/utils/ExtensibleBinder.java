/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 19.01.2021
 */

package net.mmo.utils.kism.ui.utils;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.StatusChangeEvent;
import com.vaadin.flow.data.binder.StatusChangeListener;

public class ExtensibleBinder<BEAN> extends Binder<BEAN>
{
	private static final long serialVersionUID = 8696629199590448916L;

	private List<Binder<?>> subBinders = new ArrayList<>();

	/**
	 * @param <FIELDVALUE> the value type of the field
	 * @param field the field to be bound, not null
	 * @return the new binding builder
	 */
	public <FIELDVALUE, SOME_BEAN> ExtensibleBinder<BEAN> addSubBinder(HasValue<?, FIELDVALUE> field,
	                                                                   Binder<SOME_BEAN> subBinder) {
		this.subBinders.add(subBinder);
		subBinder.addStatusChangeListener(new StatusChangeListener()
		{
			private static final long serialVersionUID = 101169872447414894L;

			@Override
			public void statusChange(StatusChangeEvent event) {
				// fireStatusChangeEvent(event.hasValidationErrors());
			}
		});
		subBinder.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<?>>()
		{
			private static final long serialVersionUID = 2438253609512092937L;

			@Override
			public void valueChanged(ValueChangeEvent<?> event) {
				// fireEvent(event);
			}
		});

		return this;
	}

}
