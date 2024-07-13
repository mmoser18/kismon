/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 3 Mar 2021
 */

package net.mmo.utils.kism.ui.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.ThreadSupport;

/**
 * utility class for the execution of activities that need to reserve the UI before being run
 */
@Slf4j
public class UIHandlerSupport
{
	private final static boolean RESERVE_UI_BEFORE_UPDATE = true;

	private UIHandlerSupport() {
		throw new RuntimeException("should never be called..."); //$NON-NLS-1$
	}

	/**
	 * @param <T> the type of the result produced by longRunningActivity
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param delay1 optional delay before longRunningActivity
	 * @param longRunningActivity method that creates a result which will be passed to uiActivity
	 * @param delay2 optional delay before uiActivity
	 * @param uiActivity operates on (typically: visualizes) the result created by longRunningActivity
	 */
	public static <T extends Object> void executeLater(Component comp,
	                                                   long delay1,
	                                                   Supplier<T> longRunningActivity,
	                                                   long delay2,
	                                                   Consumer<T> uiActivity) {
		ThreadSupport.executeLater(delay1, longRunningActivity,
		                           delay2, (result) ->
			{
				if (uiActivity != null) {
					if (RESERVE_UI_BEFORE_UPDATE) {
						log.debug("obtaining GUI access:"); //$NON-NLS-1$
						comp.getUI().ifPresentOrElse((ui) -> ui.access(() -> {
						                            	log.debug("uiActivity start:"); //$NON-NLS-1$
						                            	uiActivity.accept(result);
						                            	log.debug("uiActivity done."); //$NON-NLS-1$
						                             }),
						                             () -> {
						                            	 log.warn("unable to obtain UI-handle - output dropped"); //$NON-NLS-1$
						                            	 logout();
						                             });
					} else {
						try {
							log.debug("uiActivity start:"); //$NON-NLS-1$
							uiActivity.accept(result);
							log.debug("uiActivity done."); //$NON-NLS-1$
						} catch (Exception ex) {
							log.error("uiActivity failed.", ex); //$NON-NLS-1$
						}
					}
				}
			});
	}

	private static void logout() {
		VaadinSession current = VaadinSession.getCurrent();
		if (current != null) {
			current.getSession().invalidate();
			current.close();
		}
	}

	/**
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param delay optional delay before uiActivity is executed
	 * @param uiActivity some UI activity
	 */
	public static void executeLater(Component comp,
	                                long delay,  // in milliseconds
	                                Command uiActivity) { // ui-activity to be started after delay
		executeLater(comp,
		             0, null,
		             delay, (_1_) -> uiActivity.execute());
	}

	/**
	 * @param <O> the type of the object that is passed to uiActivity
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param obj will be passed as argument to the uiActivity:
	 * @param uiActivity that will operate on the passed object
	 */
	public static <O extends Object> void executeLater(Component comp,
	                                                   O obj,
	                                                   Consumer<O> uiActivity) {
		executeLater(comp,
		             0, null,
		             0, (_1_) -> uiActivity.accept(obj));
	}

	/**
	 * @param <T> the type of the result that is created by longRunningActivity and passed to uiActivity
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param longRunningActivity method that creates a result which will be passed to uiActivity
	 * @param uiActivity operates on (typically: visualizes) the result created by longRunningActivity
	 **/
	public static <T extends Object> void executeLater(Component comp,
	                                                   Supplier<T> longRunningActivity,
	                                                   Consumer<T> uiActivity) {
		log.debug("executeLater({}, {})", longRunningActivity, uiActivity); //$NON-NLS-1$
		executeLater(comp,
		             0, longRunningActivity,
		             0, uiActivity);
	}

	/**
	 * @param <T> the type of the result that is created by longRunningActivity and passed to uiActivity
	 * @param <O> the type of the parameter that is passed to longRunningActivity
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param obj an object that is passed to both, longRunningActivity and uiActivity
	 * @param delay1 optional delay before longRunningActivity
	 * @param longRunningActivity method that creates a result which will be passed to uiActivity
	 * @param delay2 optional delay before uiActivity
	 * @param uiActivity operates on (typically: visualizes) the result created by longRunningActivity
	 **/
	public static <O extends Object, T extends Object> void executeLater(Component comp,
	                                                                     O obj, // Object to be passed to longRunningActivity and uiActivity
	                                                                     long delay1,
	                                                                     Function<O, T> longRunningActivity,
	                                                                     long delay2,
	                                                                     BiConsumer<O, T> uiActivity) {
		executeLater(comp,
		             delay1, () -> longRunningActivity.apply(obj),
		             delay2, (result) -> uiActivity.accept(obj, result));
	}

	/**
	 * Short form of the above method with zero delays
	 * @param <O> the type of the parameter that is passed to longRunningActivity
	 * @param <T> the type of the result that is created by longRunningActivity and passed to uiActivity
	 * @param comp the component whose UI has to be reserved before uiActivity is executed
	 * @param obj an object that is passed to both, longRunningActivity and uiActivity
	 * @param longRunningActivity method that creates a result which will be passed to uiActivity
	 * @param uiActivity operates on (typically: visualizes) the result created by longRunningActivity
	 **/
	public static <O extends Object, T extends Object> void executeLater(Component comp,
	                                                                     O obj, // Object to be passed to longRunningActivity and uiActivity
	                                                                     Function<O, T> longRunningActivity,
	                                                                     BiConsumer<O, T> uiActivity) {
		executeLater(comp, obj,
		             0, longRunningActivity,
		             0, uiActivity);
	}
}
